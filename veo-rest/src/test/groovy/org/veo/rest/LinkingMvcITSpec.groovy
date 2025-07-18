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
import org.veo.core.entity.ElementType
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
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.PERSON, it) {
                    subTypes["Normal"] = new SubTypeDefinition().tap{
                        it.statuses = ["NEW"]
                    }
                    subTypes["Nice"] = new SubTypeDefinition().tap{
                        it.statuses = ["NEW"]
                    }
                    links["favScope"] = new LinkDefinition().tap{
                        targetType = ElementType.SCOPE
                        targetSubType = "Normal"
                    }
                })
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.SCOPE, it) {
                    subTypes["Normal"] = new SubTypeDefinition().tap{
                        it.statuses = ["NEW"]
                    }
                    links["linkToNormalPerson"] = new LinkDefinition().tap{
                        targetType = ElementType.PERSON
                        targetSubType = "Normal"
                    }
                    links["linkToNicePersonA"] = new LinkDefinition().tap{
                        targetType = ElementType.PERSON
                        targetSubType = "Nice"
                    }
                    links["linkToNicePersonB"] = new LinkDefinition().tap{
                        targetType = ElementType.PERSON
                        targetSubType = "Nice"
                    }
                })
            }.idAsString
            unitId = unitRepository.save(newUnit(client)).idAsString
        }
    }

    def "save multiple links"() {
        given: "three persons"
        def person1 = parseJson(post("/domains/$domainId/persons", [
            name: "person 1",
            subType: "Nice",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
        def person2 = parseJson(post("/domains/$domainId/persons", [
            name: "person 2",
            subType: "Nice",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId
        def person3 = parseJson(post("/domains/$domainId/persons", [
            name: "person 3",
            subType: "Nice",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ])).resourceId

        when: "creating a scope with different links to all persons"
        def scopeId = parseJson(post("/domains/$domainId/scopes", [
            links: [
                linkToNicePersonA: [
                    [
                        target: [targetUri: "http://localhost/persons/$person1"]
                    ],
                    [
                        target: [targetUri: "http://localhost/persons/$person2"]
                    ]
                ],
                linkToNicePersonB: [
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
            subType: "Normal",
            status: "NEW",
        ])).resourceId
        def retrievedScope = parseJson(get("/scopes/$scopeId"))

        then:
        retrievedScope.links.linkToNicePersonA*.target*.targetUri ==~ [
            "http://localhost/persons/$person1",
            "http://localhost/persons/$person2",
        ]*.toString()
        retrievedScope.links.linkToNicePersonB*.target*.targetUri ==~ [
            "http://localhost/persons/$person2",
            "http://localhost/persons/$person3",
        ]*.toString()
    }

    def "fetch links for an element"() {
        given: "a scope with inbound and outbound links"
        def scopeId = parseJson(post("/domains/$domainId/scopes", [
            name: "scope of hope",
            subType: "Normal",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ])).resourceId
        post("/domains/$domainId/persons", [
            name: "person 1",
            subType: "Normal",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
            links: [
                favScope: [
                    [target: [targetUri: "/scopes/$scopeId"]]
                ]
            ],
        ])
        post("/domains/$domainId/persons", [
            name: "person 2",
            subType: "Normal",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
            links: [
                favScope: [
                    [target: [targetUri: "/scopes/$scopeId"]]
                ]
            ],
        ])
        def person3Id = parseJson(post("/domains/$domainId/persons", [
            name: "person 3",
            subType: "Normal",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ])).resourceId
        post("/domains/$domainId/scopes/$scopeId/links", [
            linkToNormalPerson: [
                [target: [targetUri: "/persons/$person3Id"]],
            ]
        ], 204)

        when:
        def scopeLinkPage = parseJson(get("/domains/$domainId/scopes/$scopeId/links?sortBy=LINKED_ELEMENT_NAME"))

        then:
        scopeLinkPage.totalItemCount == 3
        with(scopeLinkPage.items[0]) {
            direction == "INBOUND"
            linkType == "favScope"
            linkedElement.name == "person 1"
            linkedElement.type == "person"
        }
        with(scopeLinkPage.items[1]) {
            direction == "INBOUND"
            linkType == "favScope"
            linkedElement.name == "person 2"
            linkedElement.type == "person"
        }
        with(scopeLinkPage.items[2]) {
            direction == "OUTBOUND"
            linkType == "linkToNormalPerson"
            linkedElement.name == "person 3"
            linkedElement.type == "person"
        }
    }

    def "link target type is validated"() {
        given:
        def normalPersonId = parseJson(post("/domains/$domainId/persons", [
            name: "John",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "Normal",
            status: "NEW"
        ])).resourceId
        def anotherNormalPersonId = parseJson(post("/domains/$domainId/persons", [
            name: "Mia",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "Normal",
            status: "NEW"
        ])).resourceId
        def normalScopeId = parseJson(post("/domains/$domainId/scopes", [
            name: "Just a normal scope",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "Normal",
            status: "NEW"
        ])).resourceId

        when: "posting a scope with a correct link"
        def scopeId = parseJson(post("/domains/$domainId/scopes", [
            name: "Good scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            subType: "Normal",
            status: "NEW",
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
        put("/domains/$domainId/scopes/$scopeId", [
            name: "Good scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            subType: "Normal",
            status: "NEW",
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
        put("/domains/$domainId/scopes/$scopeId", [
            name: "Bad scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            subType: "Normal",
            status: "NEW",
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
        post("/domains/$domainId/scopes", [
            name: "Bad scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            subType: "Normal",
            status: "NEW",
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
        def normalPersonId = parseJson(post("/domains/$domainId/persons", [
            name: "John",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "Normal",
            status: "NEW"
        ])).resourceId
        def nicePersonId = parseJson(post("/domains/$domainId/persons", [
            name: "Jane",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "Nice",
            status: "NEW"
        ])).resourceId
        def anotherNicePersonId = parseJson(post("/domains/$domainId/persons", [
            name: "Junior",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "Nice",
            status: "NEW"
        ])).resourceId

        when: "posting a scope with a correct link"
        def scopeId = parseJson(post("/domains/$domainId/scopes", [
            name: "Good scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            subType: "Normal",
            status: "NEW",
            links: [
                linkToNicePersonA: [
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
        put("/domains/$domainId/scopes/$scopeId", [
            name: "Good scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            subType: "Normal",
            status: "NEW",
            links: [
                linkToNicePersonA: [
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
        put("/domains/$domainId/scopes/$scopeId", [
            name: "Bad scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            subType: "Normal",
            status: "NEW",
            links: [
                linkToNicePersonA: [
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
        ex.message == "Expected target of link 'linkToNicePersonA' ('John') to have sub type 'Nice' but found 'Normal'"

        when: "posting a scope with an invalid link"
        post("/domains/$domainId/scopes", [
            name: "Bad scope",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            subType: "Normal",
            status: "NEW",
            links: [
                linkToNicePersonA: [
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
        ex.message == "Expected target of link 'linkToNicePersonA' ('John') to have sub type 'Nice' but found 'Normal'"
    }
}
