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

class DecisionRestTest extends VeoRestTest {
    def PIA = "process_privacyImpactAssessment"
    String unitUri
    String domainId
    def decisions

    def setup() {
        unitUri = "http://localhost/units/" + post("/units", [name: "decision rest test unit"]).body.resourceId

        def domain = get("/domains").body.find { it.name == "DS-GVO" }
        domainId = domain.id
        decisions = domain.decisions
    }

    def "piaRequired decision is made"() {
        given:
        def decision = decisions.piaMandatory

        when: "creating a blank data processing"
        def processId = post("/processes", [
            name: "blank process",
            domains: [
                (domainId): [
                    subType: "PRO_DataProcessing",
                    status: "NEW"
                ],
            ],
            owner: [targetUri: unitUri]
        ]).body.resourceId

        then:
        with(get("/processes/$processId").body.domains[domainId].decisionResults.piaMandatory) {
            value == null
            decision.rules[decisiveRule].description.en == "Risk analysis not carried out"
            matchingRules == [decisiveRule]
            agreeingRules == [decisiveRule]
        }

        when: "adding a risk"
        post("/scopes", [
            name: "risky scope",
            domains: [
                (domainId): [
                    riskDefinition: "DSRA"
                ]
            ],
            owner: [targetUri: unitUri],
            members: [
                [targetUri: "http://localhost/processes/$processId"]
            ],
        ])
        def scenarioId = post("/scenarios", [
            name: "danger",
            owner: [targetUri: unitUri],
        ]).body.resourceId
        post("/processes/$processId/risks", [
            domains: [
                (domainId): [
                    reference: [targetUri: "http://localhost/domains/$domainId"],
                    riskDefinitions: [
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
                                    residualRisk: 1
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ])

        // TODO VEO-1282 assert that the decision has already changed based on the added risk

        and: "adding PIA related attributes"
        put("/processes/$processId", [
            name: "blank process",
            customAspects: [
                (PIA): [
                    attributes: [
                        ("${PIA}_listed"): "${PIA}_listed_negative",
                        ("${PIA}_processingCriteria"): [
                            "${PIA}_processingCriteria_automated",
                            "${PIA}_processingCriteria_specialCategories",
                        ],
                    ]
                ]
            ],
            domains: [
                (domainId): [
                    subType: "PRO_DataProcessing",
                    status: "NEW"
                ],
            ],
            owner: [targetUri: unitUri]
        ], get("/processes/$processId").parseETag())

        then:
        with(get("/processes/$processId").body.domains[domainId].decisionResults.piaMandatory) {
            value == false
            decision.rules[decisiveRule].description.en == "Processing on list of the kinds of processing operations not subject to a DPIA"
            matchingRules.collect { decision.rules[it].description.en } =~ [
                "Processing on list of the kinds of processing operations not subject to a DPIA",
                "Two or more criteria apply"
            ]
            agreeingRules.collect { decision.rules[it].description.en } =~ [
                "Processing on list of the kinds of processing operations not subject to a DPIA"
            ]
        }
    }
}
