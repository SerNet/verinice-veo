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
import org.veo.core.entity.exception.ReferenceTargetNotFoundException
import org.veo.persistence.access.ClientRepositoryImpl
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
    TransactionTemplate txTemplate

    private String unitId
    private String domainId

    def setup() {
        txTemplate.execute {
            def client = createTestClient()
            domainId = newDomain(client) {
                riskDefinitions = [
                    "default-risk-definition": createRiskDefinition("default-risk-definition"),
                    "risk-definition-for-projects": createRiskDefinition("risk-definition-for-projects"),
                ]
                applyElementTypeDefinition(newElementTypeDefinition("scope", it) {
                    subTypes = [
                        RiskyScope: newSubTypeDefinition()
                    ]
                })
            }.idAsString
            unitId = unitRepository.save(newUnit(client)).idAsString
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
        ReferenceTargetNotFoundException ex = thrown()
        ex.message == "Risk definition 'fantasy-definition' was not found for domain '$domainId'"
    }
}
