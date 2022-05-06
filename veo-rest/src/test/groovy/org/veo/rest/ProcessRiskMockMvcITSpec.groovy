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
import org.springframework.test.web.servlet.ResultActions
import org.springframework.transaction.support.TransactionTemplate

import com.github.JanLoebel.jsonschemavalidation.JsonSchemaValidationException

import org.veo.core.VeoMvcSpec
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

import spock.lang.Issue

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
        given: "scopes with different risk definitions"
        def firstScopeId = postScope("myFirstRiskDefinition")
        def secondScopeId = postScope("mySecondRiskDefinition")
        def thirdScopeId = postScope("myThirdRiskDefinition")

        when: "creating a process with impact values for different risk definitions"
        def processId = parseJson(post("/processes?scopes=$firstScopeId,$secondScopeId,$thirdScopeId", [
            name: "Super PRO",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
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

    def "process must be in a scope with the used risk definition"() {
        given: "a process in a scope with myFirstRiskDefinition"
        def scopeId = postScope("myFirstRiskDefinition")
        def processId = parseJson(post("/processes?scopes=$scopeId", [
            name: "Super PRO",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [:]
            ]
        ])).resourceId
        def processETag = getETag(get("/processes/$processId"))

        when: "trying to update the process with risk values for mySecondRiskDefinition"
        put("/processes/$processId", [
            name: "Super PRO",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    riskValues: [
                        mySecondRiskDefinition: [
                            potentialImpacts: [
                                "C": 1,
                                "I": 2,
                            ]
                        ]
                    ]
                ]
            ]
        ], ['If-Match': processETag], 400)

        then: "it fails"
        IllegalArgumentException ex = thrown()
        ex.message == "Cannot use risk definition 'mySecondRiskDefinition' because the element is not a member of a scope with that risk definition"

        when: "adding the process to a composite that is in a scope with mySecondRiskDefinition"
        def mySecondRiskDefinitionScopeId = postScope("mySecondRiskDefinition")
        post("/processes?scopes=$mySecondRiskDefinitionScopeId", [
            name: "Super composite PRO",
            parts: [
                [targetUri: "http://localhost/processes/$processId"]
            ],
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [:]
            ]
        ])

        and: "updating the process with risk values for mySecondRiskDefinition"
        put("/processes/$processId", [
            name: "Super PRO",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    riskValues: [
                        mySecondRiskDefinition: [
                            potentialImpacts: [
                                "C": 1,
                                "I": 2,
                            ]
                        ]
                    ]
                ]
            ]
        ], ['If-Match': processETag])

        then: "it succeeds"
        notThrown(Exception)
    }


    def "can't create process with wrong riskdefinition id"() {
        given: "scopes with myFirstRiskDefinition & mySecondRiskDefinition"
        def myFirstRiskDefinitionScopeId = postScope("myFirstRiskDefinition")
        def mySecondRiskDefinitionScopeId = postScope("mySecondRiskDefinition")

        when: "creating a process in those scopes that uses a different risk definition"
        post("/processes?scopes=$myFirstRiskDefinitionScopeId,$mySecondRiskDefinitionScopeId", [
            name: "Super PRO wrong",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
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
        JsonSchemaValidationException ex = thrown()
        ex.message.contains( 'myFirstWrongDefinition' )
    }

    def "can't create process with wrong impact"() {
        given: "scopes with myFirstRiskDefinition & mySecondRiskDefinition"
        def firstScopeId = postScope("myFirstRiskDefinition")
        def secondScopeId = postScope("mySecondRiskDefinition")

        when: "creating a process with risk values for different risk definitions"
        post("/processes?scopes=$firstScopeId,$secondScopeId", [
            name: "Super PRO wrong",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
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
        ])

        then: "an exception is thrown"
        JsonSchemaValidationException ex = thrown()
        ex.message.contains( 'potentialImpacts.E' )
    }

    def "can't create process with wrong impact value"() {
        given: "a scope with mySecondRiskDefinition"
        def scopeId = postScope("mySecondRiskDefinition")

        when: "creating the process with risk values for different risk definitions"
        post("/processes?scopes=$scopeId", [
            name: "Super PRO wrong",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
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
        JsonSchemaValidationException ex = thrown()
        ex.message.contains( 'potentialImpacts.C' )
    }

    private String postScope(String riskDefinition) {
        return parseJson(post("/scopes", [
            name: "$riskDefinition scope",
            domains: [
                (domainId): [
                    riskDefinition: riskDefinition
                ]
            ],
            owner: [targetUri: "http://localhost/units/$unitId"],
        ])).resourceId
    }
}
