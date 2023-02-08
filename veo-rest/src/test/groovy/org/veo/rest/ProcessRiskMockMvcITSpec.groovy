/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler.
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
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.MethodArgumentNotValidException

import org.veo.core.VeoMvcSpec
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Test risk related functionality on controls.
 */
@WithUserDetails("user@domain.example")
class ProcessRiskMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    TransactionTemplate txTemplate

    private String unitId
    private String domainId

    def setup() {
        txTemplate.execute {
            def client = createTestClient()
            def domain = newDomain(client) {
                applyElementTypeDefinition(newElementTypeDefinition("process", it) {
                    subTypes = [
                        DifficultProcess: newSubTypeDefinition()
                    ]
                })
                riskDefinitions = [
                    "myFirstRiskDefinition": createRiskDefinition("myFirstRiskDefinition"),
                    "mySecondRiskDefinition": createRiskDefinition("mySecondRiskDefinition"),
                    "myThirdRiskDefinition": createRiskDefinition("myThirdRiskDefinition")
                ]
            }
            domainId = domain.idAsString
            unitId = unitRepository.save(newUnit(client)).idAsString
            clientRepository.save(client)
        }
    }

    def "can update process impact"() {
        when: "creating a process with impact values for different risk definitions"
        def processId = parseJson(post("/processes", [
            name: "Super PRO",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "DifficultProcess",
                    status: "NEW",
                    riskValues: [
                        myFirstRiskDefinition: [
                            potentialImpacts: [
                                "C": 0,
                                "I": 1
                            ]
                        ],
                        mySecondRiskDefinition: [
                            potentialImpacts: [
                                "C": 1
                            ]
                        ]
                    ]
                ]
            ]
        ])).resourceId

        and: "retrieving it"
        def getProcessResponse = get("/processes/$processId")
        def processETag = getETag(getProcessResponse)
        def retrievedProcess = parseJson(getProcessResponse)

        then: "the retrieved risk values are complete"
        retrievedProcess.domains[domainId].riskValues.myFirstRiskDefinition.potentialImpacts.size() == 2
        retrievedProcess.domains[domainId].riskValues.myFirstRiskDefinition.potentialImpacts.C == 0
        retrievedProcess.domains[domainId].riskValues.myFirstRiskDefinition.potentialImpacts.I == 1
        retrievedProcess.domains[domainId].riskValues.mySecondRiskDefinition.potentialImpacts.C == 1

        when: "updating the risk values on the process"
        put("/processes/$processId", [
            name: "Super PRO1",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "DifficultProcess",
                    status: "NEW",
                    riskValues: [
                        myFirstRiskDefinition: [
                            potentialImpacts: [ "C": 1,
                                "I": 2
                            ]
                        ],
                        myThirdRiskDefinition: [
                            potentialImpacts: [ "C": 1,
                                "I": 2
                            ]
                        ]
                    ]
                ]
            ]
        ], ['If-Match': processETag])

        and: "retrieving it again"
        def updatedProcess = parseJson(get("/processes/$processId"))

        then: "the changes have been applied"
        updatedProcess.domains[domainId].riskValues.myFirstRiskDefinition.potentialImpacts.C == 1
        updatedProcess.domains[domainId].riskValues.mySecondRiskDefinition == null
        updatedProcess.domains[domainId].riskValues.myThirdRiskDefinition.potentialImpacts.C == 1
        updatedProcess.domains[domainId].riskValues.myThirdRiskDefinition.potentialImpacts.I == 2
    }

    def "can't create process with wrong riskdefinition id"() {
        when: "creating a process that uses a different risk definition"
        post("/processes", [
            name: "Super PRO wrong",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "DifficultProcess",
                    status: "NEW",
                    riskValues: [
                        myFirstWrongDefinition: [
                            potentialImpacts: [
                                "E": 0,
                                "GGG": 1
                            ]
                        ],
                        mySecondRiskDefinition: [
                            potentialImpacts: [
                                "C": 1
                            ]
                        ]
                    ]
                ]
            ]
        ],400)

        then: "an exception is thrown"
        IllegalArgumentException ex = thrown()
        ex.message.contains( 'myFirstWrongDefinition' )
    }

    def "can't create process with wrong impact"() {
        when: "creating a process with risk values for different risk definitions"
        post("/processes", [
            name: "Super PRO wrong",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "DifficultProcess",
                    status: "NEW",
                    riskValues: [
                        myFirstRiskDefinition: [
                            potentialImpacts: [
                                "E": 0,
                                "GGG": 1
                            ]
                        ],
                        mySecondRiskDefinition: [
                            potentialImpacts: [
                                "C": 1
                            ]
                        ]
                    ]
                ]
            ]
        ], 400)

        then: "an exception is thrown"
        IllegalArgumentException ex = thrown()
        ex.message == "Category: 'E' not defined in myFirstRiskDefinition"
    }

    def "can't create process with wrong impact value"() {
        when: "creating the process with risk values for different risk definitions"
        post("/processes", [
            name: "Super PRO wrong",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "DifficultProcess",
                    status: "NEW",
                    riskValues: [
                        mySecondRiskDefinition: [
                            potentialImpacts: [
                                "C": 10
                            ]
                        ]
                    ]
                ]
            ]
        ], 400)

        then: "an exception is thrown"
        IllegalArgumentException ex = thrown()
        ex.message == "Impact value 10 for category 'C' is out of range"
    }
}
