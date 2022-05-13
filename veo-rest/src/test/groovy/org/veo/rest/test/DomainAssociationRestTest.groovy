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

    // TODO VEO-1413 expect this to fail
    def "can create element without domains"() {
        expect:
        post("/incidents", [
            name: "incident with domains",
            owner: [
                targetUri: unitUri
            ]
        ])
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