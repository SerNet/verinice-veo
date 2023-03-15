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

class DomainAssociationRestTest extends VeoRestTest {
    String unitUri
    String dsgvoDomainId
    String testDomainId

    def setup() {
        unitUri = "$baseUrl/units/" + postNewUnit("some unit").resourceId
        dsgvoDomainId = domains.find { it.name == "DS-GVO" }.id
        testDomainId = domains.find { it.name == "test-domain" }.id
    }

    def "can associate element with domains"() {
        when:
        def incidentId =post("/incidents", [
            name: "incident with domains",
            domains: [
                (dsgvoDomainId): [
                    subType: "INC_Incident",
                    status: "NEW"
                ]
            ],
            owner: [
                targetUri: unitUri
            ]
        ]).body.resourceId

        then:
        with(get("/incidents/$incidentId").body) {
            domains[owner.dsgvoDomainId].status == "NEW"
        }

        and:
        with(get("/domians/$dsgvoDomainId/incidents/$incidentId").body) {
            id == incidentId
            subType == "INC_Incident"
            status == "NEW"
            it.owner.targetUri == owner.unitUri // "owner" is both a DTO property and a groovy keyword
            _self == "$owner.baseUrl/domians/$owner.dsgvoDomainId/incidents/$incidentId"
        }
    }

    def "cannot create element with custom aspects and without domains"() {
        when:
        def response = post("/assets", [
            name: "asset without domains",
            owner: [
                targetUri: unitUri
            ],
            customAspects: [
                asset_details: [
                    attributes: [
                        asset_details_number: 5
                    ]
                ]
            ],
        ], 400)

        then:
        response.body.message == "Element cannot contain custom aspects or links without being associated with a domain"
    }

    def "cannot create multi-domain element with custom aspect using non-domain-specific API"() {
        when:
        def response = post("/assets", [
            name: "multi-domain asset",
            owner: [
                targetUri: unitUri
            ],
            domains:[
                (dsgvoDomainId): [
                    subType: "AST_Asset",
                    status: "NEW"
                ],
                (testDomainId): [
                    subType: "ItSystem",
                    status: "NEW"
                ],
            ],
            customAspects: [
                asset_details: [
                    attributes: [
                        asset_details_number: 5
                    ]
                ]
            ],
        ], 422)

        then:
        response.body.message == "Using custom aspects or links in a multi-domain element is not supported by this API"
    }

    def "cannot create element with links and without domains"() {
        when:
        def targetPersonId = post("/persons", [
            name: "Kim",
            owner: [
                targetUri: unitUri
            ]
        ]).body.resourceId
        def response = post("/scopes", [
            name: "scope without domains",
            owner: [
                targetUri: unitUri
            ],
            links: [
                scope_informationSecurityOfficer: [
                    [
                        target: [targetUri: "$baseUrl/persons/$targetPersonId"]
                    ]
                ]
            ]
        ], 400)

        then:
        response.body.message == "Element cannot contain custom aspects or links without being associated with a domain"
    }

    def "cannot create multi-domain element with link using non-domain-specific API"() {
        when:
        def targetPersonId = post("/persons", [
            name: "Kim",
            owner: [
                targetUri: unitUri
            ]
        ]).body.resourceId
        def response = post("/scopes", [
            name: "multi-domain scope",
            owner: [
                targetUri: unitUri
            ],
            domains:[
                (dsgvoDomainId): [
                    subType: "SCP_Scope",
                    status: "NEW"
                ],
                (testDomainId): [
                    subType: "Organization",
                    status: "NEW"
                ],
            ],
            links: [
                scope_informationSecurityOfficer: [
                    [
                        target: [targetUri: "$baseUrl/persons/$targetPersonId"]
                    ]
                ]
            ]
        ], 422)

        then:
        response.body.message == "Using custom aspects or links in a multi-domain element is not supported by this API"
    }

    def "cannot associate element with domain without a sub type"() {
        when:
        def response = post("/incidents", [
            name: "incident without a sub type",
            owner: [
                targetUri: unitUri
            ],
            domains: [
                (dsgvoDomainId): [:]
            ],
        ], 400)

        then:
        response.body["domains[$dsgvoDomainId].subType"] == "must not be null"
        response.body["domains[$dsgvoDomainId].status"] == "must not be null"
    }

    def "cannot use non-existing domains"() {
        expect:
        post("/incidents", [
            name: "incident without domains",
            domains: [
                (UUID.randomUUID()): [
                    subType: "INC_Incident",
                    status: "NEW",
                ]
            ],
            owner: [
                targetUri: unitUri
            ]
        ], 422)
    }
}