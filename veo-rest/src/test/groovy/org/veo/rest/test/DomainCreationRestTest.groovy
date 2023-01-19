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
        unitId = postNewUnit("domain test unit").resourceId
    }

    def "create new domain"() {
        when: "creating a new domain"
        def domainName = "Domain creation test ${randomUUID()}"
        def domainId = post("/domains", [
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
        post("/domains/$domainId/elementtypedefinitions/asset/updatefromobjectschema", [
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
                                    if: [
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
        ], 204, CONTENT_CREATOR)

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
        def templateUri = post("/domains/$domainId/createdomaintemplate", [
            version: "1.0.0"
        ]).body.targetUri

        and: "applying the template in another client"
        def secondaryClientUnitId = post("/units", [
            name: "secondary client unit"
        ], 201, SECONDARY_CLIENT_USER).body.resourceId
        post("$templateUri/createdomains", null, 204, ADMIN)

        and: "looking up secondary client's domain"
        def secondaryClientDomain = get("/domains", 200, SECONDARY_CLIENT_USER).body
                .find { it.name == domainName }

        then: "its metadata is correct"
        secondaryClientDomain.authority == "JJ"
        secondaryClientDomain.templateVersion == "1.0.0"

        and: "an element can be created in the domain"
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
        post("/domains", [
            name: "DS-GVO",
            authority: "JJ",
        ], 409, CONTENT_CREATOR)
        .body.message == "Templates already exist for domain name 'DS-GVO'"
    }

    def "cannot create multiple domains with the same name"() {
        given: "a random name"
        def name = "conflict test domain ${randomUUID()}"

        expect: "initial creation to succeed"
        post("/domains", [
            name: name,
            authority: "JJ",
        ], 201, CONTENT_CREATOR)

        and: "second creation to fail"
        post("/domains", [
            name: name,
            authority: "JJ",
        ], 409, CONTENT_CREATOR)
        .body.message == "A domain with name '$name' already exists in this client"
    }
}
