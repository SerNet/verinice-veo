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
            name: "process test unit"
        ]).body.resourceId
    }

    def "CRUD a composite asset"() {
        given: "two assets"
        def partAId = post("/assets", [
            name: "part a",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
        def partBId = post("/assets", [
            name: "part b",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId

        when: "creating and retrieving a composite asset containing the two existing assets"
        def compositeId = post("/assets", [
            name: "composite",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/assets/$partAId"],
                [targetUri: "http://localhost/assets/$partBId"],
            ],
        ]).body.resourceId
        def compositeResponse = get("/assets/$compositeId")

        then: "the retrieved composite assets points to its parts"
        with(compositeResponse.body.parts.toSorted{it.displayName}*.targetUri) {
            get(0) =~ /.*\/assets\/$partAId/
            get(1) =~ /.*\/assets\/$partBId/
        }

        when: "removing a part from the composite"
        put("/assets/$compositeId", [
            name: "composite",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/assets/$partBId"],
            ]
        ], compositeResponse.parseETag())

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
        def assetPartId = post("/assets", [
            name: "asset part",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
        def assetCompositeId = post("/assets", [
            name: "asset composite",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/assets/$assetPartId"]
            ],
        ]).body.resourceId

        when: "creating & retrieving a scope that contains the composite"
        def scopeId = post("/scopes", [
            name: "scope of everything",
            owner: [targetUri: "http://localhost/units/$unitId"],
            members: [
                [targetUri: "http://localhost/assets/$assetCompositeId"]
            ]
        ]).body.resourceId
        def scopeResponse = get("/scopes/$scopeId")

        then: "the retrieved scope points to the composite"
        scopeResponse.body.members.first().targetUri =~ /.*\/assets\/$assetCompositeId/

        and: "the retrieved composite points to its part"
        get("/assets/$assetCompositeId").body.parts.first().targetUri =~ /.*\/assets\/$assetPartId/

        when: "adding another composite to the part"
        def personPartId = post("/persons", [
            name: "person part",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
        def personCompositeId = post("/persons", [
            name: "person composite",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/persons/$personPartId"]
            ],
        ]).body.resourceId
        put("/scopes/$scopeId", [
            name: "scope of everything",
            owner: [targetUri: "http://localhost/units/$unitId"],
            members: [
                [targetUri: "http://localhost/assets/$assetCompositeId"],
                [targetUri: "http://localhost/persons/$personCompositeId"],
            ]
        ], scopeResponse.parseETag())

        then: "the retrieved scope points to both composites"
        with(get("/scopes/$scopeId").body.members.toSorted{it.displayName}*.targetUri) {
            get(0) =~ /.*\/assets\/$assetCompositeId/
            get(1) =~ /.*\/persons\/$personCompositeId/
        }

        and: "the retrieved person composite points to its part"
        get("/persons/$personCompositeId").body.parts.first().targetUri =~ /.*\/persons\/$personPartId/

        when: "removing the person from the scope"
        def scopeETag = get("/scopes/$scopeId").parseETag()
        put("/scopes/$scopeId", [
            name: "scope of everything",
            owner: [targetUri: "http://localhost/units/$unitId"],
            members: [
                [targetUri: "http://localhost/assets/$assetCompositeId"]
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
        def personId = post("/persons", [
            name: "little person",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ]).body.resourceId
        def scopeId = post("/scopes", [
            name: "scope with person as member",
            owner: [targetUri: "http://localhost/units/$unitId"],
            members: [
                [targetUri: "http://localhost/persons/$personId"]
            ]
        ]).body.resourceId
        def compositePersonId = post("/persons", [
            name: "person with person as part",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/persons/$personId"]
            ]
        ]).body.resourceId

        when: "updating the person"
        def personETag = get("/persons/$personId").parseETag()
        put("/persons/$personId", [
            name: "little person in a scope and in a composite",
            owner: [targetUri: "http://localhost/units/$unitId"]
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

    def "delete unit with composite that has parts in other unit"() {
        given: "a composite in this unit with a part in another unit"
        def otherUnitId = post("/units", [
            name: "process test unit"
        ]).body.resourceId
        def partInOtherUnitId = post("/persons", [
            name: "part",
            owner: [targetUri: "http://localhost/units/$otherUnitId"]
        ]).body.resourceId
        def compositeInThisUnitId = post("/persons", [
            name: "composite",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/persons/$partInOtherUnitId"]
            ]
        ]).body.resourceId

        when: "deleting this unit"
        delete("/units/$unitId")

        then: "the composite in this unit is gone"
        get("/units/$unitId", 404)
        get("/persons/$compositeInThisUnitId", 404)
        get("/persons/$partInOtherUnitId")
    }
}
