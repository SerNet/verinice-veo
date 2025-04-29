/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

class CompositeAndScopeRestTestITSpec extends VeoRestTest{
    String unitId

    def setup() {
        unitId = post("/units", [
            name: "process test unit",
            domains:[
                [ targetUri: "/domains/$testDomainId" ]
            ]
        ]).body.resourceId
    }

    def "CRUD a composite asset"() {
        given: "two assets"
        def partAId = post("/domains/$testDomainId/assets", [
            name: "part a",
            subType: 'Server',
            status: 'RUNNING',
            owner: [targetUri: "$baseUrl/units/$unitId"],
        ]).body.resourceId
        def partBId = post("/domains/$testDomainId/assets", [
            name: "part b",
            subType: 'Server',
            status: 'RUNNING',
            owner: [targetUri: "$baseUrl/units/$unitId"],
        ]).body.resourceId

        when: "creating and retrieving a composite asset containing the two existing assets"
        def compositeId = post("/domains/$testDomainId/assets", [
            name: "composite",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            subType: 'Server',
            status: 'RUNNING',
            parts: [
                [targetUri: "$baseUrl/assets/$partAId"],
                [targetUri: "$baseUrl/assets/$partBId"],
            ],
        ]).body.resourceId
        def compositeResponse = get("/assets/$compositeId")

        then: "the retrieved composite assets points to its parts"
        String urlA = "$baseUrl/assets/$partAId"
        String urlB = "$baseUrl/assets/$partBId"
        compositeResponse.body.parts*.targetUri ==~ [urlA, urlB]

        when: "removing a part from the composite"
        put("/domains/$testDomainId/assets/$compositeId", [
            name: "composite",
            subType: 'Server',
            status: 'RUNNING',
            owner: [targetUri: "$baseUrl/units/$unitId"],
            parts: [
                [targetUri: "$baseUrl/assets/$partBId"],
            ]
        ], compositeResponse.getETag())

        then: "the removed part is missing in the retrieved composite"
        get("/assets/$compositeId").body.parts*.targetUri =~ /.*\/assets\/$partBId/

        when: "deleting the composite"
        delete("/assets/$compositeId")

        then: "the composite is gone"
        get("/assets/$compositeId", 404)

        and: "the parts are still there"
        get("/assets/$partAId")
        get("/assets/$partBId")

        and:
        notThrown(Exception)
    }

    def "CRUD a scope"() {
        given: "a composite asset"
        def assetPartId = post("/domains/$testDomainId/assets", [
            name: "asset part",
            subType: 'Server',
            status: 'RUNNING',
            owner: [targetUri: "$baseUrl/units/$unitId"],
        ]).body.resourceId
        def assetCompositeId = post("/domains/$testDomainId/assets", [
            name: "asset composite",
            subType: 'Server',
            status: 'RUNNING',
            owner: [targetUri: "$baseUrl/units/$unitId"],
            parts: [
                [targetUri: "$baseUrl/assets/$assetPartId"]
            ],
        ]).body.resourceId

        when: "creating & retrieving a scope that contains the composite"
        def scopeId = post("/domains/$testDomainId/scopes", [
            name: "scope of everything",
            subType: 'Company',
            status: 'NEW',
            owner: [targetUri: "$baseUrl/units/$unitId"],
            members: [
                [targetUri: "$baseUrl/assets/$assetCompositeId"]
            ]
        ]).body.resourceId
        def scopeResponse = get("/scopes/$scopeId")

        then: "the retrieved scope points to the composite"
        scopeResponse.body.members.first().targetUri =~ /.*\/assets\/$assetCompositeId/

        and: "the retrieved composite points to its part"
        get("/assets/$assetCompositeId").body.parts.first().targetUri =~ /.*\/assets\/$assetPartId/

        when: "adding another composite to the part"
        def personPartId = post("/domains/$testDomainId/persons", [
            name: "person part",
            subType: 'Programmer',
            status: 'CODING',
            owner: [targetUri: "$baseUrl/units/$unitId"],
        ]).body.resourceId
        def personCompositeId = post("/domains/$testDomainId/persons", [
            name: "person composite",
            subType: 'Programmer',
            status: 'CODING',
            owner: [targetUri: "$baseUrl/units/$unitId"],
            parts: [
                [targetUri: "$baseUrl/persons/$personPartId"]
            ],
        ]).body.resourceId
        put("/domains/$testDomainId/scopes/$scopeId", [
            name: "scope of everything",
            subType: 'Company',
            status: 'NEW',
            owner: [targetUri: "$baseUrl/units/$unitId"],
            members: [
                [targetUri: "$baseUrl/assets/$assetCompositeId"],
                [targetUri: "$baseUrl/persons/$personCompositeId"],
            ]
        ], scopeResponse.getETag())

        then: "the retrieved scope points to both composites"
        with(get("/scopes/$scopeId").body.members.toSorted{it.displayName}*.targetUri) {
            get(0) =~ /.*\/assets\/$assetCompositeId/
            get(1) =~ /.*\/persons\/$personCompositeId/
        }

        and: "the retrieved person composite points to its part"
        get("/persons/$personCompositeId").body.parts.first().targetUri =~ /.*\/persons\/$personPartId/

        when: "removing the person from the scope"
        def scopeETag = get("/scopes/$scopeId").getETag()
        put("/domains/$testDomainId/scopes/$scopeId", [
            name: "scope of everything",
            subType: 'Company',
            status: 'NEW',
            owner: [targetUri: "$baseUrl/units/$unitId"],
            members: [
                [targetUri: "$baseUrl/assets/$assetCompositeId"]
            ]
        ], scopeETag)

        then: "the person has been removed"
        with(get("/scopes/$scopeId").body.members*.targetUri) {
            size() == 1
            get(0) =~ /.*\/assets\/$assetCompositeId/
        }

        when: "deleting the scope"
        delete("/scopes/$scopeId")

        then: "the scope is gone"
        get("/scopes/$scopeId", 404)

        and: "the composites and parts still exist"
        get("/assets/$assetCompositeId")
        get("/assets/$assetPartId")
        get("/persons/$personCompositeId")
        get("/persons/$personPartId")

        and:
        notThrown(Exception)
    }

