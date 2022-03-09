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
    String domainId

    def setup() {
        unitId = post("/units", [name: "asset risk rest test unit"]).body.resourceId
        domainId = get("/domains").body[0].id
    }

    def "create and update an asset risk"() {
        given: "a composite asset and a scenario"
        def subAssetId = post("/assets", [
            name: "sub asset",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ]).body.resourceId
        def assetId = post("/assets", [
            domains: [
                (domainId): [:]
            ],
            parts: [
                // The part is not relevant for the risk, it just spices things up.
                [targetUri: "http://localhost/assets/$subAssetId"]
            ],
            name: "risk test asset",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ]).body.resourceId
        def scenarioId = post("/scenarios", [
            name: "asset risk test scenario",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ]).body.resourceId

        when: "creating the risk"
        post("/assets/$assetId/risks", [
            domains: [
                (domainId): [
                    reference: [targetUri: "http://localhost/domains/$domainId"]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ])

        then: "it can be retrieved"
        def retrievedRiskResponse = get("/assets/$assetId/risks/$scenarioId")
        def risk = retrievedRiskResponse.body
        risk.scenario.targetUri ==~ /.*\/scenarios\/$scenarioId/

        when: "assigning a risk owner"
        def ownerPersonId = post("/persons", [
            name: "asset risk owner",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ]).body.resourceId
        risk.riskOwner = [targetUri: "http://localhost/persons/$ownerPersonId"]
        put("/assets/$assetId/risks/$scenarioId", risk, retrievedRiskResponse.parseETag())

        then: "the risk has an owner"
        get("/assets/$assetId/risks/$scenarioId").body.riskOwner.targetUri ==~ /.*\/persons\/$ownerPersonId/
    }
}
