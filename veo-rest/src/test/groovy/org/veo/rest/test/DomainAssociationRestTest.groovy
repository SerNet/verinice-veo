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

    def setup() {
        unitUri = "$baseUrl/units/" + postNewUnit("some unit").resourceId
        dsgvoDomainId = domains.find { it.name == "DS-GVO" }.id
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
    }

    // TODO VEO-661 remove (it will be impossible to specify custom aspects without a domain association in the new DTO structure)
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

    // TODO VEO-661 remove (it will be impossible to specify links without a domain association in the new DTO structure)
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

    def "cannot use non-existing domains"() {
        expect:
        post("/incidents", [
            name: "incident without domains",
            domains: [
                (UUID.randomUUID()): [:]
            ],
            owner: [
                targetUri: unitUri
            ]
        ], 404)
    }
}