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

class ProcessRiskRestTestITSpec extends VeoRestTest{

    String unitId
    String domainId

    def setup() {
        unitId = post("/units", [name: "process risk rest test unit"]).body.resourceId
        domainId = get("/domains").body[0].id
    }

    def "create and update a process risk"() {
        given: "a composite process and a scenario"
        def subProcessId = post("/processes", [
            name: "sub process",
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId
        def processId = post("/processes", [
            domains: [
                (domainId): [:]
            ],
            parts: [
                // The part is not relevant for the risk, it just spices things up.
                [targetUri: "/processes/$subProcessId"]
            ],
            name: "risk test process",
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId
        def scenarioId = post("/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId

        when: "creating the risk"
        post("/processes/$processId/risks", [
            domains: [
                [targetUri: "/domains/$domainId"]
            ],
            scenario: [targetUri: "/scenarios/$scenarioId"]
        ])

        then: "it can be retrieved"
        def retrievedRiskResponse = get("/processes/$processId/risks/$scenarioId")
        def risk = retrievedRiskResponse.body
        risk.scenario.targetUri ==~ /.*\/scenarios\/$scenarioId/

        when: "assigning a risk owner"
        def ownerPersonId = post("/persons", [
            name: "process risk owner",
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId
        risk.riskOwner = [targetUri: "/persons/$ownerPersonId"]
        put("/processes/$processId/risks/$scenarioId", risk, retrievedRiskResponse.headers["ETag"].toString())

        then: "the risk has an owner"
        get("/processes/$processId/risks/$scenarioId").body.riskOwner.targetUri ==~ /.*\/persons\/$ownerPersonId/
    }
}
