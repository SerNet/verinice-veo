/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Urs Zeidler
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
package org.veo.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.RiskTailoringReferenceValues
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.TemplateItemAspects
import org.veo.core.entity.Translated
import org.veo.core.entity.Unit
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.ImpactRef
import org.veo.core.entity.risk.ImpactValues
import org.veo.core.entity.risk.PotentialProbability
import org.veo.core.entity.risk.ProbabilityRef
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.entity.risk.RiskRef
import org.veo.core.entity.riskdefinition.CategoryLevel
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

class ChangeRiskDefininitionMvcITSpec  extends VeoMvcSpec {
    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    private Client client
    private Unit unit
    private Domain domain
    private String domainId
    private String unitId

    def setup() {
        client = createTestClient()
        domain = newDomain(client) {
            riskDefinitions = [
                "r1d1": createRiskDefinition("r1d1")
            ]

            def rd = riskDefinitions.get("r1d1")
            def rdRef = RiskDefinitionRef.from(rd)

            applyElementTypeDefinition(newElementTypeDefinition(ElementType.SCENARIO, it) {
                subTypes = [
                    scenario: newSubTypeDefinition()
                ]
            })
            ElementType.RISK_AFFECTED_TYPES.forEach{ra ->
                applyElementTypeDefinition(newElementTypeDefinition(ra, it) {
                    subTypes = [
                        (ra.singularTerm): newSubTypeDefinition()
                    ]
                })

                newCatalogItem(it, {
                    name = ra.singularTerm
                    elementType = ra
                    subType = ra.singularTerm
                    status = "NEW"
                    aspects = new TemplateItemAspects([(rdRef):(impactValues())],null, null)
                })
            }
            newCatalogItem(it, {
                name = "scenario"
                elementType = ElementType.SCENARIO
                subType = "scenario"
                status = "NEW"
                aspects = new TemplateItemAspects(null,
                        [(rdRef):(new PotentialProbability(new ProbabilityRef(new BigDecimal(2)), unitId))]
                        , null)
            })

            newProfile(it,{p->
                name = "test-profile"
                description = "my description"
                language = "de_DE"

                def npi = newProfileItem(p,{
                    name = "scenario1"
                    elementType = ElementType.SCENARIO
                    subType = "scenario"
                    status = "NEW"
                    aspects = new TemplateItemAspects(null,
                            [(rdRef):(new PotentialProbability(new ProbabilityRef(new BigDecimal(2)), unitId))]
                            , null)
                })

                ElementType.RISK_AFFECTED_TYPES.forEach{ra ->

                    newProfileItem(p) {
                        name = ra.singularTerm
                        elementType = ra
                        subType = ra.singularTerm
                        status = "NEW"
                        aspects = new TemplateItemAspects([
                            (rdRef):(impactValues())
                        ],null, null)
                        addRiskTailoringReference(TailoringReferenceType.RISK, npi, null, null, [
                            (rdRef): new RiskTailoringReferenceValues(ProbabilityRef.from(rd.getProbability().getLevels().getFirst()), "explanation", [
                                (new CategoryRef("D")): new RiskTailoringReferenceValues.CategoryValues(createImpactRef(), "explanation",
                                RiskRef.from(rd.getRiskValues().first()), "residual", [] as Set, "riskTreahment")
                            ])])
                    }
                }
            })
        }
        domainId = domain.idAsString
        clientRepository.save(client)
        unit = newUnit(client)
        unitId = unitRepository.save(unit).idAsString
    }

    @WithUserDetails("user@domain.example")
    def "change a value in the riskmatrix"(ElementType type) {
        given: "a risk affected in a domain"
        def raId = createRiskAffected(type)
        def scenarioId = createScenario()

        when: "a risk is created with specific probability and impact"
        createRisk(type, raId, scenarioId)

        then: "the risk is calculated"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId")).domains.(domainId).riskDefinitions.r1d1) {
            riskValues.size() == 1
            riskValues[0].category=='D'
            riskValues[0].inherentRisk == 0
            riskValues[0].residualRisk == 0
        }

