/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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
package org.veo.rest

import org.veo.core.VeoMvcSpec

class ContentSpec extends VeoMvcSpec {
    public static final String PROBLEM = "In computer science, there are only three hard problems: " +
    "Cache invalidation, naming things, and off-by-one errors."

    protected createUnitWithElements(String domainId, boolean addRisks = false, boolean addRiskValues = false) {
        def unitId = parseJson(post("/units", [
            name   : "you knit",
            domains: [
                [targetUri: "http://localhost/domains/$domainId"]
            ]
        ])).resourceId
        def owner = [targetUri: "http://localhost/units/$unitId"]

        def assetId = parseJson(post("/assets", [
            domains: [
                (domainId): [
                    subType: "AST_Application",
                    status: "NEW",
                    riskValues: addRiskValues ? [
                        DSRA: [
                            potentialImpacts: [
                                "C": 0,
                                "I": 0
                            ]
                        ]
                    ] : [:]
                ]
            ],
            name   : "asset",
            owner  : owner
        ])).resourceId
        post("/controls", [
            name   : "control",
            domains: [
                (domainId): [
                    subType: "CTL_TOM",
                    status: "NEW",
                ]
            ],
            owner  : owner
        ])
        post("/documents", [
            name   : "document",
            domains: [
                (domainId): [
                    subType: "DOC_Document",
                    status: "NEW",
                ]
            ],
            owner  : owner
        ])
        post("/incidents", [
            name   : "incident",
            domains: [
                (domainId): [
                    subType: "INC_Incident",
                    status: "NEW",
                ]
            ],
            owner  : owner
        ])
        post("/persons", [
            name   : "person",
            domains: [
                (domainId): [
                    subType: "PER_Person",
                    status: "NEW",
                ]
            ],
            owner  : owner
        ])
        def processId = parseJson(post("/processes", [
            domains: [
                (domainId): [
                    subType: "PRO_DataProcessing",
                    status: "NEW",
                    riskValues: addRiskValues ? [
                        DSRA: [
                            potentialImpacts: [
                                "C": 0,
                                "I": 1
                            ]
                        ]
                    ] : [:]
                ]
            ],
            name   : "process",
            owner  : owner
        ])).resourceId
        def scenarioId = parseJson(post("/scenarios", [
            name   : "scenario",
            domains: [
                (domainId): [
                    subType: "SCN_Scenario",
                    status: "NEW",
                    riskValues: addRiskValues ? [
                        DSRA: [
                            potentialProbability: 2
                        ]
                    ] : [:]
                ]
            ],
            owner  : owner
        ])).resourceId
        post("/scopes", [
            name   : "scope",
            domains: [
                (domainId): [
                    subType: "SCP_Scope",
                    status: "NEW",
                    riskDefinition: addRiskValues ? "DSRA": null,
                ]
            ],
            owner  : owner
        ])

        if (addRisks) {
            post("/assets/$assetId/risks", [
                domains: [
                    (domainId): [
                        reference: [targetUri: "http://localhost/domains/$domainId"]
                    ]
                ],
                scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
            ])
            post("/processes/$processId/risks", [
                domains: [
                    (domainId): [
                        reference: [targetUri: "http://localhost/domains/$domainId"],
                        riskDefinitions: addRiskValues ? [
                            DSRA: [
                                impactValues: [
                                    [
                                        category: "A",
                                        specificImpact: 1
                                    ]
                                ],
                                riskValues: [
                                    [
                                        category: "A",
                                        residualRiskExplanation: PROBLEM,
                                        riskTreatments: ["RISK_TREATMENT_REDUCTION"]
                                    ]
                                ]
                            ]
                        ] : [:]
                    ]
                ],
                scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
            ])
        }
        [
            unitId,
            assetId,
            scenarioId,
            processId
        ]
    }
}
