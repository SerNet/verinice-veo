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

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.exception.RiskConsistencyException
import org.veo.core.entity.risk.ImpactReason
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Test risk related functionality on controls.
 */
@WithUserDetails("user@domain.example")
class ProcessRiskMockMvcITSpec extends VeoMvcSpec {

    public static final String EVERYTHING_WRONG = "Sometimes everything is wrong Now it's time to sing along"
    public static final String TOO_MUCH = "If you think you've had too much Of this life, well hang on"
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
                            ],
                            potentialImpactReasons: [
                                "C": ImpactReason.MANUAL.translationKey,
                                "I": ImpactReason.CUMULATIVE.translationKey
                            ],
                            potentialImpactExplanations: [
                                "C": EVERYTHING_WRONG,
                                "I": TOO_MUCH
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

        then: "the retrieved impact values are complete"
        with(retrievedProcess.domains[domainId].riskValues) {
            with(myFirstRiskDefinition) {
                potentialImpacts.size() == 2
                potentialImpacts.C == 0
                potentialImpacts.I == 1

                potentialImpactsCalculated.size() == 0

                potentialImpactsEffective.size() == 2
                potentialImpactsEffective.C == 0
                potentialImpactsEffective.I == 1

                potentialImpactReasons.size() == 2
                potentialImpactReasons.C == ImpactReason.MANUAL.translationKey
                potentialImpactReasons.I == ImpactReason.CUMULATIVE.translationKey

                potentialImpactExplanations.size() == 2
                potentialImpactExplanations.C == ProcessRiskMockMvcITSpec.EVERYTHING_WRONG
                potentialImpactExplanations.I == ProcessRiskMockMvcITSpec.TOO_MUCH

                potentialImpactEffectiveReasons.size() == 2
                potentialImpactEffectiveReasons.C == ImpactReason.MANUAL.translationKey
                potentialImpactEffectiveReasons.I == ImpactReason.CUMULATIVE.translationKey
            }

            with(mySecondRiskDefinition) {
                potentialImpacts.C == 1

                potentialImpactsCalculated.size() == 0

                potentialImpactsEffective.size() == 1
                potentialImpactsEffective.C == 1
            }
        }

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
                            potentialImpacts: [ "C": 2,
                                "I": 0
                            ]
                        ]
                    ]
                ]
            ]
        ], ['If-Match': processETag])

        and: "retrieving it again"
        def updatedProcess = parseJson(get("/processes/$processId"))

        then: "the changes have been applied"
        with(updatedProcess.domains[domainId].riskValues) {
            with(myFirstRiskDefinition) {
                potentialImpacts.C == 1
                potentialImpacts.I == 2

                potentialImpactsEffective.C == 1
                potentialImpactsEffective.I == 2
            }

            mySecondRiskDefinition == null

            with(myThirdRiskDefinition) {
                potentialImpacts.C == 2
                potentialImpacts.I == 0

                potentialImpactsEffective.C == 2
                potentialImpactsEffective.I == 0
            }
        }
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
        ],422)

        then: "an exception is thrown"
        RiskConsistencyException ex = thrown()
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
        ], 422)

        then: "an exception is thrown"
        RiskConsistencyException ex = thrown()
        ex.message == "Risk definition myFirstRiskDefinition contains no category with ID E"
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
