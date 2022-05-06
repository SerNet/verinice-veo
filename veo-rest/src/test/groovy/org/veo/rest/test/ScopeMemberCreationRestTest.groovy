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

class ScopeMemberCreationRestTest extends VeoRestTest {
    String unitUri

    def setup() {
        unitUri = "$baseUrl/units/" + post("/units", [name: "decision rest test unit"]).body.resourceId
    }

    def "create elements as scope members"() {
        given: "two scopes"
        def scope1Id = post("/scopes", [
            name: "scope 1",
            owner: [targetUri: unitUri],
        ]).body.resourceId
        def scope1CreationTime = get("/scopes/$scope1Id").body.createdAt

        def scope2Id = post("/scopes", [
            name: "scope 2",
            owner: [targetUri: unitUri],
        ]).body.resourceId
        def scope2CreationTime = get("/scopes/$scope2Id").body.createdAt

        when: "creating various elements as members of both scopes"
        def assetId = post("/assets?scopes=$scope1Id,$scope2Id", [
            name: "asset in two scopes",
            owner: [targetUri: unitUri],
        ]).body.resourceId
        def controlId = post("/controls?scopes=$scope1Id,$scope2Id", [
            name: "control in two scopes",
            owner: [targetUri: unitUri],
        ]).body.resourceId
        def documentId = post("/documents?scopes=$scope1Id,$scope2Id", [
            name: "document in two scopes",
            owner: [targetUri: unitUri],
        ]).body.resourceId
        def incidentId = post("/incidents?scopes=$scope1Id,$scope2Id", [
            name: "incident in two scopes",
            owner: [targetUri: unitUri],
        ]).body.resourceId
        def personId = post("/persons?scopes=$scope1Id,$scope2Id", [
            name: "person in two scopes",
            owner: [targetUri: unitUri],
        ]).body.resourceId
        def processId = post("/processes?scopes=$scope1Id,$scope2Id", [
            name: "process in two scopes",
            owner: [targetUri: unitUri],
        ]).body.resourceId
        def scenarioId = post("/scenarios?scopes=$scope1Id,$scope2Id", [
            name: "scenario in two scopes",
            owner: [targetUri: unitUri],
        ]).body.resourceId
        def subScopeId = post("/scopes?scopes=$scope1Id,$scope2Id", [
            name: "scope in two scopes",
            owner: [targetUri: unitUri],
        ]).body.resourceId

        then: "the scopes contain all members"
        with(get("/scopes/$scope1Id").body) {
            updatedAt > scope1CreationTime
            members*.targetUri =~ [
                "$owner.baseUrl/assets/$assetId",
                "$owner.baseUrl/controls/$controlId",
                "$owner.baseUrl/documents/$documentId",
                "$owner.baseUrl/incidents/$incidentId",
                "$owner.baseUrl/persons/$personId",
                "$owner.baseUrl/processes/$processId",
                "$owner.baseUrl/scenarios/$scenarioId",
                "$owner.baseUrl/scopes/$subScopeId",
            ]
        }
        with(get("/scopes/$scope2Id").body) {
            updatedAt > scope2CreationTime
            members*.targetUri =~ [
                "$owner.baseUrl/assets/$assetId",
                "$owner.baseUrl/controls/$controlId",
                "$owner.baseUrl/documents/$documentId",
                "$owner.baseUrl/incidents/$incidentId",
                "$owner.baseUrl/persons/$personId",
                "$owner.baseUrl/processes/$processId",
                "$owner.baseUrl/scenarios/$scenarioId",
                "$owner.baseUrl/scopes/$subScopeId",
            ]
        }
    }
}
