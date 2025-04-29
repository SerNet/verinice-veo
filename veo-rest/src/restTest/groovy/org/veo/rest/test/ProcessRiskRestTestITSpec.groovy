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

import org.apache.http.HttpStatus

class ProcessRiskRestTestITSpec extends VeoRestTest{

    String unitId

    def setup() {
        unitId = postNewUnit().resourceId
    }

    def "create and update a process risk"() {
        given: "a process and a scenario"
        def processId = post("/domains/$dsgvoDomainId/processes", [
            subType: "PRO_DataTransfer",
            status: "NEW",
            name: "risk test process",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def scenarioId = post("/domains/$dsgvoDomainId/scenarios", [
            name: "process risk test scenario",
            subType: 'SCN_Scenario',
            status: 'NEW',
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId

        when: "creating the risk"
        post("/processes/$processId/risks", [
            domains: [
                (dsgvoDomainId) : [
                    reference: [targetUri: "$baseUrl/domains/$dsgvoDomainId"]
                ]
            ],
            scenario: [targetUri: "$baseUrl/scenarios/$scenarioId"]
        ])

        then: "it can be retrieved"
        def retrievedRiskResponse = get("/processes/$processId/risks/$scenarioId")
        def risk = retrievedRiskResponse.body
        risk.scenario.targetUri ==~ /.*\/scenarios\/$scenarioId/

        when: "assigning a risk owner"
        def ownerPersonId = post("/domains/$dsgvoDomainId/persons", [
            name: "process risk owner",
            subType: 'PER_Person',
            status: 'NEW',
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        risk.riskOwner = [targetUri: "$baseUrl/persons/$ownerPersonId"]
        put("/processes/$processId/risks/$scenarioId", risk, retrievedRiskResponse.getETag())

        then: "the risk has an owner"
        get("/processes/$processId/risks/$scenarioId").body.riskOwner.targetUri ==~ /.*\/persons\/$ownerPersonId/

        when: "assigning the process to another domain"
        post("/domains/$testDomainId/processes/$processId", [
            subType: "BusinessProcess",
            status: "NEW",
        ], 200)

        then: "the risk is also assigned to both domains"
        with(get("/processes/$processId/risks/$scenarioId").body) {
            domains[owner.dsgvoDomainId] != null
            domains[owner.testDomainId] != null
        }
    }

    def "create a process risk with an invalid domain reference"() {
        given: "a process and a scenario"
        def processId = post("/domains/$dsgvoDomainId/processes", [
            subType: "PRO_DataTransfer",
            status: "NEW",
            name: "risk test process-1",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def scenarioId = post("/domains/$dsgvoDomainId/scenarios", [
            name: "process risk test scenario-1",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def invalidDomainId = UUID.randomUUID().toString()

        when: "trying to create a risk with an invalid domain"
        def error = post("/processes/$processId/risks", [
            domains: [
                (invalidDomainId) :  [
                    reference: [targetUri: "$baseUrl/domains/$invalidDomainId"]
                ]
            ],
            scenario: [targetUri: "$baseUrl/scenarios/$scenarioId"]
        ], HttpStatus.SC_UNPROCESSABLE_ENTITY)

        then: "the error is present"
        error.body.message ==~ /Unable to resolve all domain references/
    }

    def "create and update a process without domain ref"() {
        given: "a process and a scenario"
        def processId = post("/domains/$dsgvoDomainId/processes", [
            subType: "PRO_DataTransfer",
            status: "NEW",
            name: "risk test process-1",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def scenarioId = post("/domains/$dsgvoDomainId/scenarios", [
            name: "process risk test scenario-1",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId

        when: "creating the risk"
        def error = post("/processes/$processId/risks", [
            domains: [
                (dsgvoDomainId) : [:]
            ],
            scenario: [targetUri: "$baseUrl/scenarios/$scenarioId"]
        ], 400)

        then: "the error is present"
        error.body.values() ==~ ['domain reference is missing']
    }
}
