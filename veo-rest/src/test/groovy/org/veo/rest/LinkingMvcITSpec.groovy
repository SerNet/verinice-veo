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
                applyElementTypeDefinition(newElementTypeDefinition(Person.SINGULAR_TERM, it) {
                    subTypes["Normal"] = new SubTypeDefinition().tap{
                        it.statuses = ["NEW"]
                    }
                    subTypes["Nice"] = new SubTypeDefinition().tap{
                        it.statuses = ["NEW"]
                    }
                })
                applyElementTypeDefinition(newElementTypeDefinition(Scope.SINGULAR_TERM, it) {
                    subTypes["Normal"] = new SubTypeDefinition().tap{
                        it.statuses = ["NEW"]
                    }
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
                })
            }.idAsString
            unitId = unitRepository.save(newUnit(client)).id.uuidValue()
        }
    }

    def "save multiple links"() {
        given: "three persons"
        def person1 = parseJson(post("/persons", [
            name: "person 1",
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
            ],
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
        def person2 = parseJson(post("/persons", [
            name: "person 2",
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
            ],
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
        def person3 = parseJson(post("/persons", [
            name: "person 3",
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
            ],
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
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
            ],
        ])).resourceId
        def retrievedScope = parseJson(get("/scopes/$scopeId"))

        then:
        retrievedScope.links.linkToWhateverPersonA*.target*.targetUri ==~ [
            "http://localhost/persons/$person1",
            "http://localhost/persons/$person2",
        ]*.toString()
        retrievedScope.links.linkToWhateverPersonB*.target*.targetUri ==~ [
            "http://localhost/persons/$person2",
            "http://localhost/persons/$person3",
        ]*.toString()
    }

    def "link target type is validated"() {
        given:
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
        def anotherNormalPersonId = parseJson(post("/persons", [
            name: "Mia",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW"
                ]
            ]
        ])).resourceId
        def normalScopeId = parseJson(post("/scopes", [
            name: "Just a normal scope",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW"
                ]
            ]
        ])).resourceId

        when: "posting a scope with a correct link"
        def scopeId = parseJson(post("/scopes", [
            name: "Good scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
            ],
            links: [
                linkToNormalPerson: [
                    [
                        target: [
                            targetUri: "http://localhost/persons/$normalPersonId"
                        ]
                    ]
                ]
            ]
        ])).resourceId
        def scopeETag = getETag(get("/scopes/$scopeId"))

        then:
        noExceptionThrown()

        when: "updating the scope with a valid link"
        put("/scopes/$scopeId", [
            name: "Good scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
            ],
            links: [
                linkToNormalPerson: [
                    [
                        target: [
                            targetUri: "http://localhost/persons/$anotherNormalPersonId"
                        ]
                    ]
                ]
            ]
        ], ['If-Match': scopeETag])
        scopeETag = getETag(get("/scopes/$scopeId"))

        then:
        noExceptionThrown()

        when: "updating the scope with an invalid link"
        put("/scopes/$scopeId", [
            name: "Bad scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
            ],
            links: [
                linkToNormalPerson: [
                    [
                        target: [
                            targetUri: "http://localhost/scopes/$normalScopeId"
                        ]
                    ]
                ]
            ]
        ], ['If-Match': scopeETag], 400)

        then:
        IllegalArgumentException ex = thrown()
        ex.message == "Invalid target type 'scope' for link type 'linkToNormalPerson'"

        when: "posting a scope with an invalid link"
        post("/scopes", [
            name: "Bad scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
            ],
            links: [
                linkToNormalPerson: [
                    [
                        target: [
                            targetUri: "http://localhost/scopes/$normalScopeId"
                        ]
                    ]
                ]
            ]
        ], 400)

        then:
        ex = thrown()
        ex.message == "Invalid target type 'scope' for link type 'linkToNormalPerson'"
    }

    def "link target sub type is validated"() {
        given:
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
        def anotherNicePersonId = parseJson(post("/persons", [
            name: "Junior",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "Nice",
                    status: "NEW"
                ]
            ]
        ])).resourceId

        when: "posting a scope with a correct link"
        def scopeId = parseJson(post("/scopes", [
            name: "Good scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
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
        ])).resourceId
        def scopeETag = getETag(get("/scopes/$scopeId"))

        then:
        noExceptionThrown()

        when: "updating the scope with a valid link"
        put("/scopes/$scopeId", [
            name: "Good scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
            ],
            links: [
                linkToNicePerson: [
                    [
                        target: [
                            targetUri: "http://localhost/persons/$anotherNicePersonId"
                        ]
                    ]
                ]
            ]
        ], ['If-Match': scopeETag])
        scopeETag = getETag(get("/scopes/$scopeId"))

        then:
        noExceptionThrown()

        when: "updating the scope with an invalid link"
        put("/scopes/$scopeId", [
            name: "Bad scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
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
        ], ['If-Match': scopeETag], 400)

        then:
        IllegalArgumentException ex = thrown()
        ex.message == "Expected target of link 'linkToNicePerson' ('John') to have sub type 'Nice' but found 'Normal'"

        when: "posting a scope with an invalid link"
        post("/scopes", [
            name: "Bad scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            domains: [
                (domainId): [
                    subType: "Normal",
                    status: "NEW",
                ]
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
        ], 400)

        then:
        ex = thrown()
        ex.message == "Expected target of link 'linkToNicePerson' ('John') to have sub type 'Nice' but found 'Normal'"
    }
}
