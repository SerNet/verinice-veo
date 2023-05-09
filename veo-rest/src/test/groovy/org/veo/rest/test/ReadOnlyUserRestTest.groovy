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
package org.veo.rest.test

import org.springframework.core.env.Profiles
import org.springframework.test.context.ActiveProfiles

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j

@Slf4j
class ReadOnlyUserRestTest extends VeoRestTest {

    String unitId
    String unitUri
    String domainId
    String processId
    String eTag

    def setup() {
        unitId = post("/units", [name: "rest test unit"]).body.resourceId
        unitUri = "$baseUrl/units/$unitId"
        domainId = get("/domains").body.find{it.name == "DS-GVO"}.id

        processId = post("/processes", [
            name: "data processing",
            domains: [
                (domainId): [
                    subType: "PRO_DataProcessing",
                    status: "NEW"
                ],
            ],
            owner: [targetUri: unitUri]
        ]).body.resourceId
        eTag = get("/processes/$processId").getETag()
    }

    def "user without write permission may GET a process"() {
        expect:
        get("/processes/$processId", 200, UserType.READ_ONLY)
    }

    def "user without write access may not POST a process"() {
        expect:
        post("/processes", [
            name   : "can't create this",
            domains: [
                (domainId): [
                    subType: "PRO_DataProcessing",
                    status : "NEW"
                ],
            ],
            owner  : [targetUri: unitUri]
        ], 403, UserType.READ_ONLY)
    }

    def "user without write access may not PUT a process"() {
        expect:
        put("/processes/$processId", [
            name   : "can't touch this",
            domains: [
                (domainId): [
                    subType: "PRO_DataProcessing",
                    status : "NEW"
                ],
            ],
            owner  : [targetUri: unitUri]
        ], eTag, 403, UserType.READ_ONLY)
    }

    def "user without write access may not POST element type definitions"() {
        expect:
        post("/domains/$domainId/elementtypedefinitions/scope/updatefromobjectschema", [
            name: "cantupdatefromobjectschemathiselementtypedefinition"
        ], 403, UserType.READ_ONLY)
    }

    def "user without write access may not DELETE a process"() {
        expect:
        delete("/processes/$processId",
                403, UserType.READ_ONLY)
    }

    def "user without write access may POST searches"() {
        expect:
        post("/processes/searches", [
            status: [
                values: ["NEW"]
            ]
        ], 201, UserType.READ_ONLY)
    }

    def "user without write access may POST evaluations"() {
        expect:
        // TODO VEO-1987 remove legacy endpoint call
        post("/processes/evaluation?domain=$domainId", [
            name: "you can evaluate this",
            domains: [
                (domainId): [
                    subType: "PRO_DataProcessing",
                    status: "NEW"
                ],
            ]
        ], 400, UserType.READ_ONLY)
        post("/domains/$domainId/processes/evaluation", [
            name: "you can evaluate this",
            domains: [
                (domainId): [
                    subType: "PRO_DataProcessing",
                    status: "NEW"
                ],
            ]
        ], 400, UserType.READ_ONLY)
    }
}
