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

import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ProcessData
import org.veo.persistence.entity.jpa.ScenarioData

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
    private ProcessRepositoryImpl processRepository

    @Autowired
    private ScenarioRepositoryImpl scenarioRepository

    private Client client
    private Unit unit
    private Domain domain
    private String unitId
    private String domainId
    private String r1d1DomainId
    private ProcessData process
    private ScenarioData scenario

    def setup() {
        client = createTestClient()
        domain = newDomain(client) {
            riskDefinitions = [
                "r1d1": createRiskDefinition("r1d1"),
                "r2d2": createRiskDefinition("r2d2")
            ]
            elementTypeDefinitions = [
                newElementTypeDefinition("process", it) {
                    subTypes = [
                        DifficultProcess: newSubTypeDefinition()
                    ]
                },
                newElementTypeDefinition("scenario", it) {
                    subTypes = [
                        BestCase: newSubTypeDefinition()
                    ]
                },
            ]
        }
        domainId = domain.idAsString

        r1d1DomainId = (newDomain(client) {
            riskDefinitions = [
                "r1d1": createRiskDefinition("r1d1"),
            ]
            elementTypeDefinitions = [
                newElementTypeDefinition("process", it) {
                    subTypes = [
                        RiskyProcess: newSubTypeDefinition()
                    ]
                },
                newElementTypeDefinition("scenario", it) {
                    subTypes = [
                        HypotheticalScenario: newSubTypeDefinition()
                    ]
                },
            ]
        }).idAsString

        unit = newUnit(client)
        unitId = unitRepository.save(unit).idAsString
        clientRepository.save(client)

        process = newProcess(unit) {
            associateWithDomain(domain, "NormalProcess", "NEW")
        }
        processRepository.save(process)

        scenario = newScenario(unit) {
            associateWithDomain(domain, "NormalScenario", "NEW")
        }
        scenarioRepository.save(scenario)
    }

    def "values can be set on a second risk definition"() {
        when: "creating a process with risk values for two risk definitions"
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
                    userDefinedResidualRisk: 2,
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

        updatedRisk.domains.(domainId).riskDefinitions.r2d2.riskValues.find{it.category=="A"}.userDefinedResidualRisk == 2
    }

    def "non-existing risk definition causes error"() {
        given:
        def processId = process.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        when: "creating a risk with risk values for a non-existing risk definition"
        post("/processes/$processId/risks", [
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
        ], 404)

        then:
        thrown(NotFoundException)
    }

    def "embedded risks can be requested"() {
        given: "a process with risks and risk values"
        def processId = process.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        def scenario2Id = scenarioRepository.save(newScenario(unit) {
            associateWithDomain(domain, "NormalScenario", "NEW")
        }).idAsString

        postRisk1(processId, scenarioId)
        postRisk2(processId, scenario2Id)

        when: "the process is requested with embedded risks"
        def response = parseJson(
                get("/processes/${process.id.uuidValue()}?embedRisks=true"))

        then: "the risk values are embedded in the response"
        response.name == "process null"
        response.risks != null
        response.risks.size() == 2
        response.risks*.domains*.(domainId).reference.targetUri =~ [
            "http://localhost/domains/$domainId"
        ]

        and: "First risk, first risk definition: all values are correct"
        with(response.risks.find { it.designator == "RSK-1" }.domains.(domainId).riskDefinitions.r1d1) {
            // probability is not set:
            probability.size() == 0

            // impact is present in first risk definition:
            with(impactValues.find { it.category == "A" }) {
                effectiveImpact == 1
                specificImpact == 1
            }

            // empty impact values contain only category:
            impactValues.find { it.category == "C" }.size() == 1
            impactValues.find { it.category == "I" }.size() == 1
            impactValues.find { it.category == "R" }.size() == 1

            // empty risk values contain category and empty impactValues collection:
            riskValues.find { it.category == "R" }.size() == 2
            riskValues.find { it.category == "I" }.size() == 2
            riskValues.find { it.category == "C" }.size() == 2
            riskValues.find { it.category == "A" }.size() == 2
        }

        and: "First risk, second risk definition: all values are correct"
        with (response.risks.find { it.designator == "RSK-1" }.domains.(domainId).riskDefinitions.r2d2) {
            // impact is present in second risk definition:
            with(impactValues.find { it.category == "A" }) {
                specificImpact == 2
                effectiveImpact == 2
            }

            // risk values are present in second risk definition:
            riskValues.find { it.category == "A" }.userDefinedResidualRisk == 0
        }

        and: "Second risk, first risk definition: all values are correct"
        with (response.risks.find { it.designator == "RSK-2" }.domains.(domainId).riskDefinitions.r1d1) {
            // probability is set:
            probability.specificProbability == 2
            probability.effectiveProbability == 2

            // impact is present in first risk definition:
            with(impactValues.find { it.category == "A" }) {
                effectiveImpact == 3
                specificImpact == 3
            }

            // empty impact values contain only category:
            impactValues.find { it.category == "C" }.size() == 1
            impactValues.find { it.category == "I" }.size() == 1
            impactValues.find { it.category == "R" }.size() == 1

            // empty risk values contain category and empty impactValues collection:
            riskValues.find { it.category == "R" }.size() == 2
            riskValues.find { it.category == "I" }.size() == 2
            riskValues.find { it.category == "C" }.size() == 2

            // risk values are calculated in first risk definition:
            with(riskValues.find{it.category == "A"}) {
                size() == 4
                inherentRisk == 3
                residualRisk == 3
            }
        }

        and: "Second risk, second risk definition: all values are correct"
        with (response.risks.find { it.designator == "RSK-2" }.domains.(domainId).riskDefinitions.r2d2) {
            // impact is present in second risk definition:
            with(impactValues.find { it.category == "A" }) {
                specificImpact == 3
                effectiveImpact == 3
            }

            // all manually set risk values are present in second risk definition:
            with(riskValues.find { it.category == "A" }) {
                size() == 4
                userDefinedResidualRisk == 3
                residualRisk == 3
            }
        }
    }

    private postRisk2(String processId, String scenario2Id) {
        post("/processes/$processId/risks", [
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
                                    category      : "A",
                                    specificImpact: 3
                                ]
                            ]
                        ],
                        r2d2: [
                            impactValues: [
                                [
                                    category      : "A",
                                    specificImpact: 3,
                                ],
                            ],
                            riskValues  : [
                                [
                                    category    : "A",
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

    private postRisk1(String processId, String scenarioId) {
        post("/processes/$processId/risks", [
            domains : [
                (domainId): [
                    reference      : [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
                        r1d1: [
                            impactValues: [
                                [
                                    category      : "A",
                                    specificImpact: 1
                                ]
                            ]
                        ],
                        r2d2: [
                            impactValues: [
                                [
                                    category      : "A",
                                    specificImpact: 2,
                                ],
                            ],
                            riskValues  : [
                                [
                                    category    : "A",
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
        def processId = process.getIdAsString()
        def scenarioId = scenario.getIdAsString()
        def scenario2 = newScenario(unit) {
            associateWithDomain(domain, "NormalScenario", "NEW")
        }
        scenarioRepository.save(scenario2)
        def scenario2Id = scenario2.getIdAsString()
        postRisk1(processId, scenarioId)
        postRisk2(processId, scenario2Id)

        def process2 = newProcess(unit) {
            associateWithDomain(domain, "NormalProcess", "NEW")
        }
        processRepository.save(process2)
        postRisk1(process2.idAsString, scenarioId)
        postRisk2(process2.idAsString, scenario2Id)

        when: "all processes are requested"
        def result = parseJson(get("/processes"))

        then: "the risks are not embedded"
        result.items != null
        result.items.size() == 2
        result.items.each {assert it.risks == null}

        when: "all processes are requested with risks"
        result = parseJson(get("/processes?embedRisks=true"))

        then: "the risks are embedded"
        result.items != null
        result.items.size() == 2
        result.items.each { assert it.risks != null }

        result.items*.risks*.domains.(domainId).riskDefinitions.r1d1.riskValues.size() == 2
        result.items*.risks*.domains.(domainId).riskDefinitions.r2d2.riskValues.size() == 2

        def process1Risks = result.items.find { it.id == process.idAsString }.risks
        process1Risks.find { it.designator == "RSK-2" }.domains.(domainId).riskDefinitions.r1d1.probability.specificProbability != null
        process1Risks.find { it.designator == "RSK-1" }.domains.(domainId).riskDefinitions.r1d1.impactValues.find { it.category == "A" }.effectiveImpact != null

        def process2Risks = result.items.find { it.id == process2.idAsString }.risks
        with(process2Risks.find { it.designator == "RSK-4" }.domains.(domainId).riskDefinitions.r1d1.riskValues.find { it.category == "A" }) {
            inherentRisk != null
            residualRisk != null
        }
    }


    def "Searching for processes with embedded risks"() {
        given: "a list of processes with risks"
        def processId = process.getIdAsString()
        def scenarioId = scenario.getIdAsString()
        def scenario2 = newScenario(unit) {
            associateWithDomain(domain, "NormalScenario", "NEW")
        }
        scenarioRepository.save(scenario2)
        def scenario2Id = scenario2.getIdAsString()
        postRisk1(processId, scenarioId)
        postRisk2(processId, scenario2Id)

        def process2 = newProcess(unit) {
            associateWithDomain(domain, "NormalProcess", "NEW")
        }
        processRepository.save(process2)
        postRisk1(process2.idAsString, scenarioId)
        postRisk2(process2.idAsString, scenario2Id)

        when: "all processes are searched for"
        def searchUrl = parseJson(post("/processes/searches", [
            unitId: [
                values: [
                    unit.id.uuidValue()
                ]
            ]
        ])).searchUrl
        def result = parseJson(get(new URI(searchUrl)))

        then: "the risks are not embedded"
        result.items != null
        result.items.size() == 2
        result.items.each {assert it.risks == null}

        when: "all processes are searched for with risks"
        result = parseJson(get(new URI(searchUrl + "?embedRisks=true")))

        then: "the risks are embedded"
        result.items != null
        result.items.size() == 2
        result.items.each {assert it.risks != null}

        result.items*.risks*.domains.(domainId).riskDefinitions.r1d1.riskValues.size() == 2
        result.items*.risks*.domains.(domainId).riskDefinitions.r2d2.riskValues.size() == 2

        def process1Risks = result.items.find { it.id == process.idAsString }.risks
        process1Risks.find{it.designator=="RSK-2"}.domains.(domainId).riskDefinitions.r1d1.probability.specificProbability != null
        process1Risks.find{it.designator=="RSK-1"}.domains.(domainId).riskDefinitions.r1d1.impactValues.find{it.category=="A"}.effectiveImpact != null

        def process2Risks = result.items.find { it.id == process2.idAsString }.risks
        with(process2Risks.find{it.designator=="RSK-4"}.domains.(domainId).riskDefinitions.r1d1.riskValues.find{it.category=="A"}) {
            inherentRisk != null
            residualRisk != null
        }
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
        given: "a process and a scenario"
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
        with(
                retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.riskValues.find{it.category=='A'}) {
                    inherentRisk == 0
                    residualRisk == 0
                }
    }

    def "Creating a risk with potential values calculates risk value"() {
        given: "a process"
        def processId = parseJson(post("/processes", [
            domains: [
                (domainId): [
                    subType: "DifficultProcess",
                    status: "NEW",
                ]
            ],
            name: "risk test process",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
        def processETag = getETag(get("/processes/$processId"))

        Map headers = [
            'If-Match': processETag
        ]
        put("/processes/$processId", [
            domains: [
                (domainId): [
                    subType: "DifficultProcess",
                    status: "NEW",
                    riskValues: [
                        r1d1 : [
                            potentialImpacts: [
                                "A": 2,
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
                    subType: "BestCase",
                    status: "NEW",
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
        retrievedProcess.domains.(domainId).riskValues.r1d1.potentialImpacts.A == 2

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
        with(
                retrievedProcessRisk2.domains.(domainId).riskDefinitions.r1d1.riskValues.find{it.category=='A'}) {
                    inherentRisk == 0
                    residualRisk == 0
                }
    }

    def "Creating a risk with potential values calculates risk value (with only one risk definition in the domain)"() {
        given: "a process in a domain with only a single risk definition"
        def processId = parseJson(post("/processes", [
            domains: [
                (r1d1DomainId): [
                    subType: "RiskyProcess",
                    status: "NEW",
                    riskValues: [
                        r1d1 : [
                            potentialImpacts: [
                                "A": 2,
                            ]
                        ]
                    ]
                ]
            ],
            name: "risk test process",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId

        def scenarioId = parseJson(post("/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (r1d1DomainId): [
                    subType: "HypotheticalScenario",
                    status: "NEW",
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
        retrievedProcess.domains.(r1d1DomainId).riskValues.r1d1.potentialImpacts.A == 2

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
        with(retrievedProcessRisk2.domains.(r1d1DomainId).riskDefinitions.r1d1.riskValues.find{it.category=='A'}) {
            inherentRisk == 0
            residualRisk == 0
        }
    }



    def "Trying to create an existing risk updates its values"() {
        given: "a process and a scenario"
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

    def "Invalid domain reference in risk leads to a sensible error code"() {
        given:
        def processId = process.getIdAsString()
        def scenarioId = scenario.getIdAsString()

        when:
        post("/processes/$processId/risks", [
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
