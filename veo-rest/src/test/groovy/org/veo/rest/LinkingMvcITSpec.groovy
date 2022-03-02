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
import org.veo.core.entity.Person
import org.veo.core.entity.Scope
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
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
            domainId = newDomain(client) {
                it.elementTypeDefinitions = [
                    newElementTypeDefinition(Person.SINGULAR_TERM, it) {
                        subTypes["Normal"] = new SubTypeDefinition().tap{
                            it.statuses = ["NEW"]
                        }
                        subTypes["Nice"] = new SubTypeDefinition().tap{
                            it.statuses = ["NEW"]
                        }
                    },
                    newElementTypeDefinition(Scope.SINGULAR_TERM, it) {
                        links["linkToWhateverPersonA"] = new LinkDefinition().tap{
                            targetType = Person.SINGULAR_TERM
                        }
                        links["linkToWhateverPersonB"] = new LinkDefinition().tap{
                            targetType = Person.SINGULAR_TERM
                        }
                        links["linkToNormalPerson"] = new LinkDefinition().tap{
                            targetType = Person.SINGULAR_TERM
                            targetSubType = "Normal"
                        }
                        links["linkToNicePerson"] = new LinkDefinition().tap{
                            targetType = Person.SINGULAR_TERM
                            targetSubType = "Nice"
                        }
                    },
                ]
            }.idAsString
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
                linkToWhateverPersonA: [
                    [
                        target: [targetUri: "http://localhost/persons/$person1"]
                    ],
                    [
                        target: [targetUri: "http://localhost/persons/$person2"]
                    ]
                ],
                linkToWhateverPersonB: [
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
        with(retrievedScope.links.linkToWhateverPersonA.sort {it.name}) {
            size() == 2
            it[0].target.targetUri == "http://localhost/persons/$person1"
            it[1].target.targetUri == "http://localhost/persons/$person2"
        }
        with(retrievedScope.links.linkToWhateverPersonB.sort{it.name}) {
            size() == 2
            it[0].target.targetUri == "http://localhost/persons/$person2"
            it[1].target.targetUri == "http://localhost/persons/$person3"
        }
    }

    def "link target sub type is validated"() {
        given:
        def nicePersonId = parseJson(post("/persons", [
            name: "Jane",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "Nice",
                    status: "NEW"
                ]
            ]
        ])).resourceId
        def normalPersonId = parseJson(post("/persons", [
            name: "John",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW"
                ]
            ]
        ])).resourceId

        when: "posting a scope with a correct link"
        post("/scopes", [
            name: "Good scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            links: [
                linkToNicePerson: [
                    [
                        target: [
                            targetUri: "http://localhost/persons/$nicePersonId"
                        ]
                    ]
                ]
            ]
        ])
        then:
        noExceptionThrown()

        when: "posting a scope with a wrong link"
        post("/scopes", [
            name: "Bad scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            links: [
                linkToNicePerson: [
                    [
                        target: [
                            targetUri: "http://localhost/persons/$normalPersonId"
                        ]
                    ]
                ]
            ]
        ])
        then:
        noExceptionThrown()
    }
}
