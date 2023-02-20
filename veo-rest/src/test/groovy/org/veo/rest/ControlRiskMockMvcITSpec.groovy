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

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.exception.RiskConsistencyException
import org.veo.core.entity.riskdefinition.ImplementationStateDefinition
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Test risk related functionality on controls.
 */
@WithUserDetails("user@domain.example")
class ControlRiskMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    private String unitId
    private String domainId

    def setup() {
        executeInTransaction {
            def client = createTestClient()
            def domain = newDomain(client) {
                applyElementTypeDefinition(newElementTypeDefinition("control", it) {
                    subTypes = [
                        NormalControl: newSubTypeDefinition()
                    ]
                })
                riskDefinitions = [
                    "myFirstRiskDefinition": createRiskDefinition("myFirstRiskDefinition"),
                    "mySecondRiskDefinition": createRiskDefinition("mySecondRiskDefinition"),
                    "myThirdRiskDefinition": createRiskDefinition("myThirdRiskDefinition"),
                    "theOneWithOnlyTwoImplementationStatuses": createRiskDefinition("theOneWithOnlyTwoImplementationStatuses") {
                        implementationStateDefinition = new ImplementationStateDefinition([
                            newCategoryLevel("not done"),
                            newCategoryLevel("done"),
                        ])
                    },
                ]
            }
            domainId = domain.idAsString
            unitId = unitRepository.save(newUnit(client)).idAsString
            clientRepository.save(client)
        }
    }

    def "can create and update control implementation status"() {
        when: "creating the control with risk values for different risk definitions"
        def controlId = parseJson(post("/controls", [
            name: "Super CTL",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "NormalControl",
                    status: "NEW",
                    riskValues: [
                        myFirstRiskDefinition : [
                            implementationStatus: 0
                        ],
                        mySecondRiskDefinition : [
                            implementationStatus: 1
                        ]
                    ]
                ]
            ]
        ])).resourceId

        and: "retrieving it"
        def getControlResponse = get("/controls/$controlId")
        def controlETag = getETag(getControlResponse)
        def retrievedControl = parseJson(getControlResponse)

        then: "the retrieved risk values are complete"
        retrievedControl.domains[domainId].riskValues.myFirstRiskDefinition.implementationStatus == 0
        retrievedControl.domains[domainId].riskValues.mySecondRiskDefinition.implementationStatus == 1

        when: "updating the risk values on the control"
        put("/controls/$controlId", [
            name: "Super CTL",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "NormalControl",
                    status: "NEW",
                    riskValues: [
                        myFirstRiskDefinition : [
                            implementationStatus: 2
                        ],
                        myThirdRiskDefinition : [
                            implementationStatus: 3
                        ]
                    ]
                ]
            ]
        ], ['If-Match': controlETag])

        and: "retrieving it again"
        def updatedControl = parseJson(get("/controls/$controlId"))

        then: "the changes have been applied"
        updatedControl.domains[domainId].riskValues.myFirstRiskDefinition.implementationStatus == 2
        updatedControl.domains[domainId].riskValues.mySecondRiskDefinition == null
        updatedControl.domains[domainId].riskValues.myThirdRiskDefinition.implementationStatus == 3
    }

    def "missing implementation status is handled"() {
        when: "creating a control with no implementation statuses"
        def controlId = parseJson(post("/controls", [
            name: "Super CTL",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "NormalControl",
                    status: "NEW",
                    riskValues: [
                        myFirstRiskDefinition : [:],
                        mySecondRiskDefinition : [:],
                    ]
                ]
            ]
        ])).resourceId

        then: "implementation statuses are missing from the response"
        with(parseJson(get("/controls/$controlId"))) {
            domains[owner.domainId].riskValues.myFirstRiskDefinition.implementationStatus == null
            domains[owner.domainId].riskValues.mySecondRiskDefinition.implementationStatus == null
        }
    }

    def "undefined implementation status is rejected"() {
        when: "creating a control with a valid implementation status"
        post("/controls", [
            name: "Super CTL",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "NormalControl",
                    status: "NEW",
                    riskValues: [
                        theOneWithOnlyTwoImplementationStatuses : [
                            implementationStatus: 1
                        ]
                    ]
                ]
            ]
        ])

        then:
        notThrown(Exception)

        when: "creating a control with an undefined implementation status"
        post("/controls", [
            name: "Super CTL",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "NormalControl",
                    status: "NEW",
                    riskValues: [
                        theOneWithOnlyTwoImplementationStatuses : [
                            implementationStatus: 2
                        ]
                    ]
                ]
            ]
        ], 422)

        then: "it is rejected"
        def ex = thrown(RiskConsistencyException)
        ex.message == "Risk definition theOneWithOnlyTwoImplementationStatuses contains no implementation status with ordinal value 2"
    }
}
