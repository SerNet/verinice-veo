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

class ProfileRoundTripRestTest extends VeoRestTest {
    String copyOfTestDomainId
    String sourceUnitId
    String targetUnitId

    def setup() {
        copyOfTestDomainId = copyDomain(testDomainId)
        sourceUnitId = postNewUnit("source", [copyOfTestDomainId]).resourceId
        targetUnitId = postNewUnit("target").resourceId
    }

    def "create, export, import & apply profile"() {
        given: "some elements and a risk"
        def originalAssetId = post("/domains/$copyOfTestDomainId/assets", [
            name: "asset enough?",
            subType: "Information",
            status: "CURRENT",
            owner: [targetUri: "/units/$sourceUnitId"],
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        D: 1
                    ]
                ]
            ]
        ]).body.resourceId
        def originalSubControlId = post("/domains/$copyOfTestDomainId/controls", [
            name: "sub control",
            subType: "TOM",
            status: "NEW",
            owner: [targetUri: "/units/$sourceUnitId"],
        ]).body.resourceId
        def originalControlId = post("/domains/$copyOfTestDomainId/controls", [
            name: "freaky control",
            subType: "TOM",
            status: "NEW",
            parts: [
                [targetUri: "/controls/$originalSubControlId"]
            ],
            owner: [targetUri: "/units/$sourceUnitId"],
        ]).body.resourceId
        def originalPersonId = post("/domains/$copyOfTestDomainId/persons", [
            name: "poster person",
            subType: "Programmer",
            status: "REVIEWING",
            owner: [targetUri: "/units/$sourceUnitId"]
        ]).body.resourceId
        def originalProcessId = post("/domains/$copyOfTestDomainId/processes", [
            name: "process processing process",
            subType: "BusinessProcess",
            status: "NEW",
            owner: [targetUri: "/units/$sourceUnitId"],
            links: [
                necessaryData: [
                    [
                        target: [targetUri: "/assets/$originalAssetId"],
                        attributes: [
                            essential: true
                        ]
                    ]
                ]
            ],
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        D: 0
                    ]
                ]
            ]
        ]).body.resourceId
        def originalSubScenarioId = post("/domains/$copyOfTestDomainId/scenarios", [
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
        ]).body.resourceId
        post("/domains/$copyOfTestDomainId/scenarios", [
            name: "super scenario",
            subType: "Attack",
            status: "NEW",
            owner: [targetUri: "/units/$sourceUnitId"],
            parts: [
                [targetUri: "/scenarios/$originalSubScenarioId"]
            ]
        ])
        def originalScopeId = post("/domains/$copyOfTestDomainId/scopes", [
            name: "Can't cope with this scope",
            subType: "Company",
            status: "NEW",
            owner: [targetUri: "/units/$sourceUnitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$originalControlId"],
                    responsible: [targetUri: "/persons/$originalPersonId"],
                    description: "Everything is under control",
                ]
            ],
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        D: 1
                    ]
                ]
            ]
        ]).body.resourceId
        get("/scopes/$originalScopeId/requirement-implementations/$originalSubControlId").with {
            body.status = "YES"
            body.responsible = [targetUri: "/persons/$originalPersonId"]
            body.implementationStatement = "bold statement"
            body.implementationUntil = "2025-01-01"
            put(body._self, body, getETag(), 204)
        }
        post("/processes/$originalProcessId/risks", [
            scenario: [targetUri: "/scenarios/$originalSubScenarioId"],
            riskOwner: [targetUri: "/persons/$originalPersonId"],
            mitigation: [targetUri: "/controls/$originalControlId"],
            domains: [
                (copyOfTestDomainId): [
                    reference: [targetUri: "/domains/$copyOfTestDomainId"],
                    riskDefinitions: [
                        riskyDef: [
                            probability: [
                                specificProbability: 1,
                                specificProbabilityExplanation: "The risk owner is a control freak who uses freaky controls, which mitigates the likelihood of this risk."
                            ],
                            impactValues: [
                                [
                                    category: "D",
                                    specificImpact: 2,
                                    specificImpactExplanation: "Because I say so."
                                ]
                            ],
                            riskValues: [
                                [
                                    category: "D",
                                    userDefinedResidualRisk: 3,
                                    residualRiskExplanation: "It's gonna be terrible.",
                                    riskTreatments: ["RISK_TREATMENT_AVOIDANCE"],
                                ]
                            ],
                        ]
                    ]
                ]
            ]
        ])
    }

    def "change riskdefinition before roundtrip and apply profile"() {
        given:
        post("/domains/$copyOfTestDomainId/assets", [
            name: "asset enough?",
            subType: "Information",
            status: "CURRENT",
            owner: [targetUri: "/units/$sourceUnitId"],
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        D: 1
                    ]
                ]
            ]
        ])

        when: "we change the risk definition"
        def dd = copyOfTestDomainId
        get("/domains/$copyOfTestDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.categories.find { it.id == "D" }.potentialImpacts.removeLast()
            definition.categories.find { it.id == "D" }.valueMatrix.removeLast()
            put("/content-creation/domains/$dd/risk-definitions/riskyDef", definition, null, 200, CONTENT_CREATOR)
        }

        and: "perform roundtrip"
        def newDomainInOtherClientId = performRoundTrip()

        then: "the original elements have been recreated in the other client and the potentialImpact 'D' is gone"
        with(get("/domains/$newDomainInOtherClientId/assets", 200, SECONDARY_CLIENT_USER).body.items) {
            size() == 1
            get(0).name == "asset enough?"
            get(0).riskValues.riskyDef.potentialImpacts.D == null
        }
    }

    private String performRoundTrip(String dtVersion = "1.1.0") {
        // create a profile from the source unit
        post("/content-creation/domains/$copyOfTestDomainId/profiles?unit=$sourceUnitId", [
            name: "test profile"
        ]).body.id

        // create a domain template from the domain
        def templateId = post("/content-creation/domains/$copyOfTestDomainId/template", [
            version: (dtVersion)
        ], 201, CONTENT_CREATOR).body.id

        // export the template
        def exportedDomainTemplate = get("/content-creation/domain-templates/$templateId", 200, CONTENT_CREATOR).body

        // import the template under a different name"
        exportedDomainTemplate.name = "completely different domain template ${randomUUID()}"
        def newDomainTemplateId = post("/content-creation/domain-templates", exportedDomainTemplate, 201, CONTENT_CREATOR).body.resourceId

        // apply the imported profile to the target unit in another client
        post("/domain-templates/$newDomainTemplateId/createdomains?restrictToClientsWithExistingDomain=false", null, 204, ADMIN)
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
        post("/domains/$newDomainInOtherClientId/profiles/$profileInOtherClientId/incarnation?unit=$unitInOtherClientId", null, 204, SECONDARY_CLIENT_USER)
        return newDomainInOtherClientId
    }
}
