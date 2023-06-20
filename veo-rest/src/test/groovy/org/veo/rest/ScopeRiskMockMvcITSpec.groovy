/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.ReferenceTargetNotFoundException
import org.veo.core.entity.exception.RiskConsistencyException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Test risk related functionality on scopes.
 */
@WithUserDetails("user@domain.example")
class ScopeRiskMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private ScenarioRepositoryImpl scenarioRepository

    @Autowired
    TransactionTemplate txTemplate

    private String unitId
    private String domainId
    private Unit unit
    private Domain domain

    def setup() {
        txTemplate.execute {
            def client = createTestClient()
            domain = newDomain(client) {
                name = "Scope Risk Test"
                templateVersion = "1.1.0"
                riskDefinitions = [
                    "default-risk-definition": createRiskDefinition("default-risk-definition"),
                    "risk-definition-for-projects": createRiskDefinition("risk-definition-for-projects"),
                    "myFirstRiskDefinition": createRiskDefinition("myFirstRiskDefinition"),
                    "mySecondRiskDefinition": createRiskDefinition("mySecondRiskDefinition"),
                    "myThirdRiskDefinition": createRiskDefinition("myThirdRiskDefinition")
                ]
                applyElementTypeDefinition(newElementTypeDefinition("scope", it) {
                    subTypes = [
                        RiskyScope: newSubTypeDefinition()
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition("scenario", it) {
                    subTypes = [
                        SCN_Scenario: newSubTypeDefinition()
                    ]
                })
            }
            domainId = domain.idAsString
            unit = unitRepository.save(newUnit(client))
            unitId = unit.idAsString
            clientRepository.save(client)
        }
    }

    def "can create a scope with a risk definition reference"() {
        when: "creating a scope with reference to a risk definition"
        def scopeId = parseJson(post("/scopes", [
            name: "Project scope",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "RiskyScope",
                    status: "NEW",
                    riskDefinition: "risk-definition-for-projects"
                ]
            ]
        ])).resourceId

        and: "retrieving it"
        def getScopeResponse = get("/scopes/$scopeId")
        def retrievedScope = parseJson(getScopeResponse)

        then: "the retrieved risk values are complete"
        retrievedScope.domains[domainId].riskDefinition == "risk-definition-for-projects"
    }

    def "can change risk definition reference"() {
        when: "creating a scope without a risk definition"
        def scopeId = parseJson(post("/scopes", [
            name: "Project scope",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "RiskyScope",
                    status: "NEW",
                ]
            ]
        ])).resourceId
        def scopeETag = getETag(get("/scopes/$scopeId"))

        and: "adding a risk definition reference"
        put("/scopes/$scopeId", [
            name: "Project scope",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "RiskyScope",
                    status: "NEW",
                    riskDefinition: "risk-definition-for-projects"
                ]
            ]
        ], ['If-Match': scopeETag])

        and: "retrieving the scope"
        def retrieveScopeResponse = get("/scopes/$scopeId")
        def retrievedScope = parseJson(retrieveScopeResponse)
        scopeETag = getETag(retrieveScopeResponse)

        then: "the change has been applied"
        retrievedScope.domains[domainId].riskDefinition == "risk-definition-for-projects"

        when: "making a change not related to the risk definition"
        put("/scopes/$scopeId", [
            name: "Cinema scope",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "RiskyScope",
                    status: "NEW",
                    riskDefinition: "risk-definition-for-projects"
                ]
            ]
        ], ['If-Match': scopeETag])
        retrieveScopeResponse = get("/scopes/$scopeId")
        scopeETag = getETag(retrieveScopeResponse)

        then: "it is persisted"
        parseJson(retrieveScopeResponse).domains[domainId].riskDefinition == "risk-definition-for-projects"

        when: "changing the risk definition"
        put("/scopes/$scopeId", [
            name: "Cinema scope",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "RiskyScope",
                    status: "NEW",
                    riskDefinition: "default-risk-definition"
                ]
            ]
        ], ['If-Match': getETag(get("/scopes/$scopeId"))])
        retrieveScopeResponse = get("/scopes/$scopeId")
        scopeETag = getETag(retrieveScopeResponse)

        then: "it is persisted"
        parseJson(retrieveScopeResponse).domains[domainId].riskDefinition == "default-risk-definition"

        when: "removing the risk definition reference"
        put("/scopes/$scopeId", [
            name: "Cinema scope",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "RiskyScope",
                    status: "NEW",
                ]
            ]
        ], ['If-Match': scopeETag])
        retrieveScopeResponse = get("/scopes/$scopeId")

        then: "it is persisted"
        parseJson(retrieveScopeResponse).domains[domainId].riskDefinition == null
    }

    def "invalid risk definition reference is rejected"() {
        when: "creating a scope with reference to a missing risk definition"
        post("/scopes", [
            name: "Project scope",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "RiskyScope",
                    status: "NEW",
                    riskDefinition: "fantasy-definition"
                ]
            ]
        ], 422)

        then:
        RiskConsistencyException ex = thrown()
        ex.message == "Domain Scope Risk Test 1.1.0 contains no risk definition with ID fantasy-definition"
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be created for a scope"() {
        given: "saved elements"
        def scopeId = parseJson(post("/scopes", [
            name: "Project scope",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "RiskyScope",
                    status: "NEW",
                ]
            ]
        ])).resourceId

        def scenario = txTemplate.execute {
            scenarioRepository.save(newScenario(unit) {
                associateWithDomain(domain, "SCN_Scenario", "NEW")
            })
        }

        when: "a new risk can be created successfully"
        def json = parseJson(post("/scopes/$scopeId/risks", [
            scenario: [ targetUri: '/scenarios/'+ scenario.id.uuidValue() ],
            domains: [
                (domain.getIdAsString()) : [
                    reference: [targetUri: '/domains/'+ domain.id.uuidValue() ]
                ]
            ]
        ] as Map))

        then:
        with(json) {
            resourceId != null
            resourceId.length() == 36
            success
            message == "Scope risk created successfully."
        }
    }

    def "can update scope impact"() {
        when: "creating a scope with impact values for different risk definitions"
        def scopeId = parseJson(post("/scopes", [
            name: "Super SCO",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "RiskyScope",
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
        def getProcessResponse = get("/scopes/$scopeId")
        def processETag = getETag(getProcessResponse)
        def retrievedProcess = parseJson(getProcessResponse)

        then: "the retrieved risk values are complete"
        retrievedProcess.domains[domainId].riskValues.myFirstRiskDefinition.potentialImpacts.size() == 2
        retrievedProcess.domains[domainId].riskValues.myFirstRiskDefinition.potentialImpacts.C == 0
        retrievedProcess.domains[domainId].riskValues.myFirstRiskDefinition.potentialImpacts.I == 1
        retrievedProcess.domains[domainId].riskValues.mySecondRiskDefinition.potentialImpacts.C == 1

        when: "updating the risk values on the asset"
        put("/scopes/$scopeId", [
            name: "Super PRO1",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "RiskyScope",
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
        def updatedProcess = parseJson(get("/scopes/$scopeId"))

        then: "the changes have been applied"
        updatedProcess.domains[domainId].riskValues.myFirstRiskDefinition.potentialImpacts.C == 1
        updatedProcess.domains[domainId].riskValues.mySecondRiskDefinition == null
        updatedProcess.domains[domainId].riskValues.myThirdRiskDefinition.potentialImpacts.C == 1
        updatedProcess.domains[domainId].riskValues.myThirdRiskDefinition.potentialImpacts.I == 2
    }
}
