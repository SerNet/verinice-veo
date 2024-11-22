/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
import org.veo.core.entity.exception.NotFoundException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Test risk related functionality on scopes.
 */
@WithUserDetails("user@domain.example")
class ProcessRiskNotFoundMockMvcITSpec extends VeoMvcSpec {

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
                applyElementTypeDefinition(newElementTypeDefinition("process", it) {
                    subTypes = [
                        RiskyProcess: newSubTypeDefinition()
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition("scenario", it) {
                    subTypes = [
                        BestCase: newSubTypeDefinition()
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition("scope", it) {
                    subTypes = [
                        SmallScope: newSubTypeDefinition()
                    ]
                })
            }.idAsString
            client = clientRepository.save(client)
            unitId = unitRepository.save(newUnit(client)).idAsString
        }
    }

    def "Get on nonexistent risk returns error code 404"() {
        given: "a process and scenario are created but no risk"
        def processId = parseJson(post("/processes", [
            domains: [
                (domainId): [
                    subType: "RiskyProcess",
                    status: "NEW",
                ]
            ],
            name: "risk test process",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId

        def scenarioId = parseJson(post("/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "BestCase",
                    status: "NEW",
                ]
            ]
        ])).resourceId

        when: "trying to get nonexistent risk returns 404"
        get("/processes/$processId/risks/$scenarioId", 404)

        then:
        thrown(NotFoundException)
    }

    def "Getting risk on nonexistent process returns 404"() {
        given: "a scenario is created but no process"
        def scenarioId = parseJson(post("/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "BestCase",
                    status: "NEW",
                ]
            ]
        ])).resourceId
        def randomUuid = UUID.randomUUID()

        when: "trying to get risk from nonexistent entity returns 404"
        get("/processes/$randomUuid/risks/$scenarioId", 404)

        then:
        thrown(NotFoundException)
    }

    def "Getting risk on nonexistent scenario returns 404"() {
        given: "a process is created"
        def processId = parseJson(post("/processes", [
            domains: [
                (domainId): [
                    subType: "RiskyProcess",
                    status: "NEW",
                ]
            ],
            name: "risk test process",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
        def randomUuid = UUID.randomUUID()

        when: "trying to get nonexistent risk returns 404"
        get("/processes/$processId/risks/$randomUuid", 404)

        then:
        thrown(NotFoundException)
    }
}