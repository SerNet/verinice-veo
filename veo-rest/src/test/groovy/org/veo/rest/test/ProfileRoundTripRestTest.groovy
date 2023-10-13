/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import static java.util.UUID.randomUUID
import static org.veo.rest.test.UserType.ADMIN
import static org.veo.rest.test.UserType.CONTENT_CREATOR
import static org.veo.rest.test.UserType.SECONDARY_CLIENT_USER

class ProfileRoundTripRestTest extends VeoRestTest{
    String copyOfTestDomainId
    String sourceUnitId
    String targetUnitId

    def setup() {
        copyOfTestDomainId = copyDomain(testDomainId)
        sourceUnitId = postNewUnit("source").resourceId
        targetUnitId = postNewUnit("target").resourceId
    }

    def "create, export, import & apply profile"() {
        given: "some elements"
        post("/domains/$copyOfTestDomainId/assets", [
            name: "asset enough?",
            subType: "Information",
            status: "CURRENT",
            owner: [targetUri: "/units/$sourceUnitId"],
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        C: 1
                    ]
                ]
            ]
        ])
        post("/domains/$copyOfTestDomainId/controls", [
            name: "freaky control",
            subType: "TOM",
            status: "NEW",
            owner: [targetUri: "/units/$sourceUnitId"],
            riskValues: [
                riskyDef: [
                    implementationStatus: 1
                ]
            ]
        ])
        post("/domains/$copyOfTestDomainId/processes", [
            name: "process processing process",
            subType: "BusinessProcess",
            status: "NEW",
            owner: [targetUri: "/units/$sourceUnitId"],
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        C: 0
                    ]
                ]
            ]
        ])
        post("/domains/$copyOfTestDomainId/scenarios", [
            name: "scenic scenario",
            subType: "Attack",
            status: "NEW",
            owner: [targetUri: "/units/$sourceUnitId"],
            riskValues: [
                riskyDef: [
                    potentialProbability: 2,
                    potentialProbabilityExplanation: "It's happened before",
                ]
            ]
        ])
        post("/domains/$copyOfTestDomainId/scopes", [
            name: "Can't cope with this scope",
            subType: "Company",
            status: "NEW",
            owner: [targetUri: "/units/$sourceUnitId"],
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        C: 1
                    ]
                ]
            ]
        ])

        when: "creating a profile from the unit"
        post("/content-creation/domains/$copyOfTestDomainId/profiles?unit=$sourceUnitId", [
            name: "test profile"
        ]).body.id

        and: "creating a domain template from the domain"
        def templateId = post("/content-creation/domains/$copyOfTestDomainId/template", [
            version: "99.99.99"
        ], 201, CONTENT_CREATOR).body.id

        and: "exporting the template"
        def exportedDomainTemplate = get("/content-creation/domain-templates/$templateId", 200, CONTENT_CREATOR).body

        then: "the export contains the right amount of items"
        exportedDomainTemplate.profiles_v2.first().items.size() == 5

        when: "importing the template under a different name"
        exportedDomainTemplate.name = "completely different domain template ${randomUUID()}"
        def newDomainTemplateId = post("/content-creation/domain-templates", exportedDomainTemplate, 201, CONTENT_CREATOR).body.resourceId

        and: "applying the imported profile in another client"
        post("/domain-templates/$newDomainTemplateId/createdomains", null, 204, ADMIN)
        def newDomainInOtherClientId = get("/domains", 200, SECONDARY_CLIENT_USER).body.find { it.name == exportedDomainTemplate.name }.id
        def profileInOtherClientId = get("/domains/$newDomainInOtherClientId/profiles", 200, SECONDARY_CLIENT_USER).body.find {
            it.name == "test profile"
        }.id
        def unitInOtherClientId = post("/units", [
            name: "profile target unit",
            domains: [
                [targetUri: "/domains/$newDomainInOtherClientId"]
            ]
        ], 201, SECONDARY_CLIENT_USER).body.resourceId
        post("/domains/$newDomainInOtherClientId/profilesnew/$profileInOtherClientId/units/$unitInOtherClientId", null, 204, SECONDARY_CLIENT_USER)

        then: "the original elements have been recreated in the other client"
        with(get("/domains/$newDomainInOtherClientId/assets", 200, SECONDARY_CLIENT_USER).body.items) {
            size() == 1
            get(0).name == "asset enough?"
            get(0).riskValues.riskyDef.potentialImpacts.C == 1
        }
        with(get("/domains/$newDomainInOtherClientId/controls", 200, SECONDARY_CLIENT_USER).body.items) {
            size() == 1
            get(0).name == "freaky control"
            get(0).riskValues.riskyDef.implementationStatus == 1
        }
        with(get("/domains/$newDomainInOtherClientId/processes", 200, SECONDARY_CLIENT_USER).body.items) {
            size() == 1
            get(0).name == "process processing process"
            get(0).riskValues.riskyDef.potentialImpacts.C == 0
        }
        with(get("/domains/$newDomainInOtherClientId/scenarios", 200, SECONDARY_CLIENT_USER).body.items) {
            size() == 1
            get(0).name == "scenic scenario"
            get(0).riskValues.riskyDef.potentialProbability == 2
            get(0).riskValues.riskyDef.potentialProbabilityExplanation == "It's happened before"
        }
        with(get("/domains/$newDomainInOtherClientId/scopes", 200, SECONDARY_CLIENT_USER).body.items) {
            size() == 1
            get(0).name == "Can't cope with this scope"
            get(0).riskValues.riskyDef.potentialImpacts.C == 1
        }
    }
}
