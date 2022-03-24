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

class SearchRestTest extends VeoRestTest{
    String unitId

    def setup() {
        unitId = post("/units", [
            name: "search test unit"
        ]).body.resourceId
    }

    def "can filter elements in scope & composite hierarchies"() {
        given: "a hierarchy of persons"
        def developerId = post("/persons", [
            name: "developer",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def designingDeveloperId = post("/persons", [
            name: "developer who is also a designer",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def salesPersonId = post("/persons", [
            name: "sales person",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId

        def developmentTeamId = post("/persons", [
            name: "development team",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            parts: [
                [targetUri: "$baseUrl/persons/$developerId"],
                [targetUri: "$baseUrl/persons/$designingDeveloperId"],
            ]
        ]).body.resourceId
        def designTeamId = post("/persons", [
            name: "design team",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            parts: [
                [targetUri: "$baseUrl/persons/$designingDeveloperId"],
            ]
        ]).body.resourceId
        def salesTeamId = post("/persons", [
            name: "sales team",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            parts: [
                [targetUri: "$baseUrl/persons/$salesPersonId"],
            ]
        ]).body.resourceId

        and: "a related hierarchy of scopes"
        def salesScopeId = post("/scopes", [
            name: "sales scope",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            members: [
                [targetUri: "$baseUrl/persons/$salesTeamId"],
            ]
        ]).body.resourceId
        def emptyScopeId = post("/scopes", [
            name: "empty scope",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            members: []
        ]).body.resourceId
        post("/scopes", [
            name: "super scope",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            members: [
                [targetUri: "$baseUrl/scopes/$salesScopeId"],
                [targetUri: "$baseUrl/scopes/$emptyScopeId"],
            ]
        ])
        post("/scopes", [
            name: "scope containing empty scope & design team",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            members: [
                [targetUri: "$baseUrl/scopes/$emptyScopeId"],
                [targetUri: "$baseUrl/persons/$designTeamId"],
            ]
        ])

        expect: "person filters to be applied correctly"
        get("/persons?unit=$unitId&childElementIds=$designingDeveloperId").body.items*.name =~ [
            "development team",
            "design team",
        ]
        get("/persons?unit=$unitId&childElementIds=$developerId,$salesPersonId").body.items*.name =~ [
            "development team",
            "sales team",
        ]
        get("/persons?unit=$unitId&hasChildElements=true").body.items*.name =~ [
            "development team",
            "design team",
            "sales team",
        ]
        get("/persons?unit=$unitId&hasChildElements=false").body.items*.name =~ [
            "developer",
            "developer who is also a designer",
            "sales person",
        ]
        get("/persons?unit=$unitId&hasParentElements=true").body.items*.name =~ [
            "developer",
            "design team",
            "developer who is also a designer",
            "sales person",
            "sales team",
        ]
        get("/persons?unit=$unitId&hasParentElements=false").body.items*.name =~ [
            "development team"
        ]

        and: "scope filters to be applied correctly"
        get("/scopes?unit=$unitId&childElementIds=$emptyScopeId").body.items*.name =~ [
            "super scope",
            "scope containing empty scope & design team",
        ]
        get("/scopes?unit=$unitId&childElementIds=$designTeamId,$salesTeamId").body.items*.name =~ [
            "scope containing empty scope & design team",
            "sales scope",
        ]
        get("/scopes?unit=$unitId&hasChildElements=true").body.items*.name =~ [
            "sales scope",
            "super scope",
            "scope containing empty scope & design team",
        ]
        get("/scopes?unit=$unitId&hasChildElements=false").body.items*.name =~ [
            "empty scope",
        ]
        get("/scopes?unit=$unitId&hasParentElements=true").body.items*.name =~ [
            "sales scope",
            "empty scope",
        ]
        get("/scopes?unit=$unitId&hasParentElements=false").body.items*.name =~ [
            "super scope",
            "scope containing empty scope & design team",
        ]
    }
}
