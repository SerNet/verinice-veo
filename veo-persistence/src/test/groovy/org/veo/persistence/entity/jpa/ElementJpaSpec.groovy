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

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.decision.DecisionRef
import org.veo.core.entity.decision.DecisionResult
import org.veo.core.entity.decision.DecisionRuleRef
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.validation.ConstraintViolationException

class ElementJpaSpec extends AbstractJpaSpec {

    @Autowired
    AssetDataRepository assetRepository

    @Autowired
    UnitDataRepository unitRepository

    @Autowired
    ClientDataRepository clientRepository

    @PersistenceContext
    private EntityManager entityManager

    Client client
    UnitData owner0
    UnitData owner1
    UnitData owner2
    Domain domain

    def setup() {
        client = clientRepository.save(newClient())
        newDomain(client)
        client = clientRepository.save(client)
        domain = client.domains.first()
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
        def result = findAssetsByUnit(owner0) + findAssetsByUnit(owner1)

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
        def result = findAssetsByUnit(owner0)

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
        def result = findAssetsByUnit(owner0) + findAssetsByUnit(owner1)

        then: "only the first two owners' composites are returned"
        with(result.sort {
            it.name
        }) {
            size() == 2
            it[0].name == "composite 0"
            it[1].name == "composite 1"
        }
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

    def 'updates decision results'() {
        given: "anasset with a decision result"
        def rule0 = new DecisionRuleRef(0)
        def rule1 = new DecisionRuleRef(1)
        def decision = new DecisionRef("overclock")
        def asset = newAsset(owner0) {
            it.associateWithDomain(domain, "Server", "RUNNING")
            setDecisionResult(decision, new DecisionResult(true, rule0, [rule0], [rule0]), domain)
        }

        expect: "results to be retrievable"
        asset.getDecisionResults(domain).get(decision).decisiveRule == rule0

        when: "setting the same decision result again"
        def changed = asset.setDecisionResult(decision, new DecisionResult(true, rule0, [rule0], [rule0]), domain)

        then: "no change is reported"
        !changed

        when: "saving a slightly different result"
        changed = asset.setDecisionResult(decision, new DecisionResult(true, rule0, [rule0, rule1], [rule0]), domain)

        then: "the results have been changed"
        changed
        asset.getDecisionResults(domain).get(decision).matchingRules == [rule0, rule1]
    }

    def 'retrieved decision results are immutable'() {
        given: "a transient asset with a decision result"
        def rule0 = new DecisionRuleRef(0)
        def asset = newAsset(owner0) {
            it.associateWithDomain(domain, "Server", "RUNNING")
            setDecisionResult(new DecisionRef("overclock"), new DecisionResult(true, rule0, [rule0], [rule0]), domain)
        }

        when: "trying to mutate the results"
        asset.getDecisionResults(domain)[new DecisionRef("turnOffAtNight")] = new DecisionResult(false, rule0, [rule0], [rule0])

        then:
        thrown(UnsupportedOperationException)

        when: "saving and reloading the asset"
        asset = assetRepository.save(asset)
        entityManager.flush()
        entityManager.detach(asset)
        asset = assetRepository.findById(asset.dbId).get()

        and: "trying to mutate the results again"
        asset.getDecisionResults(domain)[new DecisionRef("turnOffAtNight")] = new DecisionResult(false, rule0, [rule0], [rule0])

        then:
        thrown(UnsupportedOperationException)
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
        thrown(ConstraintViolationException)
    }
}