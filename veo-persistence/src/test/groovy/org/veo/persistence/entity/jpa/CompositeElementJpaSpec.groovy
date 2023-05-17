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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.CompositeElement
import org.veo.core.entity.Unit
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository

class CompositeElementJpaSpec extends AbstractJpaSpec {

    @Autowired
    UnitDataRepository unitRepository

    @Autowired
    ClientDataRepository clientRepository

    @Autowired
    TransactionTemplate txTemplate

    Client client
    Unit unit

    def setup() {
        client = clientRepository.save(newClient())
        unit = unitRepository.save(newUnit(client))
    }

    def "circular composites are supported"() {
        when:"saving a circular asset composite structure"
        def composite1Id = txTemplate.execute {
            def composite1 = newAsset(unit)
            def composite2 = newAsset(unit) {
                parts = [composite1]
            }
            composite1.addPart(composite2)
            assetDataRepository.save(composite1).id.uuidValue()
        }

        then: "the elements can be retrieved"
        def retrievedComposite1 = (Asset)assetDataRepository.findById(composite1Id).get()
        def retrievedComposite2 = (Asset)retrievedComposite1.parts.first()

        and: "composite 1 is always the same object"
        retrievedComposite2.parts.first().is(retrievedComposite1)
    }

    def "a composite can contain itself"() {
        when:
        def composite1Id = txTemplate.execute {
            assetDataRepository.save(newAsset(unit) {
                addPart(it)
            }).id.uuidValue()
        }

        then:
        def retrievedComposite1a = assetDataRepository.findById(composite1Id).get()
        def retrievedComposite1b = retrievedComposite1a.parts.first()
        retrievedComposite1a.parts.first().is(retrievedComposite1b)
        retrievedComposite1b.parts.first().is(retrievedComposite1a)

        when:
        assetDataRepository.delete(retrievedComposite1a)

        then:
        !assetDataRepository.existsById(composite1Id)
    }

    def "a composite cannot contain different types"() {
        when:
        def asset = txTemplate.execute {
            def a = newAsset(unit)
            a.addPart(a)
            assetDataRepository.save(a)
        }

        def process = txTemplate.execute {
            processDataRepository.save(newProcess(unit))
        }

        asset = txTemplate.execute {
            def ce = (CompositeElement)asset
            ce.addPart(process)
            assetDataRepository.save(asset)
        }

        then:
        JpaSystemException ex = thrown()
        ex.mostSpecificCause.message == "Can not set final java.util.Set field org.veo.persistence.entity.jpa.AssetData.parts to org.veo.persistence.entity.jpa.ProcessData"
    }
}