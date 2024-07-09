/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.service.risk

import java.util.function.Function
import java.util.stream.Collectors

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.AbstractPerformanceITSpec
import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Element
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.Scope
import org.veo.core.entity.Unit
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.DomainRiskReferenceProvider
import org.veo.core.entity.risk.ImpactRef
import org.veo.core.entity.risk.ImpactValues
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.entity.riskdefinition.CategoryDefinition
import org.veo.core.entity.riskdefinition.CategoryLevel
import org.veo.core.entity.riskdefinition.RiskDefinition

import groovy.util.logging.Slf4j
import net.ttddyy.dsproxy.QueryCountHolder
import spock.lang.Unroll

@Slf4j
@WithUserDetails("user@domain.example")
/**
 * There are dot files for the different test scenarios in /verinice veo/doc/testGraphs.
 * Use a dot viewer.
 */
class RiskServiceImpactInheritanceITSpec extends AbstractPerformanceITSpec  {

    @Autowired
    ImpactInheritanceCalculator impactInheritanceCalculator

    Client client
    Domain domain
    Domain secondDomain
    String riskDefinitionId
    RiskDefinition riskDefinition
    Unit unit
    CategoryDefinition confidentiality
    CategoryRef confidentialityRef
    CategoryDefinition integrity
    CategoryRef integrityRef
    CategoryDefinition availability
    CategoryRef availabilityRef

    RiskDefinitionRef riskDefinitionRef

    CategoryLevel confidentialityImpact0
    CategoryLevel confidentialityImpact1
    CategoryLevel confidentialityImpact2
    CategoryLevel confidentialityImpact3

    CategoryLevel availabilityImpact0
    CategoryLevel availabilityImpact1
    CategoryLevel availabilityImpact2
    CategoryLevel availabilityImpact3

    CategoryLevel integrityImpact0
    CategoryLevel integrityImpact1
    CategoryLevel integrityImpact2
    CategoryLevel integrityImpact3

    Map impactValuesEmpty
    Map impactValues0
    Map impactValues1
    Map impactValues2
    Map impactValues3
    Map impactValuesMixedI
    Map impactValuesMixedC
    Map impactValuesMixedA

    List<Asset> dataDrivenAssets

    def setup() {
        client = createTestClient()
        domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        secondDomain = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID)
        riskDefinitionId = 'DSRA'
        riskDefinition = domain.riskDefinitions.get(riskDefinitionId)
        unit = unitDataRepository.save(newUnit(client).tap{
            addToDomains(domain)
            addToDomains(secondDomain)
        })

        confidentiality = riskDefinition.getCategory('C').orElseThrow()
        confidentialityRef = CategoryRef.from(confidentiality)
        integrity = riskDefinition.getCategory('I').orElseThrow()
        integrityRef = CategoryRef.from(integrity)
        availability = riskDefinition.getCategory('A').orElseThrow()
        availabilityRef = CategoryRef.from(availability)
        riskDefinitionRef = RiskDefinitionRef.from(riskDefinition)

        confidentialityImpact0 = confidentiality.getLevel(0).orElseThrow()
        confidentialityImpact1 = confidentiality.getLevel(1).orElseThrow()
        confidentialityImpact2 = confidentiality.getLevel(2).orElseThrow()
        confidentialityImpact3 = confidentiality.getLevel(3).orElseThrow()

        availabilityImpact0 = availability.getLevel(0).orElseThrow()
        availabilityImpact1 = availability.getLevel(1).orElseThrow()
        availabilityImpact2 = availability.getLevel(2).orElseThrow()
        availabilityImpact3 = availability.getLevel(3).orElseThrow()

        integrityImpact0 = integrity.getLevel(0).orElseThrow()
        integrityImpact1 = integrity.getLevel(1).orElseThrow()
        integrityImpact2 = integrity.getLevel(2).orElseThrow()
        integrityImpact3 = integrity.getLevel(3).orElseThrow()

        impactValuesEmpty = [
            (riskDefinitionRef) : new ImpactValues([:])
        ]

