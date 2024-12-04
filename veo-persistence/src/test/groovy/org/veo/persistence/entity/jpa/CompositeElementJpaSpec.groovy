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
        def assetIds = txTemplate.execute {
            def leaf = assetDataRepository.save(newAsset(unit) {
                name = "leaf"
            })
            def composite1 = assetDataRepository.save(newAsset(unit) {
                name = "composite 1"
            })
            def composite2 = assetDataRepository.save(newAsset(unit) {
                name = "composite 2"
            })
            composite1.addPart(composite2)
            composite1.addPart(leaf)
            composite2.addPart(composite1)
            [composite1, composite2, leaf]*.id
        }

        then: "the elements can be retrieved"
        def (retrievedComposite1, retrievedComposite2, retrievedLeaf) = assetIds
                .collect { assetDataRepository.findById(it) }
                *.get()

        and: "composite and parts are set"
        retrievedComposite1.composites*.name ==~ ["composite 2"]
        retrievedComposite1.parts*.name ==~ ["composite 2", "leaf"]
        retrievedComposite2.composites*.name ==~ ["composite 1"]
        retrievedComposite2.parts*.name ==~ ["composite 1"]
        retrievedLeaf.composites*.name ==~ ["composite 1"]
        retrievedLeaf.parts*.name == []

        and: "composites and parts are found recursively"
        retrievedComposite2.compositesRecursively*.name ==~ ["composite 1", "composite 2"]
        retrievedComposite2.partsRecursively*.name ==~ [
            "composite 1",
            "composite 2",
            "leaf"
        ]
        retrievedComposite1.compositesRecursively*.name ==~ ["composite 2", "composite 1"]
        retrievedComposite1.partsRecursively*.name ==~ [
            "composite 2",
            "composite 1",
            "leaf"
        ]
        retrievedLeaf.compositesRecursively*.name ==~ ["composite 1", "composite 2"]
        retrievedLeaf.partsRecursively*.name == []

        and: "composite 1 is always the same object"
        retrievedComposite2.parts.first().is(retrievedComposite1)
    }

    def "a composite can contain itself"() {
        when:
        def composite1Id = txTemplate.execute {
            assetDataRepository.save(newAsset(unit) {
                addPart(it)
            }).id
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
        ex.mostSpecificCause.message == "Can not get final java.util.Set field org.veo.persistence.entity.jpa.AssetData.parts on org.veo.persistence.entity.jpa.ProcessData"
    }
}