/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler.
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

import java.time.Instant

import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.ReferenceTargetNotFoundException
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.core.entity.risk.DomainRiskReferenceProvider
import org.veo.core.entity.risk.ImpactValues
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ScenarioData
import org.veo.persistence.entity.jpa.ScopeData

/**
 * Test risk related functionality on controls.
 */
@WithUserDetails("user@domain.example")
class ScopeRiskValuesMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private ProcessRepositoryImpl processRepository

    @Autowired
    private ScopeRepositoryImpl scopeRepository

    @Autowired
    private ScenarioRepositoryImpl scenarioRepository

    private Client client
    private Unit unit
    private Domain domain
    private String unitId
    private String domainId
    private ScopeData scope
    private ScenarioData scenario

    def setup() {
        client = createTestClient()
        domain = newDomain(client) {
            riskDefinitions = [
                "r1d1": createRiskDefinition("r1d1"),
                "r2d2": createRiskDefinition("r2d2")
            ]
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.SCOPE, it) {
                subTypes = [
                    Difficultscope: newSubTypeDefinition()
                ]
            })
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.SCENARIO, it) {
                subTypes = [
                    BestCase: newSubTypeDefinition()
                ]
            })
        }
        domainId = domain.idAsString

        clientRepository.save(client)
        unit = newUnit(client)
        unitId = unitRepository.save(unit).idAsString

        RiskDefinitionRef riskDefinitionRef = new RiskDefinitionRef("r1d1")
        DomainRiskReferenceProvider riskreferenceProvider = DomainRiskReferenceProvider.referencesForDomain(domain)

        def categoryref = riskreferenceProvider.getCategoryRef(riskDefinitionRef.getIdRef(), "D")
        def impactValue = riskreferenceProvider.getImpactRef(riskDefinitionRef.getIdRef(), categoryref.getIdRef(), new BigDecimal("2"))
        ImpactValues scopeImpactValues = new ImpactValues([(categoryref) : impactValue])
        Map impactValues = [
            (riskDefinitionRef) : scopeImpactValues
        ]

        scope = newScope(unit) {
            associateWithDomain(domain, "Difficultscope", "NEW")
            setImpactValues(domain, impactValues)
        }
        scopeRepository.save(scope)

        scenario = newScenario(unit) {
            associateWithDomain(domain, "NormalScenario", "NEW")
        }
        scenarioRepository.save(scenario)
    }

    def "values can be set on a second risk definition"() {
        when: "creating a scope with risk values for two risk definitions"
        def scopeId = scope.getIdAsString()
        def scenarioId = scenario.getIdAsString()
        post("/scopes/$scopeId/risks", [
            domains: [
                (domainId): [
                    reference: [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
                        r1d1 : [
                            impactValues: [
                                [
                                    category: "D",
                                    specificImpact: 1
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ])

        and: "retrieving it"
        def getscopeRiskResponse = get("/scopes/$scopeId/risks/$scenarioId")
        def riskETag = getETag(getscopeRiskResponse)
        def retrievedscopeRisk = parseJson(getscopeRiskResponse)

        then: "all specified values were saved"
        def domain = retrievedscopeRisk.domains.get(domainId)
        def riskDef1Impact = domain.riskDefinitions.r1d1.impactValues
        riskDef1Impact.size() == 1
        with(riskDef1Impact.first()) {
            category=="D"
            specificImpact == 1
            effectiveImpact == 1
        }

        and: "all risk categories were initialized"
        def riskDef1Risk = domain.riskDefinitions.r1d1.riskValues
        riskDef1Risk*.category == ["D"]

        and: "the second risk definition was not initialized"
        domain.riskDefinitions.r2d2 == null

        when: "setting values for the second risk definition"
        domain.riskDefinitions.r2d2 = [
            impactValues: [
                [
                    category: "D",
                    specificImpact: 3,
                ],
            ],
            riskValues: [
                [
                    category: "D",
                    userDefinedResidualRisk: 2,
                ],
            ]
        ]
        put("/scopes/$scopeId/risks/$scenarioId", retrievedscopeRisk, ['If-Match': riskETag])

        and: "retrieving it again"
        def updatedRisk = parseJson(get("/scopes/$scopeId/risks/$scenarioId"))
        domain = updatedRisk.domains.get(domainId)

        then: "all changes are present"
        def updatedRiskDef1ImpactD = domain.riskDefinitions.r1d1.impactValues.find { it.category == "D" }
        updatedRiskDef1ImpactD.specificImpact == 1
        updatedRiskDef1ImpactD.effectiveImpact == 1

        def updatedRiskDef2ImpactD = domain.riskDefinitions.r2d2.impactValues.find { it.category == "D" }
        updatedRiskDef2ImpactD.specificImpact == 3
        updatedRiskDef2ImpactD.effectiveImpact == 3

        domain.riskDefinitions.r2d2.riskValues.find{it.category=="D"}.userDefinedResidualRisk == 2
    }

    def "non-existing risk definition causes error"() {
        given:
        def scopeId = scope.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        when: "creating a risk with risk values for a non-existing risk definition"
        post("/scopes/$scopeId/risks", [
            domains: [
                (domainId): [
                    reference: [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
                        absentRd : [
                            impactValues: [
                                [
                                    category: "A",
                                    specificImpact: 1
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ], 422)

        then:
        thrown(ReferenceTargetNotFoundException)
    }

    def "embedded risks can be requested"() {
        given: "a process with risks and risk values"
        def scopeId = scope.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        def scenario2Id = scenarioRepository.save(newScenario(unit) {
            associateWithDomain(domain, "NormalScenario", "NEW")
        }).idAsString

        postRisk1(scopeId, scenarioId)
        postRisk2(scopeId, scenario2Id)

        when: "the process is requested with embedded risks"
        def response = parseJson(
                get("/scopes/${scope.idAsString}?embedRisks=true"))

        then: "the risk values are embedded in the response"
        response.name == "scope null"
        response.risks != null
        response.risks.size() == 2
        response.risks*.domains*.get(domainId).reference.targetUri =~ [
            "http://localhost/domains/$domainId"
        ]

        and: "First risk, first risk definition: all values are correct"
        with(response.risks.find { it.designator == "RSK-1" }.domains.get(domainId).riskDefinitions.r1d1) {
            // probability is not set:
            probability.size() == 0

            // impact is present in first risk definition:
            impactValues.size() == 1
            with(impactValues.first()) {
                category == "D"
                effectiveImpact == 1
                specificImpact == 1
            }
        }

        and: "First risk, second risk definition: all values are correct"
        with (response.risks.find { it.designator == "RSK-1" }.domains.get(domainId).riskDefinitions.r2d2) {
            // impact is present in second risk definition:
            with(impactValues.find { it.category == "D" }) {
                specificImpact == 2
                effectiveImpact == 2
            }

            // risk values are present in second risk definition:
            riskValues.find { it.category == "D" }.userDefinedResidualRisk == 0
        }

        and: "Second risk, first risk definition: all values are correct"
        with (response.risks.find { it.designator == "RSK-2" }.domains.get(domainId).riskDefinitions.r1d1) {
            // probability is set:
            probability.specificProbability == 2
            probability.effectiveProbability == 2

            // impact is present in first risk definition:
            impactValues.size() == 1
            with(impactValues.first()) {
                category == "D"
                effectiveImpact == 3
                specificImpact == 3
            }

            // risk values are calculated in first risk definition:
            riskValues.size() == 1
            with(riskValues.first()) {
                category == "D"
                size() == 4
                inherentRisk == 3
                residualRisk == 3
            }
        }

        and: "Second risk, second risk definition: all values are correct"
        with (response.risks.find { it.designator == "RSK-2" }.domains.get(domainId).riskDefinitions.r2d2) {
            // impact is present in second risk definition:
            impactValues.size() == 1
            with(impactValues.first()) {
                category == "D"
                specificImpact == 3
                effectiveImpact == 3
            }

            // all manually set risk values are present in second risk definition:
            riskValues.size() == 1
            with(riskValues.first()) {
                category == "D"
                size() == 4
                userDefinedResidualRisk == 3
                residualRisk == 3
            }
        }
    }

    private postRisk2(String scopeId, String scenario2Id) {
        post("/scopes/$scopeId/risks", [
            domains : [
                (domainId): [
                    reference      : [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
                        r1d1: [
                            probability : [
                                specificProbability: 2
                            ],
                            impactValues: [
                                [
                                    category      : "D",
                                    specificImpact: 3
                                ]
                            ]
                        ],
                        r2d2: [
                            impactValues: [
                                [
                                    category      : "D",
                                    specificImpact: 3,
                                ],
                            ],
                            riskValues  : [
                                [
                                    category    : "D",
                                    userDefinedResidualRisk: 3,
                                ],
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenario2Id"]
        ])
    }

    private postRisk1(String scopeId, String scenarioId) {
        post("/scopes/$scopeId/risks", [
            domains : [
                (domainId): [
                    reference      : [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
                        r1d1: [
                            impactValues: [
                                [
                                    category      : "D",
                                    specificImpact: 1
                                ]
                            ]
                        ],
                        r2d2: [
                            impactValues: [
                                [
                                    category      : "D",
                                    specificImpact: 2,
                                ],
                            ],
                            riskValues  : [
                                [
                                    category    : "D",
                                    userDefinedResidualRisk: 0,
                                ],
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ])
    }

    def "Request a list of processes with embedded risks"() {
        given: "a list of processes with risks"
        def scopeId = scope.getIdAsString()
        def scenarioId = scenario.getIdAsString()
        def scenario2 = newScenario(unit) {
            associateWithDomain(domain, "NormalScenario", "NEW")
        }
        scenarioRepository.save(scenario2)
        def scenario2Id = scenario2.getIdAsString()
        postRisk1(scopeId, scenarioId)
        postRisk2(scopeId, scenario2Id)

        def scope2 = newScope(unit) {
            associateWithDomain(domain, "Difficultscope", "NEW")
        }
        scopeRepository.save(scope2)
        postRisk1(scope2.idAsString, scenarioId)
        postRisk2(scope2.idAsString, scenario2Id)

        when: "all scopes are requested"
        def result = parseJson(get("/scopes"))

        then: "the risks are not embedded"
        result.items != null
        result.items.size() == 2
        result.items.each {assert it.risks == null}

        when: "all scopes are requested with risks"
        result = parseJson(get("/scopes?embedRisks=true"))

        then: "the risks are embedded"
        result.items != null
        result.items.size() == 2
        result.items.each { assert it.risks != null }

        result.items*.risks*.domains.(domainId).riskDefinitions.r1d1.riskValues.size() == 2
        result.items*.risks*.domains.(domainId).riskDefinitions.r2d2.riskValues.size() == 2

        def scope1Risks = result.items.find { it.id == scope.idAsString }.risks
        scope1Risks.find { it.designator == "RSK-2" }.domains.get(domainId).riskDefinitions.r1d1.probability.specificProbability != null
        scope1Risks.find { it.designator == "RSK-1" }.domains.get(domainId).riskDefinitions.r1d1.impactValues.find { it.category == "D" }.effectiveImpact != null

        def scope2Risks = result.items.find { it.id == scope2.idAsString }.risks
        with(scope2Risks.find { it.designator == "RSK-4" }.domains.get(domainId).riskDefinitions.r1d1.riskValues.find { it.category == "D" }) {
            inherentRisk != null
            residualRisk != null
        }
    }

    def "Creating the same risk twice does not fail"() {
        when: "a POST request is issued to the risk ressource"
        def scopeId = scope.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        def beforeCreation = Instant.now()
        postscopeRisk(scopeId, scenarioId)
        def afterCreation = Instant.now()

        then: "a risk resource was created"
        def results = get("/scopes/$scopeId/risks/$scenarioId")
        def retrievedscopeRisk1 = parseJson(results)
        String eTag1 = getETag(results)

        Instant.parse(retrievedscopeRisk1.createdAt) > beforeCreation
        Instant.parse(retrievedscopeRisk1.createdAt) < afterCreation

        when: "a safe retry is made"
        postscopeRisk(scopeId, scenarioId, 204)

        and: "the resource is requested"
        results = get("/scopes/$scopeId/risks/$scenarioId")
        def retrievedscopeRisk2 = parseJson(results)
        String eTag2 = getETag(results)

        then: "the existing risk resource is unchanged: the POST request was idempotent"
        eTag2 != null
        eTag1 == eTag2
        retrievedscopeRisk2.designator == retrievedscopeRisk1.designator
        retrievedscopeRisk2.createdAt == retrievedscopeRisk1.createdAt
    }

    def "Creating an scope risk with only specific probability and impact values calculates risk value"() {
        given: "a process and a scenario"
        def scopeId = scope.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        when: "a risk is created with probability and impact"
        post("/scopes/$scopeId/risks", [
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
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ], 201)

        and: "the resource is requested"
        def results = get("/scopes/$scopeId/risks/$scenarioId")
        def retrievedscopeRisk2 = parseJson(results)

        then: "the risk resource was created with the values"
        def domain = retrievedscopeRisk2.domains.get(domainId)
        domain.riskDefinitions.size() == 1
        domain.riskDefinitions.r1d1.impactValues.find{it.category=='D'}.specificImpact == 1
        domain.riskDefinitions.r1d1.impactValues.find{it.category=='D'}.effectiveImpact == 1
        domain.riskDefinitions.r1d1.probability.specificProbability == 1
        domain.riskDefinitions.r1d1.probability.effectiveProbability == 1

        and: "the risk was calculated"
        domain.riskDefinitions.r1d1.riskValues.size() == 1
        with(
                domain.riskDefinitions.r1d1.riskValues.find{it.category=='D'}) {
                    inherentRisk == 0
                    residualRisk == 0
                }
    }

    def "Creating a risk with only specific probability and impact values calculates risk value"() {
        given: "a process and a scenario"
        def scopeId = scope.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        when: "a risk is created with probability and impact"
        post("/scopes/$scopeId/risks", [
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
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ], 201)

        and: "the resource is requested"
        def results = get("/scopes/$scopeId/risks/$scenarioId")
        def retrievedscopeRisk2 = parseJson(results)

        then: "the risk resource was created with the values"
        def domain = retrievedscopeRisk2.domains.get(domainId)
        domain.riskDefinitions.size() == 1
        domain.riskDefinitions.r1d1.impactValues.find{it.category=='D'}.specificImpact == 1
        domain.riskDefinitions.r1d1.impactValues.find{it.category=='D'}.effectiveImpact == 1
        domain.riskDefinitions.r1d1.probability.specificProbability == 1
        domain.riskDefinitions.r1d1.probability.effectiveProbability == 1

        and: "the risk was calculated"
        domain.riskDefinitions.r1d1.riskValues.size() == 1
        with(
                domain.riskDefinitions.r1d1.riskValues.find{it.category=='D'}) {
                    inherentRisk == 0
                    residualRisk == 0
                }
    }

    def "Creating a risk with potential values calculates risk value"() {
        given: "a scope & scenario"
        def scopeId = scope.idAsString

        def scenarioId = parseJson(post("/domains/$domainId/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "BestCase",
            status: "NEW",
            riskValues: [
                r1d1 : [
                    potentialProbability: 2
                ]
            ]
        ])).resourceId

        when: "a risk is created with specific probability and impact"
        post("/scopes/$scopeId/risks", [
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
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ], 201)

        then: "scope contains impact"
        def retrievedscope = parseJson(get("/scopes/$scopeId"))
        retrievedscope.domains.get(domainId).riskValues.r1d1.potentialImpacts.D == 2

        and: "scenario contains probability"
        def retrievedScenario = parseJson(get("/scenarios/$scenarioId"))
        retrievedScenario.domains.get(domainId).riskValues.r1d1.potentialProbability == 2

        and: "the risk resource was created with the values"
        def retrievedscopeRisk2 = parseJson(get("/scopes/$scopeId/risks/$scenarioId"))

        with(retrievedscopeRisk2.domains.get(domainId).riskDefinitions) {
            size() == 1

            r1d1.impactValues.find{it.category=='D'}.potentialImpact == 2
            r1d1.impactValues.find{it.category=='D'}.specificImpact == 1
            r1d1.impactValues.find{it.category=='D'}.effectiveImpact == 1

            r1d1.probability.potentialProbability == 2
            r1d1.probability.specificProbability == 1
            r1d1.probability.effectiveProbability == 1
        }

        and: "the risk was calculated"
        retrievedscopeRisk2.domains.get(domainId).riskDefinitions.r1d1.riskValues.size() == 1
        with(
                retrievedscopeRisk2.domains.get(domainId).riskDefinitions.r1d1.riskValues.find{it.category=='D'}) {
                    inherentRisk == 0
                    residualRisk == 0
                }
    }

    def "Creating a risk with potential values calculates risk value (with only one risk definition in the domain)"() {
        given: "a scope in a domain with only a single risk definition"
        domain.riskDefinitions.remove("r2d2")
        def scopeId = scope.idAsString
        def scenarioId = parseJson(post("/domains/$domainId/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "BestCase",
            status: "NEW",
            riskValues: [
                r1d1 : [
                    potentialProbability: 2
                ]
            ]
        ])).resourceId

        when: "a risk is created with specific probability and impact"
        post("/scopes/$scopeId/risks", [
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
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ], 201)

        then: "process contains impact"
        def retrievedscope = parseJson(get("/scopes/$scopeId"))
        retrievedscope.domains.get(domainId).riskValues.r1d1.potentialImpacts.D == 2

        and: "scenario contains probability"
        def retrievedScenario = parseJson(get("/scenarios/$scenarioId"))
        retrievedScenario.domains.get(domainId).riskValues.r1d1.potentialProbability == 2

        and: "the risk resource was created with the values"
        def retrievedscopeRisk2 = parseJson(get("/scopes/$scopeId/risks/$scenarioId"))
        retrievedscopeRisk2.domains.get(domainId).riskDefinitions.size() == 1

        retrievedscopeRisk2.domains.get(domainId).riskDefinitions.r1d1.impactValues.find{it.category=='D'}.potentialImpact == 2
        retrievedscopeRisk2.domains.get(domainId).riskDefinitions.r1d1.impactValues.find{it.category=='D'}.specificImpact == 1
        retrievedscopeRisk2.domains.get(domainId).riskDefinitions.r1d1.impactValues.find{it.category=='D'}.effectiveImpact == 1

        retrievedscopeRisk2.domains.get(domainId).riskDefinitions.r1d1.probability.potentialProbability == 2
        retrievedscopeRisk2.domains.get(domainId).riskDefinitions.r1d1.probability.specificProbability == 1
        retrievedscopeRisk2.domains.get(domainId).riskDefinitions.r1d1.probability.effectiveProbability == 1

        and: "the risk was calculated"
        retrievedscopeRisk2.domains.get(domainId).riskDefinitions.r1d1.riskValues.size() == 1
        with(retrievedscopeRisk2.domains.get(domainId).riskDefinitions.r1d1.riskValues.find{it.category=='D'}) {
            inherentRisk == 0
            residualRisk == 0
        }
    }

    def "Trying to create an existing risk updates its values"() {
        given: "a process and a scenario"
        def scopeId = scope.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        when: "a POST request is issued to the risk resource"
        def beforeCreation = Instant.now()
        postscopeRisk(scopeId, scenarioId)
        def afterCreation = Instant.now()

        then: "a risk resource was created"
        def results = get("/scopes/$scopeId/risks/$scenarioId")
        def retrievedscopeRisk1 = parseJson(results)
        String eTag1 = getETag(results)

        Instant.parse(retrievedscopeRisk1.createdAt) > beforeCreation
        Instant.parse(retrievedscopeRisk1.createdAt) < afterCreation
        retrievedscopeRisk1.domains.get(domainId).riskDefinitions.size() == 0

        when: "a safe retry is made with new values"
        post("/scopes/$scopeId/risks", [
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
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ], 204)

        and: "the resource is requested"
        results = get("/scopes/$scopeId/risks/$scenarioId")
        def retrievedscopeRisk2 = parseJson(results)
        String eTag2 = getETag(results)

        then: "the existing risk resource was updated with new values"
        with(retrievedscopeRisk2.domains.get(domainId)) {
            riskDefinitions.size() == 1
            riskDefinitions.r1d1.impactValues.find{it.category=='D'}.specificImpact == 1
            riskDefinitions.r1d1.impactValues.find{it.category=='D'}.effectiveImpact == 1
            riskDefinitions.r1d1.probability.specificProbability == 1
            riskDefinitions.r1d1.probability.effectiveProbability == 1
            riskDefinitions.r1d1.riskValues.size() == 1
        }

        and: "it is still the same risk object"
        retrievedscopeRisk2.designator == retrievedscopeRisk1.designator
        retrievedscopeRisk2.createdAt == retrievedscopeRisk1.createdAt
        eTag2 != null
        eTag1 != eTag2
    }

    def "Invalid domain reference in risk leads to a sensible error code"() {
        given:
        def scopeId = scope.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        when:
        post("/scopes/$scopeId/risks", [
            domains: [
                (UUID.randomUUID().toString()): [
                    reference: [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
                        absentRd : [
                            impactValues: [
                                [
                                    category: "A",
                                    specificImpact: 1
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ], HttpStatus.SC_UNPROCESSABLE_ENTITY)

        then:
        thrown(UnprocessableDataException)
    }

    def "nonexistent resource in request body leads to a sensible error code"() {
        given:
        def scopeId = scope.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        when:
        post("/scopes/$scopeId/risks", [
            domains: [
                (domainId): [
                    reference: [targetUri: "http://localhost/domains/"+UUID.randomUUID().toString()],
                    riskDefinitions: [
                        absentRd : [
                            impactValues: [
                                [
                                    category: "A",
                                    specificImpact: 1
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ], HttpStatus.SC_UNPROCESSABLE_ENTITY)

        then:
        thrown(UnprocessableDataException)
    }

    private postscopeRisk(String scopeId, String scenarioId, int expectedStatusCode = 201) {
        post("/scopes/$scopeId/risks", [
            domains : [
                (domainId): [
                    reference      : [targetUri: "http://localhost/domains/$domainId"]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ], expectedStatusCode)
    }
}
