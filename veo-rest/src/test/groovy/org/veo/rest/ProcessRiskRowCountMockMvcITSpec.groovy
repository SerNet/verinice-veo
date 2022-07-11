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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.CustomAspect
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.metrics.DataSourceProxyBeanPostProcessor

/**
 * Test row count when reading processes with embedded risks.
 */
@WithUserDetails("user@domain.example")
class ProcessRiskRowCountMockMvcITSpec extends VeoMvcSpec {

    public static final int NUM_PROCESSES = 10

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

    @DynamicPropertySource
    static void setRowCount(DynamicPropertyRegistry registry) {
        registry.add("veo.logging.datasource.row_count", { -> true })
    }

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
    }

    def "Searching for 10 processes with embedded risks"() {
        given: "a list of processes with risks"
        NUM_PROCESSES.times {
            CustomAspect cp1 = newCustomAspect('my.new.customaspect')
            CustomAspect cp2 = newCustomAspect('my.newer.customaspect')

            def scenario = newScenario(unit) {
                associateWithDomain(domain, "NormalScenario", "NEW")
            }
            scenarioRepository.save(scenario)
            def scenarioId = scenario.getIdAsString()

            def scenario2 = newScenario(unit) {
                associateWithDomain(domain, "NormalScenario", "NEW")
            }
            scenarioRepository.save(scenario2)
            def scenario2Id = scenario2.getIdAsString()

            def process2 = newProcess(unit) {
                associateWithDomain(domain, "NormalProcess", "NEW")
                customAspects = [cp1, cp2] as Set
            }
            processRepository.save(process2)
            postRisk1(process2.idAsString, scenarioId)
            postRisk2(process2.idAsString, scenario2Id)
        }
        def rowCountBeforeQuery = DataSourceProxyBeanPostProcessor.totalResultSetRowsRead

        when: "all processes are searched for with risks"
        def searchUrl = parseJson(post("/processes/searches", [
            unitId: [
                values: [
                    unit.id.uuidValue()
                ]
            ]
        ])).searchUrl
        def result = parseJson(get(new URI(searchUrl + "?embedRisks=true")))

        then: "the risks are embedded"
        result.items != null
        result.items.size() == NUM_PROCESSES
        result.items.each { assert it.risks != null }

        and: "the number of read rows is acceptable"
        // 210 is the currently observed count of 184 rows plus an acceptable safety margin
        DataSourceProxyBeanPostProcessor.totalResultSetRowsRead - rowCountBeforeQuery < 210
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
                                    category               : "A",
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
                                    category               : "A",
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
}
