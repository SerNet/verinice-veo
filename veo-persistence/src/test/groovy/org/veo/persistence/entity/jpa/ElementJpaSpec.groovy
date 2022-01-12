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
import javax.persistence.PersistenceException

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Client
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository

class ElementJpaSpec extends AbstractJpaSpec {

    @Autowired
    AssetDataRepository assetRepository

    @Autowired
    UnitDataRepository unitRepository

    @Autowired
    ClientDataRepository clientRepository

    @Autowired
    DomainDataRepository domainRepository

    @PersistenceContext
    private EntityManager entityManager

    Client client
    UnitData owner0
    UnitData owner1
    UnitData owner2

    def setup() {
        client = clientRepository.save(newClient())
        owner0 = unitRepository.save(newUnit(client))
        owner1 = unitRepository.save(newUnit(client))
        owner2 = unitRepository.save(newUnit(client))
    }

    def 'finds elements by owners'() {
        given: "three assets from different owners"

        assetRepository.save(newAsset(owner0) {
            name = "asset 0"
        })
        assetRepository.save(newAsset(owner1) {
            name = "asset 1"
        })
        assetRepository.save(newAsset(owner2) {
            name = "asset 2"
        })

        when: "querying assets from the first two owners"
        def result = assetRepository.findByUnits([owner0.dbId, owner1.dbId] as Set)

        then: "only the first two owners' assets are returned"
        with(result.sort {
            it.name
        }) {
            size() == 2
            it[0].name == "asset 0"
            it[1].name == "asset 1"
        }
    }

    def 'finding elements by owners includes composites and their parts'() {
        given: "a normal asset and an asset composite, both from the same owner"
        def part = assetRepository.save(newAsset(owner0) {
            name = "part"
        })
        assetRepository.save(newAsset(owner0) {
            name = "whole"
            parts = [part]
        })

        when: "querying the owner's assets"
        def result = assetRepository.findByUnits([owner0.dbId] as Set)

        then: "the asset composite and its part are returned"
        result*.name.toSorted() == ["part", "whole"]
    }

    def 'finds entity composites by owners'() {
        given: "three asset composites from different owners"
        assetRepository.save(newAsset(owner0) {
            name = "composite 0"
        })
        assetRepository.save(newAsset(owner1) {
            name = "composite 1"
        })
        assetRepository.save(newAsset(owner2) {
            name = "composite 2"
        })

        when: "querying composites from the first two owners"
        def result = assetRepository.findByUnits([owner0.dbId, owner1.dbId] as Set)

        then: "only the first two owners' composites are returned"
        with(result.sort {
            it.name
        }) {
            size() == 2
            it[0].name == "composite 0"
            it[1].name == "composite 1"
        }
    }

    def 'finds entities by domain'() {
        given:
        def domain1 = domainRepository.save(newDomain(client))
        def domain2 = domainRepository.save(newDomain(client))
        client = clientRepository.save(client)

        assetRepository.saveAll([
            newAsset(owner1) {
                name = "one"
                domains = [domain1]
            },
            newAsset(owner1) {
                name = "two"
                domains = [domain1, domain2]
            },
            newAsset(owner1) {
                name = "three"
                domains = [domain2]
            }
        ])

        when:
        def domain1Assets = assetRepository.findByDomain(domain1.id.uuidValue())

        then:
        domain1Assets*.name.sort() == ["one", "two"]
    }

    def 'increment version id'() {
        given: "one unit db ID"

        def dbId = owner0.getDbId()

        when: "load and save the asset"
        UnitData unit = unitRepository.findById(dbId).get()

        long versionBefore = unit.getVersion()
        unit.setName("New name")
        unitRepository.save(unit)
        entityManager.flush()

        unit = unitRepository.findById(dbId).get()

        then: "version is incremented"
        versionBefore == 0
        unit.getVersion() == 1
    }

    def 'max description length is applied'() {
        when:
        assetRepository.save(newAsset(owner0) {
            description = "-".repeat(DESCRIPTION_MAX_LENGTH)
        })
        entityManager.flush()
        then:
        noExceptionThrown()

        when:
        assetRepository.save(newAsset(owner0) {
            description = "-".repeat(DESCRIPTION_MAX_LENGTH + 1)
        })
        entityManager.flush()
        then:
        thrown(PersistenceException)
    }
}