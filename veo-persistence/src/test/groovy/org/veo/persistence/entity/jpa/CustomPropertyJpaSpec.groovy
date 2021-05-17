/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.entity.jpa

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Unit
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.entity.jpa.custom.PropertyData

class CustomPropertyJpaSpec extends AbstractJpaSpec {
    @Autowired
    AssetDataRepository assetRepository

    @Autowired
    UnitDataRepository unitRepository

    @Autowired
    ClientDataRepository clientDataRepository

    @PersistenceContext
    EntityManager entityManager
    Unit unit

    def setup() {
        def client = clientDataRepository.save(newClient())
        unit = newUnit(client)
        unit = unitRepository.save(unit)
    }

    def 'custom props are inserted'() {
        given:
        def asset = newAsset(unit) {
            customAspects = [
                new CustomPropertiesData(
                dbId: UUID.randomUUID().toString(),
                dataProperties: [
                    new PropertyData("k1", "uno"),
                    new PropertyData("k2", 2)
                ]
                )
            ]
        }
        when:
        assetRepository.save(asset)
        def retrievedAsset = assetRepository.findById(asset.dbId)
        then:
        retrievedAsset.present
        with(retrievedAsset.get().customAspects[0].dataProperties) {
            size() == 2
            it.find { it.key == "k1" }.stringValue == "uno"
            it.find { it.key == "k2" }.integerValue  == 2
        }
    }

    def 'property type can be changed'() {
        given: 'a saved asset with a string prop'
        def asset = newAsset(unit) {
            customAspects = [
                new CustomPropertiesData(
                dbId: UUID.randomUUID().toString(),
                dataProperties: [
                    new PropertyData("k1", "uno")
                ]
                )
            ]
        }
        assetRepository.save(asset)
        when: 'replacing the string prop with an int prop'
        asset.customAspects[0].dataProperties = [
            new PropertyData("k1", 1)
        ]
        assetRepository.save(asset)
        def retrievedAsset = assetRepository.findById(asset.dbId)
        then: 'the change has been applied'
        with(retrievedAsset.get().customAspects[0].dataProperties) {
            size() == 1
            it[0].type == PropertyData.Type.INTEGER
            it[0].integerValue == 1
        }
    }

    def 'property can be removed'() {
        given: 'a saved asset with two props'
        def asset = newAsset(unit) {
            customAspects = [
                new CustomPropertiesData(
                dbId: UUID.randomUUID().toString(),
                dataProperties: [
                    new PropertyData("k1", "uno"),
                    new PropertyData("k2", "due")
                ]
                )
            ]
        }
        assetRepository.save(asset)
        when: 'removing the first prop'
        asset.customAspects[0].dataProperties = [
            new PropertyData("k2", "due")
        ]
        assetRepository.save(asset)
        def retrievedAsset = assetRepository.findById(asset.dbId)
        then: 'only the second prop remains'
        with(retrievedAsset.get().customAspects[0].dataProperties) {
            size() == 1
            it[0].key == "k2"
            it[0].type == PropertyData.Type.STRING
            it[0].stringValue == "due"
        }
    }

    def 'long custom property value is accepted'() {
        given:
        def stringLength = 18000
        def longString = "X" * stringLength
        def asset = newAsset(unit) {
            customAspects = [
                new CustomPropertiesData(
                dbId: UUID.randomUUID().toString(),
                dataProperties: [
                    new PropertyData("p", longString),
                ]
                )
            ]
        }
        when:
        assetRepository.save(asset)
        def retrievedAsset = assetRepository.findById(asset.getId().uuidValue())
        then:
        retrievedAsset.present
        when:
        def savedProperty = retrievedAsset.get().customAspects.first().dataProperties.first()
        def savedValue = savedProperty.stringValue
        then:
        savedValue.length() == stringLength
        savedValue == longString
    }
}
