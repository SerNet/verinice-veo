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
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.repository.UnitRepository
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl

@WithUserDetails("user@domain.example")
class LinkingMvcITSpec extends VeoMvcSpec {

    @Autowired
    ClientRepositoryImpl clientRepository

    @Autowired
    DomainRepositoryImpl domainRepository

    @Autowired
    UnitRepository unitRepository

    @Autowired
    TransactionTemplate txTemplate

    String domainId
    String unitId

    def setup() {
        txTemplate.execute {
            def client = createTestClient()
            domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).id.uuidValue()
            unitId = unitRepository.save(newUnit(client)).id.uuidValue()
        }
    }

    def "save multiple links"() {
        given: "three persons"
        def person1 = parseJson(post("/persons", [
            name: "person 1",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
        def person2 = parseJson(post("/persons", [
            name: "person 2",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
        def person3 = parseJson(post("/persons", [
            name: "person 3",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId

        when: "creating a scope with different links to all persons"
        def scopeId = parseJson(post("/scopes", [
            links: [
                scope_dataProtectionOfficer: [
                    [
                        target: [targetUri: "http://localhost/persons/$person1"]
                    ],
                    [
                        target: [targetUri: "http://localhost/persons/$person2"]
                    ]
                ],
                scope_headOfDataProcessing: [
                    [
                        target: [targetUri: "http://localhost/persons/$person2"]
                    ],
                    [
                        target: [targetUri: "http://localhost/persons/$person3"]
                    ]
                ]
            ],
            name : "scope",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
        def retrievedScope = parseJson(get("/scopes/$scopeId"))

        then:
        with(retrievedScope.links.scope_dataProtectionOfficer.sort {it.name}) {
            size() == 2
            it[0].target.targetUri == "http://localhost/persons/$person1"
            it[1].target.targetUri == "http://localhost/persons/$person2"
        }
        with(retrievedScope.links.scope_headOfDataProcessing.sort{it.name}) {
            size() == 2
            it[0].target.targetUri == "http://localhost/persons/$person2"
            it[1].target.targetUri == "http://localhost/persons/$person3"
        }
    }

    def "link target sub type is validated"() {
        given:
        def controllerPerson = parseJson(post("/persons", [
            name: "Jane",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "PER_DataProtectionOfficer",
                    status: "NEW"
                ]
            ]
        ])).resourceId
        def randomPerson = parseJson(post("/persons", [
            name: "John",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "PER_Person",
                    status: "NEW"
                ]
            ]
        ])).resourceId

        when: "posting a process with a controller link to the controller person"
        post("/processes", [
            name: "My little process",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            links: [
                process_controller: [
                    [
                        target: [
                            targetUri: "http://localhost/persons/$controllerPerson"
                        ]
                    ]
                ]
            ]
        ])
        then:
        noExceptionThrown()

        when: "posting a process with a controller link to the other person"
        post("/processes", [
            name: "My little process",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            links: [
                process_controller: [
                    [
                        target: [
                            targetUri: "http://localhost/persons/$randomPerson"
                        ]
                    ]
                ]
            ]
        ], 201)
        then:
        noExceptionThrown()
    }
}
