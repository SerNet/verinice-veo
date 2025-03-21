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
import org.veo.core.entity.EntityType
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

            applyElementTypeDefinition(newElementTypeDefinition("scenario", it) {
                subTypes = [
                    scenario: newSubTypeDefinition()
                ]
            })
            EntityType.RISK_AFFECTED_TYPES.forEach{ra ->
                applyElementTypeDefinition(newElementTypeDefinition(ra.singularTerm, it) {
                    subTypes = [
                        (ra.singularTerm): newSubTypeDefinition()
                    ]
                })

                newCatalogItem(it, {
                    name = ra.singularTerm
                    elementType = ra.singularTerm
                    subType = ra.singularTerm
                    status = "NEW"
                    aspects = new TemplateItemAspects([(rdRef):(impactValues())],null, null)
                })
            }
            newCatalogItem(it, {
                name = "scenario"
                elementType = "scenario"
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
                    elementType = "scenario"
                    subType = "scenario"
                    status = "NEW"
                    aspects = new TemplateItemAspects(null,
                            [(rdRef):(new PotentialProbability(new ProbabilityRef(new BigDecimal(2)), unitId))]
                            , null)
                })

                EntityType.RISK_AFFECTED_TYPES.forEach{ra ->

                    newProfileItem(p) {
                        name = ra.singularTerm
                        elementType = ra.singularTerm
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
    def "change a value in the riskmatrix"(EntityType type) {
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
            EntityType.RISK_AFFECTED_TYPES.forEach{ra ->
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
        type << EntityType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "add/remove riskmatrix"(EntityType type) {
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
            EntityType.RISK_AFFECTED_TYPES.forEach{ra ->
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
            EntityType.RISK_AFFECTED_TYPES.forEach{ra ->
                catalogItems.find{ it.elementType == ra.singularTerm}.aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]

                with(profiles_v2[0].items.find{ it.elementType == ra.singularTerm}) {
                    aspects.impactValues.r1d1.potentialImpacts == [A:2, C:2, D:2, I:2, R:2]
                    tailoringReferences[0].riskDefinitions.r1d1.categories.D == null
                }
            }
        }

        where:
        type << EntityType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "add/remove category"(EntityType type) {
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
            EntityType.RISK_AFFECTED_TYPES.forEach{ra ->
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
        type << EntityType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "add/remove risk value"(EntityType type) {
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
            EntityType.RISK_AFFECTED_TYPES.forEach{ra ->
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
        type << EntityType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "add probability value"(EntityType type) {
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
            EntityType.RISK_AFFECTED_TYPES.forEach{ra ->
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
        type << EntityType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "remove probability value"(EntityType type) {
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
            EntityType.RISK_AFFECTED_TYPES.forEach{ra ->
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
        type << EntityType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "add impact value"(EntityType type) {
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
            riskValues.size() == 2
            with(riskValues[0]) {
                category == 'R'
                inherentRisk == 2
                residualRisk == 2
            }
            with(riskValues[1]) {
                category == 'D'
                inherentRisk == null
                residualRisk == null
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
            EntityType.RISK_AFFECTED_TYPES.forEach{ra ->
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
        type << EntityType.RISK_AFFECTED_TYPES
    }

    @WithUserDetails("user@domain.example")
    def "remove impact value"(EntityType type) {
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
            EntityType.RISK_AFFECTED_TYPES.forEach{ra ->
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
        type << EntityType.RISK_AFFECTED_TYPES
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

    private String createRiskAffected(EntityType type) {
        return parseJson(post("/${type.pluralTerm}", [
            domains: [
                (domainId): [
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
                    ]
                ]
            ],
            name: "risk test $type",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
    }

    private String createScenario() {
        return parseJson(post("/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "scenario",
                    status: "NEW",
                    riskValues: [
                        r1d1 : [
                            potentialProbability: 2
                        ]
                    ]
                ]
            ]
        ])).resourceId
    }

    private void createRisk(EntityType type, String raId, String scenarioId) {
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
