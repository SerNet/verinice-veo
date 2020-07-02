/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.entity.jpa

import org.springframework.beans.factory.annotation.Autowired

import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.entity.jpa.custom.PropertyData

class CustomPropertyJpaSpec extends AbstractJpaSpec {
    @Autowired
    AssetDataRepository assetRepository

    def 'custom props are inserted'() {
        given:
        def asset = new AssetData(
                id: UUID.randomUUID().toString(),
                customAspects: [
                    new CustomPropertiesData(
                    id: UUID.randomUUID().toString(),
                    dataProperties: [
                        new PropertyData("k1", "uno"),
                        new PropertyData("k2", 2)
                    ]
                    )
                ]
                )
        when:
        assetRepository.save(asset)
        def retrievedAsset = assetRepository.findById(asset.id)
        then:
        retrievedAsset.present
        with(retrievedAsset.get().customAspects[0].dataProperties) {
            size() == 2
            it.find { it.key == "k1" }.stringValue == "uno"
            it.find { it.key == "k2" }.integerValue == 2
        }
    }

    def 'property type can be changed'() {
        given: 'a saved asset with a string prop'
        def asset = new AssetData(
                id: UUID.randomUUID().toString(),
                customAspects: [
                    new CustomPropertiesData(
                    id: UUID.randomUUID().toString(),
                    dataProperties: [
                        new PropertyData("k1", "uno")
                    ]
                    )
                ]
                )
        assetRepository.save(asset)
        when: 'replacing the string prop with an int prop'
        asset.customAspects[0].dataProperties = [
            new PropertyData("k1", 1)
        ]
        assetRepository.save(asset)
        def retrievedAsset = assetRepository.findById(asset.id)
        then: 'the change has been applied'
        with(retrievedAsset.get().customAspects[0].dataProperties) {
            size() == 1
            it[0].type == PropertyData.Type.INTEGER
            it[0].integerValue == 1
        }
    }

    def 'property can be removed'() {
        given: 'a saved asset with two props'
        def asset = new AssetData(
                id: UUID.randomUUID().toString(),
                customAspects: [
                    new CustomPropertiesData(
                    id: UUID.randomUUID().toString(),
                    dataProperties: [
                        new PropertyData("k1", "uno"),
                        new PropertyData("k2", "due")
                    ]
                    )
                ]
                )
        assetRepository.save(asset)
        when: 'removing the first prop'
        asset.customAspects[0].dataProperties = [
            new PropertyData("k2", "due")
        ]
        assetRepository.save(asset)
        def retrievedAsset = assetRepository.findById(asset.id)
        then: 'only the second prop remains'
        with(retrievedAsset.get().customAspects[0].dataProperties) {
            size() == 1
            it[0].key == "k2"
            it[0].type == PropertyData.Type.STRING
            it[0].stringValue == "due"
        }
    }
}
