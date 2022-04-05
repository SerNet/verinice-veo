/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ProcessData
import org.veo.persistence.entity.jpa.ScenarioData

import spock.lang.Ignore
import spock.lang.Issue

/**
 * Test risk related functionality on controls.
 */
@WithUserDetails("user@domain.example")
class ProcessRiskValuesMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private TransactionTemplate txTemplate

    @Autowired
    private ProcessRepositoryImpl processRepository

    @Autowired
    private ScenarioRepositoryImpl scenarioRepository

    private Client client
    private String unitId
    private String domainId
    private String r1d1DomainId
    private ProcessData process
    private ScenarioData scenario

    def setup() {
        txTemplate.execute {
            client = createTestClient()
            def domain = newDomain(client) {
                riskDefinitions = [
                    "r1d1": createRiskDefinition("r1d1"),
                    "r2d2": createRiskDefinition("r2d2")
                ]
            }
            domainId = domain.idAsString

            r1d1DomainId = (newDomain(client) {
                riskDefinitions = [
                    "r1d1": createRiskDefinition("r1d1"),
                ]
            }).idAsString

            def unit = newUnit(client)
            unitId = unitRepository.save(unit).idAsString
            clientRepository.save(client)

            process = newProcess(unit) {
                addToDomains(domain)
            }
            processRepository.save(process)

            scenario = newScenario(unit) {
                addToDomains(domain)
            }
            scenarioRepository.save(scenario)
        }
    }

    def "values can be set on a second risk definition"() {
        given: "that the process can use both r1d1 & r2d2"
        addProcessToScope("r1d1")
        addProcessToScope("r2d2")

        when: "creating a process with risk values for a single risk definition"
        def processId = process.getIdAsString()
        def scenarioId = scenario.getIdAsString()
        post("/processes/$processId/risks", [
            domains: [
                (domainId): [
                    reference: [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
                        r1d1 : [
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
        ])

        and: "retrieving it"
        def getProcessRiskResponse = get("/processes/$processId/risks/$scenarioId")
        def riskETag = getETag(getProcessRiskResponse)
        def retrievedProcessRisk = parseJson(getProcessRiskResponse)

        then: "all specified values were saved"
        def riskDef1Impact = retrievedProcessRisk.domains.(domainId).riskDefinitions.r1d1.impactValues
        riskDef1Impact.find {it.category=="A"}.specificImpact == 1
        riskDef1Impact.find {it.category=="A"}.effectiveImpact == 1

        and: "all impact categories were initialized"
        riskDef1Impact.find {it.category=="I"} != null
        riskDef1Impact.find {it.category=="C"} != null
        riskDef1Impact.find {it.category=="R"} != null

        and: "all risk categories were initialized"
        def riskDef1Risk = retrievedProcessRisk.domains.(domainId).riskDefinitions.r1d1.riskValues
        riskDef1Risk.find {it.category=="I"} != null
        riskDef1Risk.find {it.category=="A"} != null
        riskDef1Risk.find {it.category=="C"} != null
        riskDef1Risk.find {it.category=="R"} != null

        and: "the second risk definition was not initialized"
        retrievedProcessRisk.domains.(domainId).riskDefinitions.r2d2 == null

        when: "setting values for the second risk definition"
        retrievedProcessRisk.domains.(domainId).riskDefinitions.r2d2 = [
            impactValues: [
                [
                    category: "A",
                    specificImpact: 3,
                ],
            ],
            riskValues: [
                [
                    category: "A",
                    residualRisk: 2,
                ],
            ]
        ]
        put("/processes/$processId/risks/$scenarioId", retrievedProcessRisk, ['If-Match': riskETag])

        and: "retrieving it again"
        def updatedRisk = parseJson(get("/processes/$processId/risks/$scenarioId"))

        then: "all changes are present"
        def updatedRiskDef1ImpactA = updatedRisk.domains.(domainId).riskDefinitions.r1d1.impactValues.find { it.category == "A" }
        updatedRiskDef1ImpactA.specificImpact == 1
        updatedRiskDef1ImpactA.effectiveImpact == 1

        def updatedRiskDef2ImpactA = updatedRisk.domains.(domainId).riskDefinitions.r2d2.impactValues.find { it.category == "A" }
        updatedRiskDef2ImpactA.specificImpact == 3
        updatedRiskDef2ImpactA.effectiveImpact == 3

        updatedRisk.domains.(domainId).riskDefinitions.r2d2.riskValues.find{it.category=="A"}.residualRisk == 2
    }

    def "cannot create risk with risk values for illegal risk definition"() {
        given: "that the process can only use one risk definition"
        addProcessToScope("r1d1")

        when: "trying to set risk values for forbidden risk definition"
        def processId = process.getIdAsString()
        def scenarioId = scenario.getIdAsString()
        post("/processes/$processId/risks/", [
            domains: [
                (domainId): [
                    reference: [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
                        r2d2 : [
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
        ], 400)

        then: "it fails"
        IllegalArgumentException ex = thrown()
        ex.message == "Cannot define risk values for risk definition 'r2d2' because the process $processId is not within a scope that uses that risk definition"
    }

    def "cannot update risk with risk values for illegal risk definition"() {
        given: "that the process can only use one risk definition"
        addProcessToScope("r1d1")

        when: "initializing the risk with values for legal risk definition"
        def processId = process.getIdAsString()
        def scenarioId = scenario.getIdAsString()
        post("/processes/$processId/risks/", [
            domains: [
                (domainId): [
                    reference: [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
                        r1d1 : [
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
        ])
        def eTag = getETag(get("/processes/$processId/risks/$scenarioId"))

        then: "it is accepted"
        notThrown(Exception)

        when: "trying to update it with illegal values"
        put("/processes/$processId/risks/$scenarioId", [
            domains: [
                (domainId): [
                    reference: [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
                        r1d1 : [
                            impactValues: [
                                [
                                    category: "A",
                                    specificImpact: 1
                                ]
                            ]
                        ],
                        r2d2 : [
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
        ], ['If-Match': eTag], 400)

        then: "it fails"
        IllegalArgumentException ex = thrown()
        ex.message == "Cannot define risk values for risk definition 'r2d2' because the process $processId is not within a scope that uses that risk definition"
    }

    private def addProcessToScope(String riskDefinitionId) {
        post("/scopes", [
            domains: [
                (domainId): [
                    riskDefinition: riskDefinitionId
                ]
            ],
            name: "$riskDefinitionId scope",
            members: [
                [targetUri: "http://localhost/processes/$process.idAsString"]
            ],
            owner: [targetUri: "http://localhost/units/$process.owner.idAsString"]
        ])
    }

    def "Creating the same risk twice does not fail"() {
        when: "a POST request is issued to the risk ressource"
        def processId = process.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        def beforeCreation = Instant.now()
        postProcessRisk(processId, scenarioId)
        def afterCreation = Instant.now()

        then: "a risk resource was created"
        def results = get("/processes/$processId/risks/$scenarioId")
        def retrievedProcessRisk1 = parseJson(results)
        String eTag1 = getETag(results)

        Instant.parse(retrievedProcessRisk1.createdAt) > beforeCreation
        Instant.parse(retrievedProcessRisk1.createdAt) < afterCreation

        when: "a safe retry is made"
        postProcessRisk(processId, scenarioId, 204)

        and: "the resource is requested"
        results = get("/processes/$processId/risks/$scenarioId")
        def retrievedProcessRisk2 = parseJson(results)
        String eTag2 = getETag(results)

        then: "the existing risk resource is unchanged: the POST request was idempotent"
        eTag2 != null
        eTag1 == eTag2
        retrievedProcessRisk2.designator == retrievedProcessRisk1.designator
        retrievedProcessRisk2.createdAt == retrievedProcessRisk1.createdAt
    }

    def "Creating a risk with only specific probability and impact values calculates risk value"() {
        given: "a process in a scope with a risk definition"
        addProcessToScope("r1d1")
        def processId = process.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        when: "a risk is created with probability and impact"
        post("/processes/$processId/risks", [
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

        and: "the resource is requested"
        def results = get("/processes/$processId/risks/$scenarioId")
        def retrievedProcessRisk2 = parseJson(results)

        then: "the risk resource was created with the values"
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.size() == 1
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.impactValues.find{it.category=='A'}.specificImpact == 1
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.impactValues.find{it.category=='A'}.effectiveImpact == 1
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.probability.specificProbability == 1
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.probability.effectiveProbability == 1

        and: "the risk was calculated"
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.riskValues.size == 4
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.riskValues.find{it.category=='A'}.inherentRisk == 0
    }

    def "Creating a risk with potential values calculates risk value"() {
        given: "a process in a scope with one risk definition (out of two in the domain)"
        def processId = parseJson(post("/processes", [
            domains: [
                (domainId): [ : ]
            ],
            name: "risk test process",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
        def processETag = getETag(get("/processes/$processId"))

        post("/scopes", [
            name: "r1d1 scope",
            domains: [
                (domainId): [
                    riskDefinition: "r1d1"
                ]
            ],
            members: [
                [targetUri: "http://localhost/processes/$processId"]
            ],
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])

        Map headers = [
            'If-Match': processETag
        ]
        put("/processes/$processId", [
            domains: [
                (domainId): [
                    riskValues: [
                        r1d1 : [
                            potentialImpacts: [
                                "A": "2",
                            ]
                        ]
                    ]
                ]
            ],
            name: "risk test process",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ], headers)

        def scenarioId = parseJson(post("/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    riskValues: [
                        r1d1 : [
                            potentialProbability: 2
                        ]
                    ]
                ]
            ]
        ])).resourceId

        when: "a risk is created with specific probability and impact"
        post("/processes/$processId/risks", [
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

        then: "process contains impact"
        def retrievedProcess = parseJson(get("/processes/$processId"))
        retrievedProcess.domains.(domainId).riskValues.r1d1.potentialImpacts.A == "2"

        and: "scenario contains probability"
        def retrievedScenario = parseJson(get("/scenarios/$scenarioId"))
        retrievedScenario.domains.(domainId).riskValues.r1d1.potentialProbability == 2

        and: "the risk resource was created with the values"
        def retrievedProcessRisk2 = parseJson(get("/processes/$processId/risks/$scenarioId"))
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.size() == 1

        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.impactValues.find{it.category=='A'}.potentialImpact == 2
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.impactValues.find{it.category=='A'}.specificImpact == 1
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.impactValues.find{it.category=='A'}.effectiveImpact == 1

        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.probability.potentialProbability == 2
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.probability.specificProbability == 1
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.probability.effectiveProbability == 1

        and: "the risk was calculated"
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.riskValues.size == 4
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.riskValues.find{it.category=='A'}.inherentRisk == 0
    }

    def "Creating a risk with potential values calculates risk value (with only one risk definition in the domain)"() {
        given: "a process in a domain with only a single risk definition"
        def processId = parseJson(post("/processes", [
            domains: [
                (r1d1DomainId): [ : ]
            ],
            name: "risk test process",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
        def processETag = getETag(get("/processes/$processId"))

        post("/scopes", [
            name: "r1d1 scope",
            domains: [
                (r1d1DomainId): [
                    riskDefinition: "r1d1"
                ]
            ],
            members: [
                [targetUri: "http://localhost/processes/$processId"]
            ],
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])

        Map headers = [
            'If-Match': processETag
        ]
        put("/processes/$processId", [
            domains: [
                (r1d1DomainId): [
                    riskValues: [
                        r1d1 : [
                            potentialImpacts: [
                                "A": "2",
                            ]
                        ]
                    ]
                ]
            ],
            name: "risk test process",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ], headers)

        def scenarioId = parseJson(post("/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (r1d1DomainId): [
                    riskValues: [
                        r1d1 : [
                            potentialProbability: 2
                        ]
                    ]
                ]
            ]
        ])).resourceId

        when: "a risk is created with specific probability and impact"
        post("/processes/$processId/risks", [
            domains : [
                (r1d1DomainId): [
                    reference      : [targetUri: "http://localhost/domains/$r1d1DomainId"],
                    riskDefinitions: [
                        r1d1: [
                            probability: [
                                specificProbability: 1
                            ],
                            impactValues: [
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

        then: "process contains impact"
        def retrievedProcess = parseJson(get("/processes/$processId"))
        retrievedProcess.domains.(r1d1DomainId).riskValues.r1d1.potentialImpacts.A == "2"

        and: "scenario contains probability"
        def retrievedScenario = parseJson(get("/scenarios/$scenarioId"))
        retrievedScenario.domains.(r1d1DomainId).riskValues.r1d1.potentialProbability == 2

        and: "the risk resource was created with the values"
        def retrievedProcessRisk2 = parseJson(get("/processes/$processId/risks/$scenarioId"))
        retrievedProcessRisk2.domains.(r1d1DomainId).riskDefinitions.size() == 1

        retrievedProcessRisk2.domains.(r1d1DomainId).riskDefinitions.r1d1.impactValues.find{it.category=='A'}.potentialImpact == 2
        retrievedProcessRisk2.domains.(r1d1DomainId).riskDefinitions.r1d1.impactValues.find{it.category=='A'}.specificImpact == 1
        retrievedProcessRisk2.domains.(r1d1DomainId).riskDefinitions.r1d1.impactValues.find{it.category=='A'}.effectiveImpact == 1

        retrievedProcessRisk2.domains.(r1d1DomainId).riskDefinitions.r1d1.probability.potentialProbability == 2
        retrievedProcessRisk2.domains.(r1d1DomainId).riskDefinitions.r1d1.probability.specificProbability == 1
        retrievedProcessRisk2.domains.(r1d1DomainId).riskDefinitions.r1d1.probability.effectiveProbability == 1

        and: "the risk was calculated"
        retrievedProcessRisk2.domains.(r1d1DomainId).riskDefinitions.r1d1.riskValues.size == 4
        retrievedProcessRisk2.domains.(r1d1DomainId).riskDefinitions.r1d1.riskValues.find{it.category=='A'}.inherentRisk == 0
    }



    def "Trying to create an existing risk updates its values"() {
        given: "a process in a scope with a risk definition"
        addProcessToScope("r1d1")
        def processId = process.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        when: "a POST request is issued to the risk resource"
        def beforeCreation = Instant.now()
        postProcessRisk(processId, scenarioId)
        def afterCreation = Instant.now()

        then: "a risk resource was created"
        def results = get("/processes/$processId/risks/$scenarioId")
        def retrievedProcessRisk1 = parseJson(results)
        String eTag1 = getETag(results)

        Instant.parse(retrievedProcessRisk1.createdAt) > beforeCreation
        Instant.parse(retrievedProcessRisk1.createdAt) < afterCreation
        retrievedProcessRisk1.domains.(domainId).riskDefinitions.size() == 0

        when: "a safe retry is made with new values"
        post("/processes/$processId/risks", [
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
                                    category      : "A",
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
        results = get("/processes/$processId/risks/$scenarioId")
        def retrievedProcessRisk2 = parseJson(results)
        String eTag2 = getETag(results)

        then: "the existing risk resource was updated with new values"
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.size() == 1
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.impactValues.find{it.category=='A'}.specificImpact == 1
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.impactValues.find{it.category=='A'}.effectiveImpact == 1
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.probability.specificProbability == 1
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.probability.effectiveProbability == 1
        retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.riskValues.size == 4

        and: "it is still the same risk object"
        retrievedProcessRisk2.designator == retrievedProcessRisk1.designator
        retrievedProcessRisk2.createdAt == retrievedProcessRisk1.createdAt
        eTag2 != null
        eTag1 != eTag2
    }

    private postProcessRisk(String processId, String scenarioId, int expectedStatusCode = 201) {
        post("/processes/$processId/risks", [
            domains : [
                (domainId): [
                    reference      : [targetUri: "http://localhost/domains/$domainId"]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ], expectedStatusCode)
    }
}