        when: "we change a risk matrix value"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            categories.find{ it.id == "D" }.valueMatrix[1][1].ordinalValue = 3
            categories.find{ it.id == "D" }.valueMatrix[1][1].symbolicRisk = "symbolic_risk_4"
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then: "the risk was recalculated"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId")).domains.(domainId).riskDefinitions.r1d1) {
            riskValues.size() == 1
            riskValues[0].category=='D'
            riskValues[0].inherentRisk == 3
            riskValues[0].residualRisk == 3
        }

        and: "The scenario's potentialProbability is unchanged"
        parseJson(get("/scenarios/$scenarioId")).domains.(domainId).riskValues.r1d1.potentialProbability == 2

        and:"the catalogItems and profileItems are unchanged"
        with(parseJson(get("/domains/${domainId}/export"))) {
            ElementType.RISK_AFFECTED_TYPES.forEach{ra ->
                catalogItems.find{ it.elementType == ra.singularTerm}.aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                with(profiles_v2[0].items.find{ it.elementType == ra.singularTerm}) {
                    aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                    tailoringReferences[0].riskDefinitions.r1d1.categories.D != null
                }
            }
            profiles_v2[0].items.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
            catalogItems.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
        }

        where:
        type << ElementType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "add/remove riskmatrix"(ElementType type) {
        given: "a risk affected in a domain"
        def raId = createRiskAffected(type)
        def scenarioId = createScenario()
        createRisk(type, raId, scenarioId)

        when: "we add a risk matrix to category R"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            categories.find{ it.id == "R" }.valueMatrix = [
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0],
                    riskValues[0],
                ],
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0],
                    riskValues[0],
                ],
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0],
                    riskValues[0],
                ],
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0],
                    riskValues[0],
                ],
            ]
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then: "the risk values are calculated for both categories"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId"))) {
            with(domains.(owner.domainId).riskDefinitions.r1d1) {
                riskValues.size() == 2
                with(riskValues[0]) {
                    category == 'D'
                    inherentRisk == 0
                    residualRisk == 0
                }
                with(riskValues[1]) {
                    category == 'R'
                    inherentRisk == 1
                    residualRisk == 1
                }
            }
        }

        when: "we remove the valueMatrix from category R"
        parseJson(get("/domains/$domainId")).riskDefinitions.r1d1.with {
            categories.find { it.id == "R" }.valueMatrix = null
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:], 200)
        }

        then: "the risk values are calculated and R is removed"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId"))) {
            with(domains.(owner.domainId).riskDefinitions.r1d1) {
                riskValues.size() == 1
                with(riskValues[0]) {
                    category == 'D'
                    inherentRisk == 0
                    residualRisk == 0
                }
            }
        }

        and:"the impacts are unchanged"
        with(parseJson(get("/$type.pluralTerm/$raId")).domains.(domainId).riskValues.r1d1.potentialImpacts) {
            D == 2
            C == 2
            I == 1
            A == 1
            R == 2
        }

        and:"the catalogItems are unchanged"
        with(parseJson(get("/domains/${domainId}/export"))) {
            ElementType.RISK_AFFECTED_TYPES.forEach{ra ->
                catalogItems.find{ it.elementType == ra.singularTerm}.aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                with(profiles_v2[0].items.find{ it.elementType == ra.singularTerm}) {
                    aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                    tailoringReferences[0].riskDefinitions.r1d1.categories.D != null
                }
            }
            profiles_v2[0].items.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
            catalogItems.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
        }

        when:"we remove the riskmatix for D"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with{
            categories.find{ it.id == "D" }.valueMatrix = null
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then:"the catalogItems are unchanged but the category D is removed in the tailoringRef"
        with(parseJson(get("/domains/${domainId}/export"))) {
            ElementType.RISK_AFFECTED_TYPES.forEach{ra ->
                catalogItems.find{ it.elementType == ra.singularTerm}.aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]

                with(profiles_v2[0].items.find{ it.elementType == ra.singularTerm}) {
                    aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                    tailoringReferences[0].riskDefinitions.r1d1.categories.D == null
                }
            }
        }

        where:
        type << ElementType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "add/remove category"(ElementType type) {
        given: "a risk affected in a domain"
        def raId = createRiskAffected(type)
        def scenarioId = createScenario()
        createRisk(type, raId, scenarioId)

        when: "we add a category"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            categories.add(["id": "NEW",
                "translations":["EN": ["name": "new kid on the block"]],
                "potentialImpacts":[
                    ["ordinalValue":0,"htmlColor": "#101010"],
                    ["ordinalValue":1,"htmlColor": "#111111"]
                ],
                "valueMatrix":[
                    [
                        riskValues[0],
                        riskValues[0],
                        riskValues[0],
                        riskValues[0]
                    ],
                    [
                        riskValues[0],
                        riskValues[3],
                        riskValues[0],
                        riskValues[0]
                    ]
                ]
            ])
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then: "the risk category is present"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId")).domains.(domainId).riskDefinitions.r1d1) {
            riskValues.size() == 2
            with(riskValues[0]) {
                category=='D'
                inherentRisk == 0
                residualRisk == 0
            }
            with(riskValues[1]) {
                category=='NEW'
                inherentRisk == null
                residualRisk == null
            }
        }

        when:"we set the impact"
        get("/domains/$domainId/$type.pluralTerm/$raId").with {
            def ra = parseJson(it)
            ra.riskValues.r1d1.potentialImpacts = [C:2,I:2,A:2,R:2,NEW:1]
            put(ra._self, ra, ['If-Match': getETag(it)])
        }

        and: "we remove the categories C, I, A, R & D"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            categories = [
                categories.find{
                    it.id == "NEW"
                }
            ]
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then: "the risk values are calculated"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId")).domains.(domainId).riskDefinitions.r1d1) {
            riskValues.size() == 1
            with(riskValues[0]) {
                category == 'NEW'
                inherentRisk == 3
                residualRisk == 3
            }
        }

        and: "the impacts are gone"
        with(parseJson(get("/$type.pluralTerm/$raId")).domains.(domainId).riskValues.r1d1.potentialImpacts) {
            NEW == 1
        }

        and:"the impacts C, I, A, R, D are also gone in the catalogItems and the profileItems"
        with(parseJson(get("/domains/${domainId}/export"))) {
            ElementType.RISK_AFFECTED_TYPES.forEach{ra ->
                catalogItems.find{ it.elementType == ra.singularTerm}.aspects.impactValues.r1d1.potentialImpacts == [:]
                with(profiles_v2[0].items.find{ it.elementType == ra.singularTerm}) {
                    aspects.impactValues.r1d1.potentialImpacts == [:]
                    tailoringReferences[0].riskDefinitions.r1d1.categories == [:]
                }
            }
            profiles_v2[0].items.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
            catalogItems.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
        }

        where:
        type << ElementType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "add/remove risk value"(ElementType type) {
        given: "a risk affected in a domain"
        def raId = createRiskAffected(type)
        def scenarioId = createScenario()
        createRisk(type, raId, scenarioId)

        when: "we add a risk value"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            riskValues.add([ordinalValue: 4, symbolicRisk: "symbolic_risk_5"])
            categories.find{ it.id == "D" }.valueMatrix = [
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[4],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ]
            ]
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then: "the risk values are calculated"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId")).domains.(domainId).riskDefinitions.r1d1) {
            riskValues.size() == 1
            with(riskValues[0]) {
                category == 'D'
                inherentRisk == 4
                residualRisk == 4
            }
        }

        when: "we remove a risk value"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            riskValues.removeLast()
            categories.find{ it.id == "D" }.valueMatrix = [
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[3],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ]
            ]
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then: "the risk values are calculated"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId")).domains.(domainId).riskDefinitions.r1d1) {
            riskValues.size() == 1
            with(riskValues[0]) {
                category == 'D'
                inherentRisk == 3
                residualRisk == 3
            }
        }

        and:"the impacts are unchanged"
        with(parseJson(get("/$type.pluralTerm/$raId")).domains.(domainId).riskValues.r1d1.potentialImpacts) {
            D == 2
            C == 2
            I == 1
            A == 1
            R == 2
        }

        and:"the catalogItems are unchanged"
        with(parseJson(get("/domains/${domainId}/export"))) {
            ElementType.RISK_AFFECTED_TYPES.forEach{ra ->
                catalogItems.find{ it.elementType == ra.singularTerm}.aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                with(profiles_v2[0].items.find{ it.elementType == ra.singularTerm}) {
                    aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                    tailoringReferences[0].riskDefinitions.r1d1.categories.D != null
                }
            }
            profiles_v2[0].items.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
            catalogItems.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
        }

        where:
        type << ElementType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "add probability value"(ElementType type) {
        given: "a risk affected in a domain"
        def raId = createRiskAffected(type)
        def scenarioId = createScenario()
        createRisk(type, raId, scenarioId)

        when: "we add a probability value and change the matrix to conform"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            probability.levels.add([:])
            categories.find{ it.id == "D" }.valueMatrix = [
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
            ]
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then: "the risk values are cleared"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId")).domains.(domainId)) {
            riskDefinitions == [:]
        }

        and:"the impacts are unchanged"
        with(parseJson(get("/$type.pluralTerm/$raId")).domains.(domainId).riskValues.r1d1.potentialImpacts) {
            D == 2
            C == 2
            I == 1
            A == 1
            R == 2
        }

        and: "The scenario's potentialProbability is cleared"
        parseJson(get("/scenarios/$scenarioId")).domains.(domainId).riskValues.r1d1 ==[:]

        and:"the catalogItem impacts are unchanged, the risk tailoringReferences is cleared, the potentialProbability is also cleared"
        with(parseJson(get("/domains/${domainId}/export"))) {
            ElementType.RISK_AFFECTED_TYPES.forEach{ra ->
                catalogItems.find{ it.elementType == ra.singularTerm}.aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                with(profiles_v2[0].items.find{ it.elementType == ra.singularTerm}) {
                    aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                    tailoringReferences[0].riskDefinitions.r1d1.categories == [:]
                }
            }
            profiles_v2[0].items.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1 == [:]
            catalogItems.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1 == [:]
        }

        where:
        type << ElementType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "remove probability value"(ElementType type) {
        given: "a risk affected in a domain"
        def raId = createRiskAffected(type)
        def scenarioId = createScenario()
        createRisk(type, raId, scenarioId)

        when: "we add a probability value and change the matrix"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            probability.levels.removeLast()
            categories.find{ it.id == "D" }.valueMatrix = [
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0]
                ],
            ]
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then: "the values are cleared"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId")).domains.(domainId)) {
            riskDefinitions == [:]
        }

        and:"the impacts are unchanged"
        with(parseJson(get("/$type.pluralTerm/$raId")).domains.(domainId).riskValues.r1d1.potentialImpacts) {
            D == 2
            C == 2
            I == 1
            A == 1
            R == 2
        }

        and: "The scenario's potentialProbability is cleared"
        parseJson(get("/scenarios/$scenarioId")).domains.(domainId).riskValues.r1d1 == [:]

        and:"the catalogItem impacts are unchanged, the risk tailoringReferences is cleared, the potentialProbability is also cleared"
        with(parseJson(get("/domains/${domainId}/export"))) {
            ElementType.RISK_AFFECTED_TYPES.forEach{ra ->
                catalogItems.find{ it.elementType == ra.singularTerm}.aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                with(profiles_v2[0].items.find{ it.elementType == ra.singularTerm}) {
                    aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                    tailoringReferences[0].riskDefinitions.r1d1.categories == [:]
                }
            }
            profiles_v2[0].items.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1 == [:]
            catalogItems.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1 == [:]
        }

        where:
        type << ElementType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "add impact value"(ElementType type) {
        given: "a risk affected in a domain"
        def raId = createRiskAffected(type)
        def scenarioId = createScenario()
        createRisk(type, raId, scenarioId)

        when: "add another matrix"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            categories.find{ it.id == "R" }.valueMatrix = [
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[2],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ]
            ]
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then: "the both values are present"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId")).domains.(domainId).riskDefinitions.r1d1) {
            riskValues.size() == 2
            with(riskValues[0]) {
                category == 'D'
                inherentRisk == 0
                residualRisk == 0
            }
            with(riskValues[1]) {
                category == 'R'
                inherentRisk == 2
                residualRisk == 2
            }
        }

        when: "we add an impact value in category D and need to change the matrix"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            categories.find{ it.id == "D" }.potentialImpacts.add([:])
            categories.find{ it.id == "D" }.valueMatrix = [
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[0],
                    riskValues[0],
                    riskValues[0]
                ]
            ]
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then: "the value for D is cleared"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId")).domains.(domainId).riskDefinitions.r1d1) {
            riskValues.size() == 1
            with(riskValues[0]) {
                category == 'R'
                inherentRisk == 2
                residualRisk == 2
            }
        }

        and:"the impact D is removed"
        with(parseJson(get("/$type.pluralTerm/$raId")).domains.(domainId).riskValues.r1d1.potentialImpacts) {
            D == null
            C == 2
            I == 1
            A == 1
            R == 2
        }

        and:"the value for D is cleared in the catalogItems and profiles"
        with(parseJson(get("/domains/${domainId}/export"))) {
            ElementType.RISK_AFFECTED_TYPES.forEach{ra ->
                catalogItems.find{ it.elementType == ra.singularTerm}.aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                with(profiles_v2[0].items.find{ it.elementType == ra.singularTerm}) {
                    aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, I:2, R:2]
                    tailoringReferences[0].riskDefinitions.r1d1.categories == [:]
                }
            }
            profiles_v2[0].items.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
            catalogItems.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
        }

        where:
        type << ElementType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "remove impact value"(ElementType type) {
        given: "a risk affected in a domain"
        def raId = createRiskAffected(type)
        def scenarioId = createScenario()
        createRisk(type, raId, scenarioId)

        when: "we remove an impact value in category D and change the matrix"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            categories.find{ it.id == "D" }.potentialImpacts.removeLast()
            categories.find{ it.id == "D" }.valueMatrix = [
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0],
                    riskValues[0]
                ],
                [
                    riskValues[0],
                    riskValues[1],
                    riskValues[0],
                    riskValues[0]
                ]
            ]
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        then: "the risk values are cleared"
        with(parseJson(get("/$type.pluralTerm/$raId/risks/$scenarioId")).domains.(domainId)) {
            riskDefinitions == [:]
        }

        and:"the impact D is removed"
        with(parseJson(get("/$type.pluralTerm/$raId")).domains.(domainId).riskValues.r1d1.potentialImpacts) {
            D == null
            C == 2
            I == 1
            A == 1
            R == 2
        }

        and:"the category D in the catalogItems and profiles is removed"
        with(parseJson(get("/domains/${domainId}/export"))) {
            ElementType.RISK_AFFECTED_TYPES.forEach{ra ->
                catalogItems.find{ it.elementType == ra.singularTerm}.aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2,I:2, R:2]
                with(profiles_v2[0].items.find{ it.elementType == ra.singularTerm}) {
                    aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, I:2, R:2]
                    tailoringReferences[0].riskDefinitions.r1d1.categories == [:]
                }
            }
            profiles_v2[0].items.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
            catalogItems.find{ it.elementType == "scenario"}.aspects.scenarioRiskValues.r1d1.potentialProbability == 2
        }

        where:
        type << ElementType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "apply profile after adding a risk matrix"() {
        when: "we add a risk matrix to category R"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            categories.find{ it.id == "R" }.valueMatrix = [
                [
                    riskValues[2],
                    riskValues[2],
                    riskValues[2],
                    riskValues[2]
                ],
                [
                    riskValues[2],
                    riskValues[2],
                    riskValues[2],
                    riskValues[2]
                ],
                [
                    riskValues[2],
                    riskValues[2],
                    riskValues[2],
                    riskValues[2]
                ],
                [
                    riskValues[2],
                    riskValues[2],
                    riskValues[2],
                    riskValues[2]
                ]
            ]
            put("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1", it, [:],200)
        }

        and: "we apply the profile"
        def profiles = parseJson(get("/domains/${domainId}/profiles"))
        post("/domains/${domainId}/profiles/${profiles[0].id}/incarnation?unit=${unitId}",[:], 204)

        def retrievedElement = parseJson(get("/scopes")).items[0]
        def retrievedScenario = parseJson(get("/scenarios")).items[0]

        then: "the risk value for R is calculated"
        with(parseJson(get("/scopes/${retrievedElement.id}/risks/${retrievedScenario.id}")).domains.(domainId).riskDefinitions.r1d1) {
            with(riskValues.find{ it.category == 'R' }) {
                inherentRisk == 2
                residualRisk == 2
            }
            with(riskValues.find{ it.category == 'D' }) {
                inherentRisk == 1
                residualRisk == 0
            }
        }
    }

    @WithUserDetails("user@domain.example")
    def "use evaluation to edit a risk definition"() {
        given:
        def ret

        when: "we remove an impact value in category D"
        parseJson(get("/domains/${domainId}/risk-definitions/r1d1")).with {
            categories.find{ it.id == "D" }.potentialImpacts.removeLast()
            ret = parseJson(post("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1/evaluation", it, 200))
        }

        then: "the risk matrix is updated"
        with(ret.riskDefinition.categories.find{ it.id == "D" }) {
            valueMatrix.size() == 3
            valueMatrix*.size() as Set == [4] as Set
        }
        with(ret) {
            changes[0].changeType == "RiskMatrixResize"
            changes[0].categories == ["D"]
            changes[1].changeType == "ImpactListResize"
            changes[1].categories == ["D"]
            effects[0].description.en == "Risk values for category 'D' are removed from all risks."
            effects[1].description.en == "Impact values for category 'D' are removed from all assets, processes and scopes."
            validationMessages[0].description.en == "The following risk matrices have been resized, please adjust the risk values if necessary: [D]"
            validationMessages[0].changedCategories == ["D"]
        }

        when: "we add a probability value"
        parseJson(get("/domains/${domainId}/risk-definitions/r1d1")).with {
            probability.levels.add([:])
            probability.levels.add([:])
            ret = parseJson(post("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1/evaluation", it, 200))
        }

        then: "and the matrix conforms"
        with(ret.riskDefinition.categories.find{ it.id == "D" }) {
            valueMatrix.size() == 4
            valueMatrix[0].size() == 6
            valueMatrix*.size() as Set == [6] as Set
        }
        with(ret) {
            changes[0].changeType == "ProbabilityListResize"
            changes[0].categories == ["C", "I", "A", "R", "D"]
            changes[1].changeType == "RiskMatrixDiff"
            changes[1].categories == ["D"]
            effects[0].description.en == "Risk values for category 'C' are removed from all risks."
            effects[1].description.en == "Risk values for category 'I' are removed from all risks."
            effects[2].description.en == "Risk values for category 'A' are removed from all risks."
            effects[3].description.en == "Risk values for category 'R' are removed from all risks."
            effects[4].description.en == "Risk values for category 'D' are removed from all risks."
            validationMessages[0].description.en == "Risk matrices have been changed. Please adjust the risk values for the following criteria: [D]"
            validationMessages[0].changedCategories == ["D"]
            validationMessages[0].severity == "WARNING"
        }

        when: "we add a matrix to category C"
        parseJson(get("/domains/${domainId}/risk-definitions/r1d1")).with {
            categories.find{ it.id == "C" }.valueMatrix = []
            ret = parseJson(post("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1/evaluation", it, 200))
        }

        then: "the risk matrix is defined"
        with(ret) {
            with(riskDefinition.categories.find{ it.id == "C" }) {
                valueMatrix.size() == 4
                valueMatrix*.size() as Set == [4] as Set
                valueMatrix[0]*.ordinalValue as Set ==~ [
                    ret.riskDefinition.riskValues.last().ordinalValue
                ]
                valueMatrix[1]*.ordinalValue as Set ==~ [
                    ret.riskDefinition.riskValues.last().ordinalValue
                ]
                valueMatrix[2]*.ordinalValue as Set ==~ [
                    ret.riskDefinition.riskValues.last().ordinalValue
                ]
                valueMatrix[3]*.ordinalValue as Set ==~ [
                    ret.riskDefinition.riskValues.last().ordinalValue
                ]
            }
            changes[0].changeType == "RiskMatrixAdd"
            changes[0].categories == ["C"]
            changes[0].effects == null
            effects[0].description.en == "Risk values are recalculated."
            effects[1].description.en == "Risk values for category 'C' are added to risks."
        }

        when: "we add a matrix to all categories and clean existing"
        parseJson(get("/domains/${domainId}/risk-definitions/r1d1")).with {
            categories.each {  it.valueMatrix = [] }
            ret = parseJson(post("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1/evaluation", it, 200))
        }

        then: "the risk matrix is defined"
        with(ret.riskDefinition.categories.find{ it.id == "C" }) {
            valueMatrix.size() == 4
            valueMatrix*.size() as Set == [4] as Set
        }
        ret.changes*.changeType  ==~ [
            "RiskMatrixDiff",
            "RiskMatrixAdd",
            "RiskMatrixAdd",
            "RiskMatrixAdd",
            "RiskMatrixAdd"
        ]
        with(ret) {
            effects[0].description.en == "Risk values are recalculated."
            effects[1].description.en == "Risk values for category 'C' are added to risks."
            effects[2].description.en == "Risk values for category 'I' are added to risks."
            effects[3].description.en == "Risk values for category 'R' are added to risks."
            effects[4].description.en == "Risk values for category 'A' are added to risks."
            validationMessages[0].description.en == "Risk matrices have been changed. Please adjust the risk values for the following criteria: [D]"
            validationMessages[0].changedCategories == ["D"]
        }

        when: "we remove riskvalues"
        parseJson(get("/domains/${domainId}/risk-definitions/r1d1")).with {
            riskValues.removeLast()
            riskValues.removeLast()
            riskValues.removeLast()
            ret = parseJson(post("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1/evaluation", it, 200))
        }

        then: "the risk matrix is defined and all values are changed to 0"
        with(ret.riskDefinition.categories.find{ it.id == "D" }) {
            valueMatrix.size() == 4
            valueMatrix*.size() as Set ==~ [4]
            valueMatrix[0]*.ordinalValue as Set ==~ [0]
            valueMatrix[1]*.ordinalValue as Set ==~ [0]
            valueMatrix[2]*.ordinalValue as Set ==~ [0]
            valueMatrix[3]*.ordinalValue as Set ==~ [0]
        }
        ret.changes*.changeType as Set == [
            "RiskValueListResize",
            "RiskMatrixDiff"
        ] as Set

        with(ret) {
            validationMessages.size() == 1
            validationMessages[0].description.en == "Risk matrices have been changed. Please adjust the risk values for the following criteria: [D]"
            validationMessages[0].changedCategories ==~ ["D"]
        }

        when: "we remove all riskvalues"
        parseJson(get("/domains/${domainId}/risk-definitions/r1d1")).with {
            riskValues = []
            ret = parseJson(post("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1/evaluation", it, 200))
        }

        then: "all risk matrices are cleared"
        with(ret.riskDefinition) {
            categories*.valueMatrix as Set == [null, null, null, null] as Set
        }
        ret.changes*.changeType as Set == [
            "RiskValueListResize",
            "RiskMatrixRemove"
        ] as Set

        and: "the effect is stated"
        with(ret) {
            effects.size() == 1
            effects[0].description.en == "Risk values for category 'D' are removed from all risks."
            effects[0].category ==~ ["D"]
        }

        when: "we provide an empty matrix and no Impact"
        parseJson(get("/domains/${domainId}/risk-definitions/r1d1")).with {
            categories.find{ it.id == "D" }.valueMatrix = []
            categories.find{ it.id == "D" }.potentialImpacts = []
            ret = parseJson(post("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1/evaluation", it, 200))
        }

        then: "the risk matrix for D is cleared"
        with(ret.riskDefinition.categories.find{ it.id == "D" }) {
            valueMatrix == null
        }

        when: "we remove risk values, potential impacts, probabilities and categories"
        parseJson(get("/domains/${domainId}/risk-definitions/r1d1")).with {
            riskValues.removeLast()
            riskValues.removeLast()
            categories.find{ it.id == "D" }.potentialImpacts.removeLast()
            categories.find{ it.id == "D" }.potentialImpacts.removeLast()
            probability.levels.removeLast()
            probability.levels.removeLast()
            categories.removeFirst()
            categories.removeFirst()
            categories.removeFirst()
            categories.removeFirst()

            ret = parseJson(post("/content-customizing/domains/$owner.domainId/risk-definitions/r1d1/evaluation", it, 200))
        }

        then: "the risk matrix is defined and all values are changed to 0"
        with(ret.riskDefinition.categories.find{ it.id == "D" }) {
            valueMatrix.size() == 2
            valueMatrix*.size() ==~ [2, 2]
            valueMatrix[0]*.ordinalValue ==~ [0, 0]
            valueMatrix[1]*.ordinalValue ==~ [0, 0]
        }
        ret.changes.size() == 8
        ret.changes*.changeType ==~ [
            "RiskValueListResize",
            "RiskMatrixResize",
            "ProbabilityListResize",
            "ImpactListResize",
            "CategoryListRemove",
            "CategoryListRemove",
            "CategoryListRemove",
            "CategoryListRemove"
        ]
        with(ret) {
            effects.size() == 11
            effects[0].description.en == "Risk values for category 'C' are removed from all risks."
            effects[1].description.en == "Risk values for category 'I' are removed from all risks."
            effects[2].description.en == "Risk values for category 'A' are removed from all risks."
            effects[3].description.en == "Risk values for category 'R' are removed from all risks."
            effects[4].description.en == "Risk values for category 'D' are removed from all risks."
            effects[5].description.en == "Risk values are recalculated."
            effects[6].description.en == "Impact values for category 'C' are removed from all assets, processes and scopes."
            effects[7].description.en == "Impact values for category 'A' are removed from all assets, processes and scopes."
            effects[8].description.en == "Impact values for category 'R' are removed from all assets, processes and scopes."
            effects[9].description.en == "Impact values for category 'I' are removed from all assets, processes and scopes."
            effects[10].description.en == "Impact values for category 'D' are removed from all assets, processes and scopes."
            validationMessages[0].description.en == "The following risk matrices have been resized, please adjust the risk values if necessary: [D]"
            validationMessages[0].changedCategories ==~ ["D"]
        }

        when:"we use an undefined risk value"
        ret.riskDefinition.categories.first().valueMatrix[1][1] = [ordinalValue:100,
            htmlColor:"#noColor",
            translations: (ret.riskDefinition.riskValues[1].translations)
        ]
        ret = parseJson(post("/content-customizing/domains/$domainId/risk-definitions/r1d1/evaluation", ret.riskDefinition, 200))

        then: "the value is replaced by the current maximum"
        ret.riskDefinition.categories.first().valueMatrix[1][1].ordinalValue == 1

        when:"we make the riskvalues inconsitent"
        ret.riskDefinition.categories.first().valueMatrix[0][0] = ret.riskDefinition.riskValues[1]
        ret = parseJson(post("/content-customizing/domains/$domainId/risk-definitions/r1d1/evaluation", ret.riskDefinition, 200))

        then: "an evaluation message is produced"
        with(ret) {
            validationMessages.size() == 2
            effects.size() == 11
            changes.size() == 8
            with(validationMessages[0]) {
                description.de == "Die Risikomatrizen für die folgenden Kriterien sind inkonsistent: [D]"
                severity == "WARNING"
                changedCategories ==~ ["D"]
                column == 0
                row == 1
            }
            with(validationMessages[1]) {
                description.en == "The following risk matrices have been resized, please adjust the risk values if necessary: [D]"
                changedCategories ==~ ["D"]
            }
        }

        when:"we use the riskdef as new"
        ret = parseJson(post("/content-customizing/domains/$domainId/risk-definitions/new/evaluation", ret.riskDefinition, 200))

        then:"new riskdefinition is the change"
        with(ret) {
            changes.size() == 1
            changes*.changeType as Set ==~ ["NewRiskDefinition"]
            effects.size() == 0
            validationMessages.size() == 1
            with(validationMessages[0]) {
                description.de == "Die Risikomatrizen für die folgenden Kriterien sind inkonsistent: [D]"
                severity == "WARNING"
                changedCategories ==~ ["D"]
                column == 0
                row == 1
            }
        }

        when: "we use the riskdef to update the active riskdef"
        put("/content-customizing/domains/$domainId/risk-definitions/r1d1", ret.riskDefinition, [:],200)

        then:"this riskdefinition is active"
        parseJson(get("/domains/${domainId}")).riskDefinitions.r1d1.with {
            categories.size() == 1
        }
    }

    private static ImpactRef createImpactRef(c = new CategoryLevel(2, "", new Translated())) {
        return ImpactRef.from(c)
    }

    private ImpactValues impactValues() {
        return new ImpactValues([
            (new CategoryRef("C")):(createImpactRef()),
            (new CategoryRef("I")):(createImpactRef()),
            (new CategoryRef("A")):(createImpactRef()),
            (new CategoryRef("R")):(createImpactRef()),
            (new CategoryRef("D")):(createImpactRef())
        ])
    }

    private String createRiskAffected(ElementType type) {
        return parseJson(post("/domains/$domainId/${type.pluralTerm}", [
            subType: "${type.singularTerm}",
            status: "NEW",
            riskValues: [
                r1d1 : [
                    potentialImpacts: [
                        "D": 2,
                        "I": 1,
                        "C": 2,
                        "A": 1,
                        "R": 2
                    ]
                ]
            ],
            name: "risk test $type",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
    }

    private String createScenario() {
        return parseJson(post("/domains/$domainId/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "scenario",
            status: "NEW",
            riskValues: [
                r1d1 : [
                    potentialProbability: 2
                ]
            ]
        ])).resourceId
    }

    private void createRisk(ElementType type, String raId, String scenarioId) {
        post("/${type.pluralTerm}/$raId/risks", [
            domains : [
                (domainId): [
                    reference      : [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
                        r1d1: [
                            probability: [
                                specificProbability: 1
                            ],
                            impactValues: [
                                [
                                    category      : "D",
                                    specificImpact: 1
                                ],
                                [
                                    category      : "R",
                                    specificImpact: 2
                                ],
                                [
                                    category      : "C",
                                    specificImpact: 1
                                ],
                                [
                                    category      : "I",
                                    specificImpact: 1
                                ],
                                [
                                    category      : "A",
                                    specificImpact: 1
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ], 201)
    }
}