    def "updating an element does not detach it from its scopes and composites"() {
        given: "a person in a scope and in a composite"
        def personId = post("/domains/$testDomainId/persons", [
            name: "little person",
            subType: 'Programmer',
            status: 'CODING',
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def scopeId = post("/domains/$testDomainId/scopes", [
            name: "scope with person as member",
            subType: 'Company',
            status: 'NEW',
            owner: [targetUri: "$baseUrl/units/$unitId"],
            members: [
                [targetUri: "$baseUrl/persons/$personId"]
            ]
        ]).body.resourceId
        def compositePersonId = post("/domains/$testDomainId/persons", [
            name: "person with person as part",
            subType: 'Programmer',
            status: 'CODING',
            owner: [targetUri: "$baseUrl/units/$unitId"],
            parts: [
                [targetUri: "$baseUrl/persons/$personId"]
            ]
        ]).body.resourceId

        when: "updating the person"
        def personETag = get("/persons/$personId").getETag()
        put("/domains/$testDomainId/persons/$personId", [
            name: "little person in a scope and in a composite",
            subType: 'Programmer',
            status: 'CODING',
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ], personETag)

        then: "it is still a member of the scope"
        with(get("/scopes/$scopeId").body.members*.targetUri) {
            size() == 1
            get(0) =~ /.*\/persons\/$personId/
        }

        and: "it is still a part of the composite"
        with(get("/persons/$compositePersonId").body.parts*.targetUri) {
            size() == 1
            get(0) =~ /.*\/persons\/$personId/
        }
    }

    def "simple circular structure is supported"() {
        given: "an asset that is part of itself"
        def assetId = post("/domains/$testDomainId/assets", [
            name: "I am so alone",
            subType: 'Server',
            status: 'RUNNING',

            owner: [targetUri: "/units/$unitId"],
        ]).body.resourceId
        get("/domains/$testDomainId/assets/$assetId").with{
            body.parts = [
                [targetUri: "/assets/$assetId"]
            ]
            put(body._self, body, getETag())
        }

        expect: "that it can be updated"
        get("/domains/$testDomainId/assets/$assetId").with{
            body.name = "I am my own best friend"
            put(body._self, body, getETag())
        }

        and: "and retrieved"
        with(get("/domains/$testDomainId/assets/$assetId").body) {
            name == "I am my own best friend"
            parts.size() == 1
            parts[0].targetUri.endsWith"/assets/$assetId"
            parts[0].displayName.endsWith("I am my own best friend")
        }
    }

    def "adding a part from another unit is forbidden"() {
        given: "assets in two different units"
        def mainUnitAssetId = post("/domains/$testDomainId/assets", [
            name: "asset in main unit",
            subType: 'Server',
            status: 'RUNNING',
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId
        def otherUnitId = postNewUnit().resourceId
        def otherUnitAssetId = post("/domains/$testDomainId/assets", [
            name: "asset in other unit",
            subType: 'Server',
            status: 'RUNNING',
            owner: [targetUri: "/units/$otherUnitId"],
        ]).body.resourceId

        expect: "that the units cannot be part of the same hierarchy"
        get("/domains/$testDomainId/assets/$mainUnitAssetId").with{
            body.parts = [
                [targetUri: "/assets/$otherUnitAssetId"]
            ]
            put(body._self, body, getETag(), 422)
        }.body.message == "Elements in other units must not be referenced"
    }

    def "adding a member from another unit is forbidden"() {
        given: "scopes in two different units"
        def mainUnitScopeId = post("/domains/$testDomainId/scopes", [
            name: "scope in main unit",
            subType: 'Company',
            status: 'NEW',
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId
        def otherUnitId = postNewUnit().resourceId
        def otherUnitScopeId = post("/domains/$testDomainId/scopes", [
            name: "scope in other unit",
            subType: 'Company',
            status: 'NEW',
            owner: [targetUri: "/units/$otherUnitId"],
        ]).body.resourceId

        expect: "that the units cannot be part of the same hierarchy"
        get("/domains/$testDomainId/scopes/$mainUnitScopeId").with{
            body.members = [
                [targetUri: "/scopes/$otherUnitScopeId"]
            ]
            put(body._self, body, getETag(), 422)
        }.body.message == "Elements in other units must not be referenced"
    }
}
