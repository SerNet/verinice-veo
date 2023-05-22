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
package org.veo.rest.test

import static java.util.UUID.randomUUID
import static org.veo.rest.test.UserType.ADMIN
import static org.veo.rest.test.UserType.CONTENT_CREATOR
import static org.veo.rest.test.UserType.SECONDARY_CLIENT_USER

class DomainCreationRestTest extends VeoRestTest {
    String unitId

    def setup() {
        unitId = postNewUnit().resourceId
    }

    def "create new domain"() {
        when: "creating a new domain"
        def domainName = "Domain creation test ${randomUUID()}"
        def domainId = post("/content-creation/domains", [
            name: domainName,
            abbreviation: "dct",
            description: "best one ever",
            authority: "JJ",
        ], 201, CONTENT_CREATOR).body.resourceId

        then: "it can be retrieved"
        with(get("/domains/$domainId").body) {
            name == domainName
            abbreviation == "dct"
            description == "best one ever"
            authority == "JJ"
            templateVersion == "0.1.0"
            domainTemplate == null
        }

        when: "defining an element type in the domain"
        postAssetObjectSchema(domainId)

        and: "adding the domain to the unit"
        get("/units/$unitId").with{
            body.domains.add([targetUri: "/domains/$domainId"])
            put("/units/$unitId", body, getETag())
        }

        then: "an element can be created in the domain"
        post("/assets", [
            name: "main server",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "server",
                    status: "on"
                ]
            ]
        ])

        when: "creating a template from the domain"
        def templateUri = post("/content-creation/domains/$domainId/template", [
            version: "1.0.0"
        ]).body.targetUri

        and: "applying the template in another client"
        post("$templateUri/createdomains", null, 204, ADMIN)

        and: "looking up secondary client's domain"
        def secondaryClientDomain = get("/domains", 200, SECONDARY_CLIENT_USER).body
                .find { it.name == domainName }

        then: "its metadata is correct"
        secondaryClientDomain.authority == "JJ"
        secondaryClientDomain.templateVersion == "1.0.0"

        and: "an element can be created in the domain"
        def secondaryClientUnitId = post("/units", [
            name: "secondary client unit",
            domains: [
                [targetUri: secondaryClientDomain._self]
            ]
        ], 201, SECONDARY_CLIENT_USER).body.resourceId
        post("/assets", [
            name: "gain server",
            owner: [targetUri: "http://localhost/units/$secondaryClientUnitId"],
            domains: [
                (secondaryClientDomain.id): [
                    subType: "server",
                    status: "off"
                ]
            ]
        ], 201, SECONDARY_CLIENT_USER)
    }

    def "cannot create domain with name of existing template"() {
        expect:
        post("/content-creation/domains", [
            name: "DS-GVO",
            authority: "JJ",
        ], 409, CONTENT_CREATOR)
        .body.message == "Templates already exist for domain name 'DS-GVO'"
    }

    def "cannot create multiple domains with the same name"() {
        given: "a random name"
        def name = "conflict test domain ${randomUUID()}"

        expect: "initial creation to succeed"
        post("/content-creation/domains", [
            name: name,
            authority: "JJ",
        ], 201, CONTENT_CREATOR)

        and: "second creation to fail"
        post("/content-creation/domains", [
            name: name,
            authority: "JJ",
        ], 409, CONTENT_CREATOR)
        .body.message == "A domain with name '$name' already exists in this client"
    }

    def "create new domain and delete"() {
        when: "creating a new domain"
        def domainName = "Domain deletion test 1"
        def domainId = post("/content-creation/domains", [
            name: domainName,
            abbreviation: "11",
            description: "my",
            authority: "qq",
        ], 201, CONTENT_CREATOR).body.resourceId

        then: "it can be retrieved"
        with(get("/domains/$domainId").body) {
            name == domainName
            abbreviation == "11"
            description == "my"
            authority == "qq"
            templateVersion == "0.1.0"
            domainTemplate == null
        }

        when:
        delete("/content-creation/domains/${domainId}", 204)

        then:
        get("/domains/$domainId", 404)
    }

    def "delete only domains not in use"() {
        when: "we have a domain"
        def domainId = post("/content-creation/domains", [
            name: "Domain deletion test 2",
            abbreviation: "111",
            description: "my1",
            authority: "qq1",
        ], 201, CONTENT_CREATOR).body.resourceId

        and: "it is linked to a unit"
        def unitId = post("/units", [
            name: 'my unit used by domain',
            domains: [
                [targetUri: "/domains/${domainId}"]
            ]
        ]).body.resourceId

        then: "delete is not allowed"
        with(delete("/content-creation/domains/${domainId}", 409).body) {
            message ==~ /Domain in use.*/
        }

        when: "we remove the link"
        delete("/units/${unitId}")

        then: "delete is allowed and the domain is deleted"
        delete("/content-creation/domains/${domainId}",204)
        with(get("/domains/$domainId", 404).body) {
            message ==~ /Domain with ID .* not found/
        }
    }

    def "create new domain with schema and delete"() {
        given:
        def domainId = post("/content-creation/domains", [
            name: "Domain deletion test 3",
            abbreviation: "11",
            description: "my",
            authority: "qq",
        ], 201, CONTENT_CREATOR).body.resourceId

        when: "defining an element type in the domain"
        postAssetObjectSchema(domainId)

        and:
        delete("/content-creation/domains/${domainId}", 204)

        then:
        get("/domains/${domainId}", 404)
    }

    def postAssetObjectSchema(String domainId) {
        post("/content-creation/domains/$domainId/element-type-definitions/asset/object-schema",
                [
                    properties: [
                        domains: [
                            properties: [
                                (domainId): [
                                    properties: [
                                        subType: [
                                            enum: ["server"],
                                        ]
                                    ],
                                    allOf: [
                                        [
                                            "if": [
                                                properties: [
                                                    subType: [
                                                        const: "server"
                                                    ]
                                                ]
                                            ],
                                            then: [
                                                properties: [
                                                    status: [
                                                        enum: ["off", "on"]
                                                    ]
                                                ]
                                            ]
                                        ]
                                    ]
                                ]
                            ]
                        ],
                        customAspects: [
                            properties: [:]
                        ],
                        links: [
                            properties: [:]
                        ],
                        translations: [
                            en: [
                                asset_server_status_off: "off",
                                asset_server_status_on: "on"
                            ]
                        ]
                    ]
                ],
                204, CONTENT_CREATOR)
    }
}
