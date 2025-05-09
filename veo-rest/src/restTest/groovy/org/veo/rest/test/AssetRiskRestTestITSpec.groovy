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

class AssetRiskRestTestITSpec extends VeoRestTest{

    String unitId

    def setup() {
        unitId = postNewUnit().resourceId
    }

    def "create and update an asset risk"() {
        given: "a composite asset and a scenario"
        def subAssetId = post("/domains/$dsgvoDomainId/assets", [
            name: "sub asset",
            subType: "AST_Datatype",
            status: "IN_PROGRESS",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def assetId = post("/domains/$dsgvoDomainId/assets", [
            subType: "AST_Datatype",
            status: "IN_PROGRESS",
            riskValues: [
                DSRA : [
                    potentialImpacts: [
                        "C": 0,
                        "I": 1
                    ]
                ]
            ],
            parts: [
                // The part is not relevant for the risk, it just spices things up.
                [targetUri: "$baseUrl/assets/$subAssetId"]
            ],
            name: "risk test asset",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def scenarioId = post("/domains/$dsgvoDomainId/scenarios", [
            name: "asset risk test scenario",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId

        when: "creating the risk"
        post("/assets/$assetId/risks", [
            domains: [
                (dsgvoDomainId): [
                    reference: [targetUri: "$baseUrl/domains/$dsgvoDomainId"]
                ]
            ],
            scenario: [targetUri: "$baseUrl/scenarios/$scenarioId"]
        ])

        then: "it can be retrieved"
        def retrievedRiskResponse = get("/assets/$assetId/risks/$scenarioId")
        def risk = retrievedRiskResponse.body
        risk.scenario.targetUri ==~ /.*\/scenarios\/$scenarioId/

        when: "assigning a risk owner"
        def ownerPersonId = post("/domains/$dsgvoDomainId/persons", [
            name: "asset risk owner",
            subType: 'PER_Person',
            status: 'NEW',
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        risk.riskOwner = [targetUri: "$baseUrl/persons/$ownerPersonId"]
        put("/assets/$assetId/risks/$scenarioId", risk, retrievedRiskResponse.getETag())

        then: "the risk has an owner"
        get("/assets/$assetId/risks/$scenarioId").body.riskOwner.targetUri ==~ /.*\/persons\/$ownerPersonId/
    }

    def "cannot add risk for scenario in another unit"() {
        given: "an asset and a scenario in different units"
        def assetId = post("/domains/$testDomainId/assets", [
            name: "asset in main unit",
            subType: "Server",
            status: "RUNNING",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def otherUnitId = postNewUnit().resourceId
        def scenarioId = post("/domains/$testDomainId/scenarios", [
            name: "scenario in other unit",
            subType: "Attack",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$otherUnitId"]
        ]).body.resourceId

        expect: "risk creation to fail"
        post("/assets/$assetId/risks", [
            domains: [
                (testDomainId): [
                    reference: [targetUri: "$baseUrl/domains/$testDomainId"]
                ]
            ],
            scenario: [targetUri: "$baseUrl/scenarios/$scenarioId"]
        ], 422).body.message == "Elements in other units must not be referenced"
    }

    def "cannot assign risk owner or mitigation from another unit"() {
        given: "a risk"
        def assetId = post("/domains/$testDomainId/assets", [
            name: "asset in main unit",
            subType: "Server",
            status: "RUNNING",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def scenarioId = post("/domains/$testDomainId/scenarios", [
            name: "scenario in main unit",
            subType: "Attack",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        post("/assets/$assetId/risks", [
            domains: [
                (testDomainId): [
                    reference: [targetUri: "$baseUrl/domains/$testDomainId"]
                ]
            ],
            scenario: [targetUri: "$baseUrl/scenarios/$scenarioId"]
        ])

        when: "a person is created in another unit"
        def otherUnitId = postNewUnit().resourceId
        def personId = post("/domains/$testDomainId/persons", [
            name: "person in another unit",
            subType: "MasterOfDisaster",
            status: "WATCHING_DISASTER_MOVIES",
            owner: [targetUri: "$baseUrl/units/$otherUnitId"]
        ]).body.resourceId

        then: "it cannot be assigned as a risk owner"
        get("/assets/$assetId/risks/$scenarioId").with{
            body.riskOwner = [targetUri: "/persons/$personId"]
            put(body._self, body, getETag(), 422)
        }.body.message == "Elements in other units must not be referenced"

        when: "a control is created in another unit"
        def controlId = post("/domains/$testDomainId/controls", [
            name: "control in another unit",
            subType: "TOM",
            status: "OLD",
            owner: [targetUri: "$baseUrl/units/$otherUnitId"]
        ]).body.resourceId

        then: "it cannot be used to mitigate the risk"
        get("/assets/$assetId/risks/$scenarioId").with{
            body.mitigation = [targetUri: "/controls/$controlId"]
            put(body._self, body, getETag(), 422)
        }.body.message == "Elements in other units must not be referenced"
    }
}