        impactValues0 = [
            (riskDefinitionRef) : new ImpactValues([
                (integrityRef) : ImpactRef.from(integrityImpact0),
                (confidentialityRef) : ImpactRef.from(confidentialityImpact0),
                (availabilityRef): ImpactRef.from(availabilityImpact0)
            ])
        ]
        impactValues1 = [
            (riskDefinitionRef) : new ImpactValues([
                (integrityRef) : ImpactRef.from(integrityImpact1),
                (confidentialityRef) : ImpactRef.from(confidentialityImpact1),
                (availabilityRef): ImpactRef.from(availabilityImpact1)
            ])
        ]
        impactValues2 = [
            (riskDefinitionRef) : new ImpactValues([
                (integrityRef) : ImpactRef.from(integrityImpact2),
                (confidentialityRef) : ImpactRef.from(confidentialityImpact2),
                (availabilityRef): ImpactRef.from(availabilityImpact2)
            ])
        ]
        impactValues3 = [
            (riskDefinitionRef) : new ImpactValues([
                (integrityRef) : ImpactRef.from(integrityImpact3),
                (confidentialityRef) : ImpactRef.from(confidentialityImpact3),
                (availabilityRef): ImpactRef.from(availabilityImpact3)
            ])
        ]
        impactValuesMixedI = [
            (riskDefinitionRef) : new ImpactValues([
                (integrityRef) : ImpactRef.from(integrityImpact3),
                (confidentialityRef) : ImpactRef.from(confidentialityImpact0),
                (availabilityRef): ImpactRef.from(availabilityImpact0)
            ])
        ]
        impactValuesMixedC = [
            (riskDefinitionRef) : new ImpactValues([
                (integrityRef) : ImpactRef.from(integrityImpact0),
                (confidentialityRef) : ImpactRef.from(confidentialityImpact3),
                (availabilityRef): ImpactRef.from(availabilityImpact0)
            ])
        ]
        impactValuesMixedA = [
            (riskDefinitionRef) : new ImpactValues([
                (integrityRef) : ImpactRef.from(integrityImpact0),
                (confidentialityRef) : ImpactRef.from(confidentialityImpact0),
                (availabilityRef): ImpactRef.from(availabilityImpact3)
            ])
        ]
    }

    def "determine the max of incoming"() {
        given: "a root and many leafs"
        Asset root = assetDataRepository.save(newAsset(unit) {
            name = "r"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValuesEmpty)
        })
        Asset l1 = assetDataRepository.save(newAsset(unit) {
            name = "l1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
            links = [
                newCustomLink(root, "asset_asset_app", domain),
            ]
        })

        Asset l2 = assetDataRepository.save(newAsset(unit) {
            name = "l2"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues1)
            links = [
                newCustomLink(root, "asset_asset_app", domain),
            ]
        })

        Asset l3 = assetDataRepository.save(newAsset(unit) {
            name = "l3"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues2)
            links = [
                newCustomLink(root, "asset_asset_app", domain),
            ]
        })

        Asset l4 = assetDataRepository.save(newAsset(unit) {
            name = "l4"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues3)
            links = [
                newCustomLink(root, "asset_asset_app", domain),
            ]
        })

        when: "Recalculate for the root element"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, root)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then: "the root element is set to max"
        result.size() == 1
        result[0].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues3.get(riskDefinitionRef).potentialImpacts

        and:
        verifyAll {
            queryCounts.select == 20
            queryCounts.insert == 0
            queryCounts.update == 2
            queryCounts.delete == 0
            queryCounts.time < 120
        }
    }

    def "each category is handled"() {
        given: "a root and many leafs each with only a max in one category"
        Asset root = assetDataRepository.save(newAsset(unit) {
            name = "root"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValuesEmpty)
        })
        Asset l1 = assetDataRepository.save(newAsset(unit) {
            name = "l1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValuesMixedA)
            links = [
                newCustomLink(root, "asset_asset_app", domain),
            ]
        })

        Asset l2 = assetDataRepository.save(newAsset(unit) {
            name = "l2"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValuesMixedC)
            links = [
                newCustomLink(root, "asset_asset_app", domain),
            ]
        })

        Asset l3 = assetDataRepository.save(newAsset(unit) {
            name = "l3"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValuesMixedI)
            links = [
                newCustomLink(root, "asset_asset_app", domain),
            ]
        })

        when: "Recalculate for the root element"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, root)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then: "the root impact of the element is set to max in each category"
        result.size() == 1
        result[0].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues3.get(riskDefinitionRef).potentialImpacts

        and:
        verifyAll {
            queryCounts.select == 19
            queryCounts.insert == 0
            queryCounts.update == 2
            queryCounts.delete == 0
            queryCounts.time < 100
        }
    }

    def "determine the max in a circle"() {
        given: "a circle"
        Asset startC = assetDataRepository.save(newAsset(unit) {
            name = "startC"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
        })
        def circle = buildAssetList(startC, unit, domain,"C2", impactValues0)
        circle = buildAssetList(circle, unit, domain,"C3",impactValues1)
        circle = buildAssetList(circle, unit, domain,"C4",impactValues2)
        startC.applyLink(newCustomLink(circle, "asset_asset_app", domain))
        startC = assetDataRepository.save(startC)

        when: "we change the startC element"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, startC)
        }

        then: "the root element is set to impactValues2 as all others are"
        def queryCounts = QueryCountHolder.grandTotal
        verifyAll {
            queryCounts.select == 19
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 100
        }
    }

    def "determine a circle in a list"() {
        given: "a circle in the middle"
        def l0 = assetDataRepository.save(newAsset(unit) {
            name = "l0"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
        })

        def listasset1 = buildAssetList(l0, unit, domain, "l1")
        def listasset2 = buildAssetList(listasset1, unit, domain, "l2")
        def listassetCircle = buildAssetList(listasset2, unit, domain,"l3")
        def listasset4 = buildAssetList(listassetCircle, unit, domain,"l4")
        def listasset5 = buildAssetList(listasset4, unit, domain,"l5")
        listassetCircle.applyLink(newCustomLink( listasset5, "asset_asset_app", domain))
        listassetCircle = assetDataRepository.save(listassetCircle)
        def listasset6 = buildAssetList(listasset5, unit, domain,"l6")

        when: "we change the l3 element"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, listassetCircle)
        }

        then: "the l3 is part of a circle"
        def queryCounts = QueryCountHolder.grandTotal

        and:
        verifyAll {
            queryCounts.select == 9
            queryCounts.insert == 0
            queryCounts.update == 0
            queryCounts.delete == 0
            queryCounts.time < 100
        }

        when: "we change the l0 element"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, l0)
        }
        queryCounts = QueryCountHolder.grandTotal

        then: "l0 is a root element so one changes"
        result.size() == 1
        result[0] == l0

        and:
        verifyAll {
            queryCounts.select == 17
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 100
        }

        when: "we change the l6 element"
        log.debug("---------------------------------------------")
        listasset6 = executeInTransaction {
            listasset6 = assetDataRepository.findById(listasset6.idAsUUID).get()
            listasset6.setImpactValues(domain, impactValues1)
            assetDataRepository.save(listasset6)
        }

        QueryCountHolder.clear()
        result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, listasset6)
        }

        queryCounts = QueryCountHolder.grandTotal

        then:
        verifyAll {
            queryCounts.select == 13
            queryCounts.insert == 1
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 100
        }
    }

    def "determine the value in a simple list"() {
        given: "A chain of asset with an impact at the leaf"
        def a1 = assetDataRepository.save(newAsset(unit) {
            name = "a1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
        })
        def a2 = buildAssetListOpposite(a1, unit, domain,"a2",impactValuesEmpty)
        def a3 = buildAssetListOpposite(a2, unit, domain,"a3",impactValuesEmpty)
        def a4 = buildAssetListOpposite(a3, unit, domain,"a4",impactValuesEmpty)
        def a5 = buildAssetListOpposite(a4, unit, domain,"a5",impactValuesEmpty)

        when: "we calculate from a1"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        def result = executeInTransaction {
            a1 = assetDataRepository.findById(a1.idAsUUID).get()
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, a1)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then:"all impacts are changed in the chain"
        result.size() == 5
        result[0] == a1
        result[1].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues0.get(riskDefinitionRef).potentialImpacts
        result[2].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues0.get(riskDefinitionRef).potentialImpacts
        result[3].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues0.get(riskDefinitionRef).potentialImpacts
        result[4].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues0.get(riskDefinitionRef).potentialImpacts
        result[4] == a5

        and:
        verifyAll {
            queryCounts.select == 13
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 100
        }
    }

    def "two chains with two links"() {
        def b1 = assetDataRepository.save(newAsset(unit) {
            name = "b1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
        })
        def b2 = buildAssetListOpposite(b1, unit, domain,"b2",impactValuesEmpty)
        def b3 = buildAssetListOpposite(b2, unit, domain,"b3",impactValuesEmpty)
        def b4 = buildAssetListOpposite(b3, unit, domain,"b4",impactValuesEmpty)
        def b5 = buildAssetListOpposite(b4, unit, domain,"b5",impactValuesEmpty)

        def c1 = assetDataRepository.save(newAsset(unit) {
            name = "c1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
        })
        def c2 = buildAssetListOpposite(c1, unit, domain,"c2",impactValuesEmpty)
        def c3 = buildAssetListOpposite(c2, unit, domain,"c3",impactValuesEmpty)
        def c4 = buildAssetListOpposite(c3, unit, domain,"c4",impactValuesEmpty)
        def c5 = buildAssetListOpposite(c4, unit, domain,"c5",impactValuesEmpty)

        b4 = executeInTransaction {
            b4 = assetDataRepository.findById(b4.idAsUUID).get()
            b4.applyLink(newCustomLink(c4, "asset_asset_app", domain))
            assetDataRepository.save(b4)
        }
        c2 = executeInTransaction {
            c2 = assetDataRepository.findById(c2.idAsUUID).get()
            c2.applyLink(newCustomLink(b3, "asset_asset_app", domain))
            assetDataRepository.save(c2)
        }

        when: "We change the impact of b2"
        log.debug("---------------------------------------------")
        b2 = executeInTransaction {
            b2 = assetDataRepository.findById(b2.idAsUUID).get()
            b2.setImpactValues(domain, impactValues1)
            assetDataRepository.save(b2)
        }

        and: "recalculate the impact for b2"
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, b2)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then: "also the linked c4 and c5 is affected"
        result.size() == 6
        result[0] == b2
        result[0].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues1.get(riskDefinitionRef).potentialImpacts
        result[1] == b3
        result[2] == b4
        result[3] == b5
        result[4] == c4
        result[4].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues1.get(riskDefinitionRef).potentialImpacts

        and:
        verifyAll {
            queryCounts.select == 18
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 70
        }

        when: "we get calculate the impact for c2"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        result = executeInTransaction {
            c2 = assetDataRepository.findById(c2.idAsUUID).get()
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, c2)
        }
        queryCounts = QueryCountHolder.grandTotal

        then: "c2 to c3 are affected as c4 has the same impact set"
        result.size() == 9
        result[0] == c2
        result[1] == b3
        result[2] == b4
        result[3] == b5

        result[4] == c4
        result[5] == c5
        result[6] == c3
        result[7] == c4
        result[8] == c5

        and:
        verifyAll {
            queryCounts.select == 17
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 100
        }

        when: "We change the impact of c1 to 3"
        log.debug("---------------------------------------------")
        c1 = executeInTransaction {
            c1 = assetDataRepository.findById(c1.idAsUUID).get()
            c1.setImpactValues(domain, impactValues2)
            assetDataRepository.save(c1)
        }

        and: "recalculate the impact for c1"
        QueryCountHolder.clear()
        result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, c1)
        }
        queryCounts = QueryCountHolder.grandTotal

        then: "also the linked c4 and c5 is affected"
        result.size() == 10
        result[0] == c1
        result[0].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues2.get(riskDefinitionRef).potentialImpacts
        result[1] == c2
        result[2] == b3
        result[3] == b4
        result[4] == b5
        result[5] == c4
        result[5].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues2.get(riskDefinitionRef).potentialImpacts
        result[6] == c5
        result[7] == c3

        and:
        verifyAll {
            queryCounts.select == 18
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 100
        }

        when: "We change the impact of b1"
        log.debug("---------------------------------------------")
        b1 = executeInTransaction {
            b1 = assetDataRepository.findById(b1.idAsUUID).get()
            b1.setImpactValues(domain, impactValues3)
            assetDataRepository.save(b1)
        }

        and: "recalculate the impact for b1"
        QueryCountHolder.clear()
        result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, b1)
        }
        queryCounts = QueryCountHolder.grandTotal

        then: "only b1 and b2 are affected as b2 has an impact set"
        result.size() == 7
        result[0] == b1
        result[0].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues3.get(riskDefinitionRef).potentialImpacts
        result[1] == b2
        result[1].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues3.get(riskDefinitionRef).potentialImpacts
        result[1].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues1.get(riskDefinitionRef).potentialImpacts

        and:
        verifyAll {
            queryCounts.select == 18
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 100
        }
    }

    def "three chains with links"() {
        given: "three connected chains"
        def a1 = assetDataRepository.save(newAsset(unit) {
            name = "a1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValuesEmpty)
        })
        def a2 = buildAssetListOpposite(a1, unit, domain,"a2",impactValuesEmpty)
        def a3 = buildAssetListOpposite(a2, unit, domain,"a3",impactValuesEmpty)
        def a4 = buildAssetListOpposite(a3, unit, domain,"a4",impactValuesEmpty)
        def a5 = buildAssetListOpposite(a4, unit, domain,"a5",impactValuesEmpty)

        def b1 = assetDataRepository.save(newAsset(unit) {
            name = "b1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValuesEmpty)
        })
        def b2 = buildAssetListOpposite(b1, unit, domain,"b2",impactValuesEmpty)
        def b3 = buildAssetListOpposite(b2, unit, domain,"b3",impactValuesEmpty)
        def b4 = buildAssetListOpposite(b3, unit, domain,"b4",impactValuesEmpty)
        def b5 = buildAssetListOpposite(b4, unit, domain,"b5",impactValuesEmpty)

        def c1 = assetDataRepository.save(newAsset(unit) {
            name = "c1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValuesEmpty)
        })
        def c2 = buildAssetListOpposite(c1, unit, domain,"c2",impactValuesEmpty)
        def c3 = buildAssetListOpposite(c2, unit, domain,"c3",impactValuesEmpty)
        def c4 = buildAssetListOpposite(c3, unit, domain,"c4",impactValuesEmpty)
        def c5 = buildAssetListOpposite(c4, unit, domain,"c5",impactValuesEmpty)

        executeInTransaction {
            def a = assetDataRepository.findById(b1.idAsUUID).get()
            a.applyLink(newCustomLink(assetDataRepository.findById(a1.idAsUUID).get(), "asset_asset_app", domain))
            assetDataRepository.save(a)
            a= assetDataRepository.findById(b2.idAsUUID).get()
            a.applyLink(newCustomLink(assetDataRepository.findById(a2.idAsUUID).get(), "asset_asset_app", domain))
            assetDataRepository.save(a)
            a= assetDataRepository.findById(b3.idAsUUID).get()
            a.applyLink(newCustomLink(assetDataRepository.findById(c2.idAsUUID).get(), "asset_asset_app", domain))
            assetDataRepository.save(a )
            a = assetDataRepository.findById(a4.idAsUUID).get()
            a.applyLink(newCustomLink(assetDataRepository.findById(b4.idAsUUID).get(), "asset_asset_app", domain))
            assetDataRepository.save(a)
            a = assetDataRepository.findById(b4.idAsUUID).get()
            a.applyLink(newCustomLink(assetDataRepository.findById(c4.idAsUUID).get(), "asset_asset_app", domain))
            assetDataRepository.save(a)
        }

        when: "We change the impact of b1"
        log.debug("---------------------------------------------")
        b1 = executeInTransaction {
            b1 = assetDataRepository.findById(b1.idAsUUID).get()
            b1.setImpactValues(domain, impactValues1)
            assetDataRepository.save(b1)
        }

        and: "recalculate the impact for b1"
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, b1)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then: "the linked a1-a5, b1-b5 and c4 and c5 are affected"
        result.size() == 28// we have visited some elements more than once
        result[0] == b1
        result[1] == a1//has an impact set to 0 so only the calculated valued gets updated thru b1
        result[1].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues1.get(riskDefinitionRef).potentialImpacts
        result[2] == a2//for a2 to a5 the calculated values is 0 as the effective impact of a1 is 0
        result[5] == a5
        result[6] == b4//has the 1 impact inherited from b1 thru b3
        result[6].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues1.get(riskDefinitionRef).potentialImpacts
        result[7] == b5
        result[8] == c4
        result[9] == c5
        result[10] == b2//has the 1 impact inherited from b1
        result[10].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues1.get(riskDefinitionRef).potentialImpacts
        result[11] == a2
        result[12] == a3
        result[13] == a4
        result[27] == c5

        and:
        verifyAll {
            queryCounts.select == 24
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 100
        }

        when: "We change the impact of c1"
        log.debug("---------------------------------------------")
        c1 = executeInTransaction {
            c1 = assetDataRepository.findById(c1.idAsUUID).get()
            c1.setImpactValues(domain, impactValues2)
            assetDataRepository.save(c1)
        }

        and: "recalculate the impact for c1"
        QueryCountHolder.clear()
        result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, c1)
        }
        queryCounts = QueryCountHolder.grandTotal

        then: "the linked a1-a5, b1-b5 and c4 and c5 are affected and the impact is 2"
        result.size() == 5
        result[0] == c1
        result[0].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues2.get(riskDefinitionRef).potentialImpacts
        result[1] == c2
        result[2] == c3
        result[3] == c4
        result[4] == c5

        and:
        verifyAll {
            queryCounts.select == 16
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 100
        }

        when: "We change the impact of b3"
        log.debug("---------------------------------------------")
        b3 = executeInTransaction {
            b3 = assetDataRepository.findById(b3.idAsUUID).get()
            b3.setImpactValues(domain, impactValues3)
            assetDataRepository.save(b3)
        }

        and: "recalculate the impact for b3"
        QueryCountHolder.clear()
        result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, b3)
        }
        queryCounts = QueryCountHolder.grandTotal

        then: "the linked elements are updated the impact is 3"
        result.size() == 9
        result[0] == b3
        result[0].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues3.get(riskDefinitionRef).potentialImpacts
        result[1] == b4
        result[2] == b5
        result[2].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues3.get(riskDefinitionRef).potentialImpacts
        result[2].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues3.get(riskDefinitionRef).potentialImpacts
        result[3] == c4
        result[4] == c5
        result[4].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues3.get(riskDefinitionRef).potentialImpacts
        result[4].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues3.get(riskDefinitionRef).potentialImpacts
        result[5] == c2
        result[5].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues3.get(riskDefinitionRef).potentialImpacts
        result[6] == c3
        result[6].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues3.get(riskDefinitionRef).potentialImpacts
        result[7] == c4
        result[8] == c5

        and:
        verifyAll {
            queryCounts.select == 19
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 100
        }
    }

    def "a big graph"() {
        given: "A graph with 702 elements and 500 links"
        def a0 = assetDataRepository.save(newAsset(unit) {
            name = "A-0"
            associateWithDomain(domain, "AST_Application", "NEW")
        })

        Asset simpleAsset = a0
        List<Asset> assets = new ArrayList<>(100)
        (1..100).each { index ->
            simpleAsset = buildAssetList(simpleAsset, unit, domain, "A-"+index)
            assets.add(simpleAsset)
        }
        def b0 = assetDataRepository.save(newAsset(unit) {
            name = "B-0"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
        })
        (1..100).each { index ->
            buildAssetList(b0, unit, domain, "B-"+index)
        }

        (1..100).each { index ->
            def c0 = assetDataRepository.save(newAsset(unit) {
                name = "C-"+index
                associateWithDomain(domain, "AST_Application", "NEW")
                setImpactValues(domain, impactValues0)
            })

            buildAssetList(c0, unit, domain, "C-1-"+index)
        }
        Process process
        List<Process> processes = new ArrayList<>(100)
        List<Process> processes1 = new ArrayList<>(100)
        (1..100).each { index ->
            process = processDataRepository.save( newProcess(unit) {
                name = "p-"+index
                associateWithDomain(domain, "NormalProcess", "NEW")
            })
            processes.add(process)
            Asset sys = assetDataRepository.save(newAsset(unit) {
                name = "SYS-"+index
                associateWithDomain(domain, "AST_Application", "NEW")
                setImpactValues(domain, impactValues0)
            })

            Process process1 = processDataRepository.save(newProcess(unit) {
                name = "p-1-"+index
                associateWithDomain(domain, "NormalProcess", "NEW")
                links = [
                    newCustomLink(sys, "process_requiredITSystems", domain),
                    newCustomLink(process, "process_PIADataProcessing", domain)
                ]
            })
            processes1.add(process1)
            Scope scope = scopeDataRepository.save(newScope(unit) {
                name = "s-"+index
                associateWithDomain(domain, "NormalProcess", "NEW")
            })
        }

        log.debug("---------------------------------------------")

        when: "change the impact of process p-1-100"
        process = executeInTransaction {
            process = processDataRepository.findById(processes1[99].idAsUUID).get()
            process.setImpactValues(domain, impactValues1)
            processDataRepository.save(process)
        }
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, process)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then: "three elements are affected (p-100, p-1-100 and SYS-100)"
        result.size() == 3
        result[0] == process
        result[1].name == "SYS-100"
        result[2].name == "p-100"
        result[2].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues1.get(riskDefinitionRef).potentialImpacts
        result[2].getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues1.get(riskDefinitionRef).potentialImpacts

        and: "one impact was insert"
        verifyAll {
            queryCounts.select == 15
            queryCounts.insert == 1
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 300
        }

        when: "recalculate the impact of a simple Asset"
        a0 = executeInTransaction {
            a0 = assetDataRepository.findById(a0.idAsUUID).get()
            a0.setImpactValues(domain, impactValues2)
            assetDataRepository.save(a0)
        }
        simpleAsset = executeInTransaction {
            simpleAsset = assetDataRepository.findById(simpleAsset.idAsUUID).get()
            simpleAsset.setImpactValues(domain, impactValues2)
            assetDataRepository.save(simpleAsset)
        }

        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, a0)
        }
        queryCounts = QueryCountHolder.grandTotal

        then: "one impact is set"
        result.size() == 1
        result[0] == a0
        a0.getImpactValues(domain, riskDefinitionRef).get().getPotentialImpactsEffective() == impactValues2.get(riskDefinitionRef).potentialImpacts

        and:
        verifyAll {
            queryCounts.select == 11
            queryCounts.insert == 0
            queryCounts.update == 0
            queryCounts.delete == 0
            queryCounts.time < 700
        }

        when: "recalculate the impact of a A-50"
        def asset50 = executeInTransaction {
            Asset n = assetDataRepository.findById(assets[50].idAsUUID).get()
            n.setImpactValues(domain, impactValues2)
            assetDataRepository.save(n)
        }
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, asset50)
        }
        queryCounts = QueryCountHolder.grandTotal

        then: "51 assets where changed"
        result.size() == 52

        and:
        verifyAll {
            queryCounts.select == 62
            queryCounts.insert == 2
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 400
        }
    }

    def "a big connected graph"() {
        given: "A graph with 402 elements and 500 links"
        def a0 = assetDataRepository.save(newAsset(unit) {
            name = "A-0"
            associateWithDomain(domain, "AST_Application", "NEW")
        })
        Asset currentAAsset = a0
        List<Asset> assetsA = new ArrayList<>(100)
        (1..100).each { index ->
            currentAAsset = buildAssetList(currentAAsset, unit, domain, "A-"+index)
            assetsA.add(currentAAsset)
        }

        Asset b0 = assetDataRepository.save(newAsset(unit) {
            name = "B-0"
            associateWithDomain(domain, "AST_Application", "NEW")
        })
        Asset currentBAsset = b0
        List<Asset> assetsB = new ArrayList<>(100)
        (1..100).each { index ->
            currentBAsset = buildAssetList(currentBAsset, unit, domain, "B-"+index)
            assetsB.add(currentBAsset)
        }

        Asset c0 = assetDataRepository.save(newAsset(unit) {
            name = "C-NOT-CONNECTED"
            associateWithDomain(domain, "AST_Application", "NEW")
        })
        Asset currentCAsset = c0
        List<Asset> assetsC = new ArrayList<>(100)
        (1..100).each { index ->
            c0 = assetDataRepository.save(newAsset(unit) {
                name = "C-"+index
                associateWithDomain(domain, "AST_Application", "NEW")
            })
            currentCAsset = buildAssetList(c0, unit, domain, "C-1-"+index)
            assetsC.add(currentCAsset)
        }

        // Create a juncture by linking A-49 to B-50
        // Create a join by linking B-89 to A-90
        assetsA[48].applyLink(newCustomLink(assetsB[47], "asset_asset_app", domain))
        assetsB[88].applyLink(newCustomLink(assetsA[87], "asset_asset_app", domain))
        assetDataRepository.save(assetsA[48])
        assetDataRepository.save(assetsB[88])

        log.debug("all saved elements {}", assetDataRepository.count())

        when: "recalculate the impact of a A-50"
        def asset50 = executeInTransaction {
            Asset n = assetDataRepository.findById(assetsA[49].idAsUUID).get()
            n.setImpactValues(domain, impactValues2)
            assetDataRepository.save(n)
        }
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, asset50)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then: "100 assets where changed"
        result.size() == 100
        result[0] == asset50
        result[50] == a0
        result[51] == assetsB[47]
        result[99] == b0

        and:
        verifyAll {
            queryCounts.select == 111
            queryCounts.insert == 4
            queryCounts.update == 0
            queryCounts.delete == 0
            queryCounts.time < 400
        }

        when: "recalculate for whole unit"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        result = executeInTransaction {
            impactInheritanceCalculator.updateAllRootNodes(unit, domain, riskDefinitionId)
        }
        queryCounts = QueryCountHolder.grandTotal
        def map = result.stream().collect(Collectors.groupingBy(Function.identity()))
        def elementByCount = map.values().stream().collect(Collectors.toMap({
            it.first()
        }, {
            it.size()
        }))

        def summary = elementByCount.entrySet().stream().collect(Collectors.summarizingInt({ it.getValue() }))
        log.debug("summary {}",summary)

        //402 relvant assets
        //B-48 -- B-0 3 times

        then: "402 unique assets where changed"
        elementByCount.entrySet().size() == 402
        summary.count == 402

        and:
        verifyAll {
            queryCounts.select == 409
            queryCounts.insert == 11
            queryCounts.update == 0
            queryCounts.delete == 0
            queryCounts.time < 400
        }
    }

    @Unroll
    // Test impact inheritance in this graph:
    // ```mermaid
    //graph TD
    //    A(Asset 0) -->|asset_asset_app| B(Asset 1)
    //    B -->|asset_asset_app| C(Asset 2)
    //    B -->|asset_asset_noninheriting| F(Asset 5)
    //    C -->|asset_asset_app| D(Asset 3)
    //    F -->|asset_asset_noninheriting| G(Asset 6)
    //    G -->|asset_asset_noninheriting| D
    //    D -->|asset_asset_app| E(Asset 4)
    //    B -->|asset_asset_app| H(Asset 7)
    //    H -->|asset_asset_app| D
    //```
    def "Impact #checkImpact was correctly determined for element no. #checkAsset when changing impact to #firstImpact / #secondImpact for element numbers #firstAsset / #secondAsset because #reasons"() {
        given:
        txTemplate.execute {
            dataDrivenAssets = [] as List
            (0..7).each { i ->
                dataDrivenAssets << newAsset(unit) {
                    associateWithDomain(domain, "AST_Application", "NEW")
                    id = Key.newUuid()
                    name = "Asset-${i}"
                }
            }
            dataDrivenAssets = dataDrivenAssets.collect { assetDataRepository.save(it) }

            dataDrivenAssets[0].setLinks([
                newCustomLink(dataDrivenAssets[1], "asset_asset_app", domain)
            ] as Set)
            dataDrivenAssets[1].setLinks([
                newCustomLink(dataDrivenAssets[2], "asset_asset_app", domain),
                newCustomLink(dataDrivenAssets[7], "asset_asset_app", domain),
                newCustomLink(dataDrivenAssets[5], "asset_asset_noninheriting", domain)
            ] as Set)
            dataDrivenAssets[2].setLinks([
                newCustomLink(dataDrivenAssets[3], "asset_asset_app", domain)
            ] as Set)
            dataDrivenAssets[5].setLinks([
                newCustomLink(dataDrivenAssets[6], "asset_asset_noninheriting", domain)
            ] as Set)
            dataDrivenAssets[6].setLinks([
                newCustomLink(dataDrivenAssets[3], "asset_asset_noninheriting", domain)
            ] as Set)
            dataDrivenAssets[3].setLinks([
                newCustomLink(dataDrivenAssets[4], "asset_asset_app", domain)
            ] as Set)
            dataDrivenAssets[7].setLinks([
                newCustomLink(dataDrivenAssets[3], "asset_asset_app", domain)
            ] as Set)
            dataDrivenAssets = dataDrivenAssets.collect {  assetDataRepository.save(it) }
        }

        when:
        txTemplate.execute {

            def first = this.dataDrivenAssets[firstAsset]
            first = assetDataRepository.findById(first.idAsUUID).get()
            first.setImpactValues(domain, newImpactValue(confidentialityRef, firstImpact as String))
            first = assetDataRepository.save(first)
            log.debug("-> {}", first.name)

            def second = this.dataDrivenAssets[secondAsset]
            second = assetDataRepository.findById(second.idAsUUID).get()
            second.setImpactValues(domain, newImpactValue(confidentialityRef, secondImpact as String))
            second = assetDataRepository.save(second)

            log.debug("{}", second.name)

            def changed = impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, first)
            changed.forEach {assetDataRepository.save(it)}

            this.dataDrivenAssets = this.dataDrivenAssets
                    .collect {
                        assetDataRepository.findById(it.idAsUUID).get()
                    }
            this.dataDrivenAssets*.getImpactValues(domain) // hydrate values
        }

        then:
        if (checkImpact == null) {
            assert this.dataDrivenAssets[checkAsset].getImpactValues(domain, riskDefinitionRef).isEmpty()
        } else {
            assert this.dataDrivenAssets[checkAsset].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated
            .get(confidentialityRef).getIdRef() == checkImpact
            assert this.dataDrivenAssets[checkAsset].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated
            .get(integrityRef).getIdRef() == 2
        }

        where:
        firstAsset | firstImpact |  secondAsset | secondImpact | checkAsset | checkImpact | reasons
        0          | 3           |  5           | 1            | 4          | 3           | "5 is not in inheriting path"
        0          | 3           |  2           | 1            | 4          | 3           | "3 takes precedence using path over 7"
        0          | 3           |  3           | 1            | 4          | 1           | "value from 3 overwrites previous values"
        1          | 1           |  5           | 3            | 4          | 1           | "link from 5 is not inheriting impact"
        5          | 1           |  1           | 3            | 4          | null        | "calculation stops at 5 because no links to follow"
        1          | 2           |  5           | 1            | 2          | 2           | "parallel path 1 followed"
        1          | 2           |  5           | 1            | 7          | 2           | "parallel path 2 followed"
        2          | 2           |  7           | 3            | 4          | 3           | "higher value from 7 takes precedence"
        2          | 2           |  7           | 1            | 4          | 2           | "higher value from 2 takes precedence"
        0          | 1           |  1           | 2            | 5          | null        | "5 is not inheriting impact"
        0          | 1           |  1           | 2            | 6          | null        | "6 is not inheriting impact"
    }

    @Unroll
    // Test impact inheritance in this graph:
    // ```mermaid
    //graph TD
    //    A(Asset 0) -->|asset_asset_app| B(Asset 1)
    //    B -->|asset_asset_app| C(Asset 2)
    //    B -->|asset_asset_noninheriting| F(Asset 5)
    //    C -->|asset_asset_app| D(Asset 3)
    //    F -->|asset_asset_noninheriting| G(Asset 6)
    //    G -->|asset_asset_noninheriting| D
    //    D -->|asset_asset_app| E(Asset 4)
    //    B -->|asset_asset_app| H(Asset 7)
    //    H -->|asset_asset_app| D
    //    D -->|asset_asset_app| B
    //```
    def "Circle test: impact #checkImpact was correctly determined for element no. #checkAsset when changing impact to #firstImpact / #secondImpact for element numbers #firstAsset / #secondAsset because #reasons"() {
        given:
        txTemplate.execute {
            dataDrivenAssets = [] as List
            (0..7).each { i ->
                dataDrivenAssets << newAsset(unit) {
                    associateWithDomain(domain, "AST_Application", "NEW")
                    id = Key.newUuid()
                    name = "Asset-${i}"
                }
            }
            dataDrivenAssets = dataDrivenAssets.collect { assetDataRepository.save(it) }

            dataDrivenAssets[0].setLinks([
                newCustomLink(dataDrivenAssets[1], "asset_asset_app", domain)
            ] as Set)
            dataDrivenAssets[1].setLinks([
                newCustomLink(dataDrivenAssets[2], "asset_asset_app", domain),
                newCustomLink(dataDrivenAssets[7], "asset_asset_app", domain),
                newCustomLink(dataDrivenAssets[5], "asset_asset_noninheriting", domain)
            ] as Set)
            dataDrivenAssets[2].setLinks([
                newCustomLink(dataDrivenAssets[3], "asset_asset_app", domain)
            ] as Set)
            dataDrivenAssets[5].setLinks([
                newCustomLink(dataDrivenAssets[6], "asset_asset_noninheriting", domain)
            ] as Set)
            dataDrivenAssets[6].setLinks([
                newCustomLink(dataDrivenAssets[3], "asset_asset_noninheriting", domain)
            ] as Set)
            dataDrivenAssets[3].setLinks([
                newCustomLink(dataDrivenAssets[4], "asset_asset_app", domain),
                newCustomLink(dataDrivenAssets[1], "asset_asset_app", domain)
            ] as Set)
            dataDrivenAssets[7].setLinks([
                newCustomLink(dataDrivenAssets[3], "asset_asset_app", domain)
            ] as Set)
            dataDrivenAssets = dataDrivenAssets.collect {  assetDataRepository.save(it) }
        }

        when:
        txTemplate.execute {

            def first = this.dataDrivenAssets[firstAsset]
            first = assetDataRepository.findById(first.idAsUUID).get()
            first.setImpactValues(domain, newImpactValue(confidentialityRef, firstImpact as String))
            first = assetDataRepository.save(first)
            log.debug("-> {}", first.name)

            def second = this.dataDrivenAssets[secondAsset]
            second = assetDataRepository.findById(second.idAsUUID).get()
            second.setImpactValues(domain, newImpactValue(confidentialityRef, secondImpact as String))
            second = assetDataRepository.save(second)

            log.debug("{}", second.name)

            try {
                def changed = impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, first)
                changed.forEach {assetDataRepository.save(it)}
            } catch (ImpactInheritanceCircleException e) {
                log.debug("Ignoring exception ", e)
            }

            this.dataDrivenAssets = this.dataDrivenAssets
                    .collect {
                        assetDataRepository.findById(it.idAsUUID).get()
                    }
            this.dataDrivenAssets*.getImpactValues(domain) // hydrate values
        }

        then:
        if (checkImpact == null) {
            assert this.dataDrivenAssets[checkAsset].getImpactValues(domain, riskDefinitionRef).isEmpty()
        } else {
            assert this.dataDrivenAssets[checkAsset].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated
            .get(confidentialityRef).getIdRef() == checkImpact
            assert this.dataDrivenAssets[checkAsset].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated
            .get(integrityRef).getIdRef() == 2
        }

        where:
        firstAsset | firstImpact |  secondAsset | secondImpact | checkAsset | checkImpact | reasons
        0          | 3           |  1           | 1            | 2          | null        | "loop was detected"
        1          | 3           |  0           | 1            | 2          | null        | "loop was detected"
        0          | 3           |  0           | 1            | 3          | null        | "loop was detected"
        1          | 3           |  0           | 1            | 3          | null        | "loop was detected"
    }

    def "determine the value in a simple list for complete unit"() {
        given: "A chain of asset with an impact at the leaf"
        def a1 = assetDataRepository.save(newAsset(unit) {
            name = "a1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
        })
        def a2 = buildAssetListOpposite(a1, unit, domain,"a2",impactValuesEmpty)
        def a3 = buildAssetListOpposite(a2, unit, domain,"a3",impactValuesEmpty)
        def a4 = buildAssetListOpposite(a3, unit, domain,"a4",impactValuesEmpty)
        def a5 = buildAssetListOpposite(a4, unit, domain,"a5",impactValuesEmpty)

        when: "we calculate for all roots"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.updateAllRootNodes(unit, domain, riskDefinitionId)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then:"all impacts are changed in the chain"
        result.size() == 5
        result[0] == a1
        result[1].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues0.get(riskDefinitionRef).potentialImpacts
        result[2].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues0.get(riskDefinitionRef).potentialImpacts
        result[3].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues0.get(riskDefinitionRef).potentialImpacts
        result[4].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues0.get(riskDefinitionRef).potentialImpacts
        result[4] == a5

        and:
        verifyAll {
            queryCounts.select == 12
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 25
        }
    }

    def "determine the value of a single element for complete unit"() {
        def a1 = assetDataRepository.save(newAsset(unit) {
            name = "a1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
        })
        assetDataRepository.save(a1)

        when: "we calculate for all roots"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.updateAllRootNodes(unit, domain, riskDefinitionId)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then:"no impacts are changed"
        result.size() == 0
    }

    def "determine the value in a simple circle for complete unit"() {
        def a1 = assetDataRepository.save(newAsset(unit) {
            name = "a1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
        })
        def a2 = assetDataRepository.save(newAsset(unit) {
            name = "a2"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
        })
        def a3 = assetDataRepository.save(newAsset(unit) {
            name = "a3"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues0)
        })

        a1.setLinks([
            newCustomLink(a2, "asset_asset_app", domain)
        ] as Set)
        a2.setLinks([
            newCustomLink(a3, "asset_asset_app", domain)
        ] as Set)
        a3.setLinks([
            newCustomLink(a1, "asset_asset_app", domain)
        ] as Set)

        assetDataRepository.save(a1)
        assetDataRepository.save(a2)
        assetDataRepository.save(a3)

        when: "we calculate for all roots"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.updateAllRootNodes(unit, domain, riskDefinitionId)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then:"all impacts are changed in the circle"
        result.size() == 3

        when: "we add to roots"
        def r1 = assetDataRepository.save(newAsset(unit) {
            name = "r1"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues1)
        })
        def r2 = assetDataRepository.save(newAsset(unit) {
            name = "r2"
            associateWithDomain(domain, "AST_Application", "NEW")
            setImpactValues(domain, impactValues2)
        })
        r1.setLinks([
            newCustomLink(a1, "asset_asset_app", domain)
        ] as Set)
        r2.setLinks([
            newCustomLink(a3, "asset_asset_app", domain)
        ] as Set)
        assetDataRepository.save(r1)
        assetDataRepository.save(r2)

        and: "we calculate all roots in the unit"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        result = executeInTransaction {
            impactInheritanceCalculator.updateAllRootNodes(unit, domain, riskDefinitionId)
        }
        queryCounts = QueryCountHolder.grandTotal

        then:"the roots and their paths are returned"
        result.size() == 4
        result[0] == r1
        result[1] == a1
        result[1].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == [:]
        result[2] == r2
        result[3] == a3
        result[3].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == [:]

        and:
        verifyAll {
            queryCounts.select == 12
            queryCounts.insert == 0
            queryCounts.update == 0
            queryCounts.delete == 0
            queryCounts.time < 35
        }
    }

    def "determine the values in in two simple lines for the complete unit"() {
        given: "Independent lines"
        def line1 = [] as List
        (0..9).each { i ->
            line1 << newAsset(unit) {
                associateWithDomain(domain, "AST_Application", "NEW")
                id = Key.newUuid()
                name = "Line1-${i}"
            }
        }
        line1 = line1.collect { assetDataRepository.save(it) }
        (0..8).each { i ->
            line1[i].setLinks([
                newCustomLink(line1[i+1], "asset_asset_app", domain)
            ] as Set)
        }
        line1 = line1.collect { assetDataRepository.save(it) }

        line1[0].setImpactValues(domain, impactValues2)
        assetDataRepository.save(line1[0])

        def line2 = [] as List
        (0..9).each { i ->
            line2 << newAsset(unit) {
                associateWithDomain(domain, "AST_Application", "NEW")
                id = Key.newUuid()
                name = "Line2-${i}"
            }
        }
        line2 = line2.collect { assetDataRepository.save(it) }
        (0..8).each { i ->
            line2[i].setLinks([
                newCustomLink(line2[i+1], "asset_asset_app", domain)
            ] as Set)
        }
        line2 = line2.collect { assetDataRepository.save(it) }
        line2[0].setImpactValues(domain, impactValues3)
        assetDataRepository.save(line2[0])

        when: "we calculate for all roots in unit"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.updateAllRootNodes(unit, domain, riskDefinitionId)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then: "the roots and their path are returned"
        result.size() == 20
        result[0] == line1[0]
        result[10] == line2[0]
        result[1].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues2.get(riskDefinitionRef).potentialImpacts
        result[9].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues2.get(riskDefinitionRef).potentialImpacts
        result[11].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues3.get(riskDefinitionRef).potentialImpacts
        result[19].getImpactValues(domain, riskDefinitionRef).get().potentialImpactsCalculated == impactValues3.get(riskDefinitionRef).potentialImpacts

        and:
        verifyAll {
            queryCounts.select == 27
            queryCounts.insert == 1
            queryCounts.update == 0
            queryCounts.delete == 0
            queryCounts.time < 50
        }

        when: "we calculate all roots for a unit"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        executeInTransaction {
            impactInheritanceCalculator.updateAllRootNodes(unit)
        }
        queryCounts = QueryCountHolder.grandTotal

        then:"no result is returned"
        verifyAll {
            queryCounts.select == 27
            queryCounts.insert == 0
            queryCounts.update == 0
            queryCounts.delete == 0
            queryCounts.time < 40
        }

        when: "we calculate all roots for a second domain with no risk definition"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        executeInTransaction {
            impactInheritanceCalculator.updateAllRootNodes(unit, secondDomain)
        }
        queryCounts = QueryCountHolder.grandTotal

        then:"no result is returned"
        verifyAll {
            queryCounts.select == 0
            queryCounts.insert == 0
            queryCounts.update == 0
            queryCounts.delete == 0
            queryCounts.time < 20
        }
    }

    def "determine the value for single element in a unit"() {
        given: "a single asset"
        def asset = newAsset(unit) {
            associateWithDomain(domain, "AST_Application", "NEW")
            id = Key.newUuid()
            name = "single asset"
        }
        asset.setImpactValues(domain, impactValues3)
        asset = assetDataRepository.save(asset)

        when: "we calculate for an element"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        def result = executeInTransaction {
            impactInheritanceCalculator.calculateImpactInheritance(unit, domain, riskDefinitionId, asset)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then:"no result is returned"
        result.size() == 0

        and:
        verifyAll {
            queryCounts.select == 14
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 10
        }

        when: "we calculate for all roots in domain and riskdefinition"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        result = executeInTransaction {
            impactInheritanceCalculator.updateAllRootNodes(unit, domain, riskDefinitionId)
        }
        queryCounts = QueryCountHolder.grandTotal

        then:"no result is returned"
        result.size() == 0

        and:
        verifyAll {
            queryCounts.select == 1
            queryCounts.insert == 0
            queryCounts.update == 0
            queryCounts.delete == 0
            queryCounts.time < 10
        }

        when: "we calculate for all roots in the unit"
        log.debug("---------------------------------------------")
        QueryCountHolder.clear()
        executeInTransaction {
            impactInheritanceCalculator.updateAllRootNodes(unit)
        }
        queryCounts = QueryCountHolder.grandTotal

        then:"no result is returned"

        and:
        verifyAll {
            queryCounts.select == 1
            queryCounts.insert == 0
            queryCounts.update == 0
            queryCounts.delete == 0
            queryCounts.time < 10
        }
    }

    private void listLinks(Element a) {
        a.links.forEach{
            log.debug("{}->{}", it.source.designator,it.target.designator)
            listLinks(it.target)
        }
    }

    private Asset buildAssetList(Asset source, Unit unit, Domain domain,String elementName, impactValues = null, link = "asset_asset_app") {
        def target = newAsset(unit) {
            name = elementName
            associateWithDomain(domain, "AST_Application", "NEW")
            if (impactValues != null) {
                setImpactValues(domain, impactValues)
            }
        }
        target.applyLink(newCustomLink(source, link, domain))
        return assetDataRepository.save(target)
    }

    protected newImpactValue(CategoryRef catRef, String i) {
        DomainRiskReferenceProvider riskreferenceProvider = DomainRiskReferenceProvider.referencesForDomain(domain)
        def impactValue = riskreferenceProvider.getImpactRef(riskDefinitionRef.idRef, catRef.idRef, new BigDecimal(i))
        def defaultIntegrityImpact = riskreferenceProvider.getImpactRef(riskDefinitionRef.idRef, integrityRef.idRef, new BigDecimal("2"))
        // add integrity plus requested values:
        // (integrity values must not interfere with confidentiality impact calculation)
        ImpactValues assetImpactValues = new ImpactValues([
            (catRef): impactValue,
            (integrityRef): defaultIntegrityImpact
        ])
        Map impactValues = [
            (riskDefinitionRef): assetImpactValues
        ]
        impactValues
    }

    private Asset buildAssetListOpposite(Asset source, Unit unit, Domain domain,String elementName, impactValues = null, link = "asset_asset_app") {
        def target = newAsset(unit) {
            name = elementName
            associateWithDomain(domain, "AST_Application", "NEW")
            if (impactValues != null) {
                setImpactValues(domain, impactValues)
            }
        }
        target = assetDataRepository.save(target)
        source.applyLink(newCustomLink(target, link, domain))
        assetDataRepository.save(source)
        return target
    }
}
