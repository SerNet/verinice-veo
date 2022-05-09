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

class ProcessRiskValuesRestTestITSpec extends VeoRestTest{

    public static final String NO_DICE = "God does not play dice."
    public static final String BRACE_FOR_IMPACT = "Brace for impact"
    public static final String NO_RISK = "Don't be silly. If this was really the ship's Self-Destruct-Button, do you think they'd leave it lying around where anyone could press it?"
    public static final String PROBLEM = "Houston, we have a problem"

    String unitId
    String domainId

    def setup() {
        unitId = post("/units", [name: "process risk rest test unit"]).body.resourceId
        domainId = get("/domains").body[0].id
    }

    def "create and update process risk values"() {
        given: "a composite process and a scenario"
        def processId = post("/processes", [
            domains: [
                (domainId): [ : ]
            ],
            name: "risk test process",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId
        def processETag = get("/processes/$processId").parseETag()

        post("/scopes", [
            name: "DSRA scope",
            domains: [
                (domainId): [
                    riskDefinition: "DSRA"
                ]
            ],
            members: [
                [targetUri: "$baseUrl/processes/$processId"]
            ],
            owner: [targetUri: "$baseUrl/units/$unitId"],
        ])

        put("/processes/$processId", [
            domains: [
                (domainId): [
                    riskValues: [
                        DSRA : [
                            potentialImpacts: [
                                "C": 0,
                                "I": 1
                            ]
                        ]
                    ]
                ]
            ],
            name: "risk test process",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ], processETag)

        def scenarioId = post("/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            domains: [
                (domainId): [
                    riskValues: [
                        DSRA : [
                            potentialProbability: 2
                        ]
                    ]
                ]
            ]
        ]).body.resourceId

        when: "creating the risk with only partial risk values"
        def riskBody = [
            domains : [
                (domainId): [
                    reference: [targetUri: "$baseUrl/domains/$domainId"],
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
                                    residualRiskExplanation: PROBLEM,
                                    riskTreatments: ["RISK_TREATMENT_REDUCTION"]
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "$baseUrl/scenarios/$scenarioId"]
        ]
        post("/processes/$processId/risks", riskBody)


        then: "categories were fully initialized including the provided values"
        def retrievedRiskResponse = get("/processes/$processId/risks/$scenarioId")
        def risk = retrievedRiskResponse.body
        risk.scenario.targetUri ==~ /.*\/scenarios\/$scenarioId/

        def createdRiskValues = risk.domains.(domainId)
                .riskDefinitions.values()[0]
        def probability = createdRiskValues.probability
        def impactI = createdRiskValues.impactValues.find {it.category == "I"}
        def riskI = createdRiskValues.riskValues.find {it.category == "I"}
        def impactA = createdRiskValues.impactValues.find {it.category == "A"}
        def riskA = createdRiskValues.riskValues.find {it.category == "A"}
        def impactC = createdRiskValues.impactValues.find {it.category == "C"}


        // one json object for each category and one for probability was initialized:
        risk.domains.(domainId).riskDefinitions.values()[0].impactValues.size() == 4
        risk.domains.(domainId).riskDefinitions.values()[0].riskValues.size() == 4
        probability == [potentialProbability:2, effectiveProbability:2]

        impactI.size() == 3
        impactI.category == "I"
        impactI.potentialImpact == 1
        impactI.effectiveImpact == 1
        riskI.size() == 3
        riskI.category == "I"
        riskI.riskTreatments == []
        riskI.inherentRisk == 1

        impactA.size() == 3
        impactA.category == "A"
        impactA.specificImpact == 1
        impactA.effectiveImpact == 1
        riskA.category == "A"
        riskA.residualRiskExplanation == PROBLEM
        riskA.riskTreatments ==~ ["RISK_TREATMENT_REDUCTION"]

        impactC.size() == 3
        impactC.category == "C"
        impactC.potentialImpact == 0
        impactC.effectiveImpact == 0

        when: "the risk is updated with additional values"
        probability.specificProbability = 2
        probability.specificProbabilityExplanation = NO_DICE

        impactI.specificImpact = 3
        impactI.specificImpactExplanation = BRACE_FOR_IMPACT

        riskI.residualRisk = 1
        riskI.residualRiskExplanation = NO_RISK
        riskI.riskTreatments = [
            "RISK_TREATMENT_ACCEPTANCE",
            "RISK_TREATMENT_TRANSFER"
        ]
        riskI.riskTreatmentExplanation = PROBLEM

        // make sure that read-only fields are *not* saved:
        probability.potentialProbability = 1
        impactI.potentialImpact = 2
        riskI.inherentRisk = 2

        put("/processes/$processId/risks/$scenarioId", risk, retrievedRiskResponse.parseETag())


        then: "the changed risk values can be retrieved"
        def updatedRisk = get("/processes/$processId/risks/$scenarioId").body

        def updatedRiskValues = updatedRisk.domains.(domainId)
                .riskDefinitions.values()[0]
        def updatedProbability = updatedRiskValues.probability
        def updatedImpactI = updatedRiskValues.impactValues.find {it.category == "I"}
        def updatedRiskI = updatedRiskValues.riskValues.find {it.category == "I"}
        def updatedImpactA = updatedRiskValues.impactValues.find {it.category == "A"}
        def updatedRiskA = updatedRiskValues.riskValues.find {it.category == "A"}

        updatedProbability.specificProbability == 2
        updatedProbability.effectiveProbability == 2
        updatedProbability.specificProbabilityExplanation == NO_DICE

        updatedImpactI.specificImpact == 3
        updatedImpactI.effectiveImpact == 3
        updatedImpactI.specificImpactExplanation == BRACE_FOR_IMPACT

        updatedRiskI.inherentRisk == 3
        updatedRiskI.residualRisk == 1
        updatedRiskI.residualRiskExplanation == NO_RISK
        updatedRiskI.riskTreatments ==~ [
            "RISK_TREATMENT_ACCEPTANCE",
            "RISK_TREATMENT_TRANSFER"
        ]
        updatedRiskI.riskTreatmentExplanation == PROBLEM

        // read-only values have not been changed:
        updatedProbability.potentialProbability == 2
        updatedImpactI.potentialImpact == 1

        and: "the first saved values are still present"
        updatedImpactA.category == "A"
        updatedImpactA.specificImpact == 1
        updatedRiskA.category == "A"
        updatedRiskA.residualRiskExplanation == PROBLEM
        updatedRiskA.riskTreatments ==~ ["RISK_TREATMENT_REDUCTION"]
    }
}
