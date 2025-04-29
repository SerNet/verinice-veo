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

import groovy.util.logging.Slf4j

@Slf4j
class RiskValuesRestTestITSpec extends VeoRestTest{

    public static final String IT_HAPPENED = "everything possible will happen, so it has happened already, infinite times"
    public static final String NO_DICE = "God does not play dice."
    public static final String BRACE_FOR_IMPACT = "Brace for impact"
    public static final String NO_RISK = "Don't be silly. If this was really the ship's Self-Destruct-Button, do you think they'd leave it lying around where anyone could press it?"
    public static final String PROBLEM = "Houston, we have a problem"

    String unitId

    def setup() {
        unitId = postNewUnit().resourceId
    }

    def "create and update process risk values"() {
        given: "a composite process and a scenario"
        def processId = post("/domains/$dsgvoDomainId/processes", [
            subType: "PRO_DataTransfer",
            status: "NEW",
            riskValues: [
                DSRA : [
                    potentialImpacts: [
                        "C": 0,
                        "I": 1
                    ]
                ]
            ],
            name: "risk test process",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId

        def scenarioId = post("/domains/$dsgvoDomainId/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            subType: "SCN_Scenario",
            status: "NEW",
            riskValues: [
                DSRA : [
                    potentialProbability           : 2,
                    potentialProbabilityExplanation: "$IT_HAPPENED"
                ]
            ]
        ]).body.resourceId

        when: "creating the risk with only partial risk values"
        def riskBody = [
            domains : [
                (dsgvoDomainId): [
                    reference: [targetUri: "$baseUrl/domains/$dsgvoDomainId"],
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

        def createdRiskValues = risk.domains.(dsgvoDomainId)
                .riskDefinitions.values()[0]
        def probability = createdRiskValues.probability
        def impactI = createdRiskValues.impactValues.find {it.category == "I"}
        def riskI = createdRiskValues.riskValues.find {it.category == "I"}
        def impactA = createdRiskValues.impactValues.find {it.category == "A"}
        def riskA = createdRiskValues.riskValues.find {it.category == "A"}
        def impactC = createdRiskValues.impactValues.find {it.category == "C"}

        // one json object for each category and one for probability was initialized:
        risk.domains.(dsgvoDomainId).riskDefinitions.values()[0].impactValues.size() == 4
        risk.domains.(dsgvoDomainId).riskDefinitions.values()[0].riskValues.size() == 4
        probability == [potentialProbability:2, effectiveProbability:2]

        impactI.size() == 3
        impactI.category == "I"
        impactI.potentialImpact == 1
        impactI.effectiveImpact == 1
        with(riskI) {
            size() == 4
            category == "I"
            riskTreatments == []
            inherentRisk == 1
            residualRisk == 1
        }

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

        riskI.userDefinedResidualRisk = 1
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

        put("/processes/$processId/risks/$scenarioId", risk, retrievedRiskResponse.getETag())

        then: "the changed risk values can be retrieved"
        def updatedRisk = get("/processes/$processId/risks/$scenarioId").body

        def updatedRiskValues = updatedRisk.domains.(dsgvoDomainId)
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
        updatedRiskI.userDefinedResidualRisk == 1
        updatedRiskI.residualRiskExplanation == NO_RISK
        updatedRiskI.riskTreatments ==~ [
            "RISK_TREATMENT_ACCEPTANCE",
            "RISK_TREATMENT_TRANSFER"
        ]
        updatedRiskI.riskTreatmentExplanation == PROBLEM
        updatedRiskI.residualRisk == 1

        // read-only values have not been changed:
        updatedProbability.potentialProbability == 2
        updatedImpactI.potentialImpact == 1

        and: "the first saved values are still present"
        updatedImpactA.category == "A"
        updatedImpactA.specificImpact == 1
        updatedRiskA.category == "A"
        updatedRiskA.residualRiskExplanation == PROBLEM
        updatedRiskA.riskTreatments ==~ ["RISK_TREATMENT_REDUCTION"]
        updatedRiskA.residualRisk == 1

        when: "updating scenario risk values"
        get("/domains/$dsgvoDomainId/scenarios/$scenarioId").with{
            body.riskValues.DSRA.potentialProbability = 3
            put(body._self, body, getETag())
        }

        then: "the risk has been updated"
        with(get("/processes/$processId/risks/$scenarioId").body.domains[dsgvoDomainId].riskDefinitions.DSRA) {
            it.probability.potentialProbability == 3
        }

        when: "updating process risk values"
        get("/domains/$dsgvoDomainId/processes/$processId").with{
            body.riskValues.DSRA.potentialImpacts.C = 2
            put(body._self, body, getETag())
        }

        then: "the risk has been updated"
        with(get("/processes/$processId/risks/$scenarioId").body.domains[dsgvoDomainId].riskDefinitions.DSRA) {
            it.impactValues.find {it.category == "C"}.potentialImpact == 2
        }
    }

    def "create and update process risk values with mitigations"(String type, String subType) {
        given: "a composite process and a scenario"
        def processId = post("/domains/$dsgvoDomainId/$type", [
            subType: "$subType",
            status: "NEW",
            riskValues: [
                DSRA : [
                    potentialImpacts: [
                        "C": 0,
                        "I": 1
                    ]
                ]
            ],
            name: "risk test $type",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId

        def scenarioId = post("/domains/$dsgvoDomainId/scenarios", [
            name: "$type risk test scenario",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            subType: "SCN_Scenario",
            status: "NEW",
            riskValues: [
                DSRA : [
                    potentialProbability: 2
                ]
            ]
        ]).body.resourceId

        def controlId = post("/domains/$dsgvoDomainId/controls", [
            name: "$type risk test control",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            subType: "CTL_TOM",
            status: "NEW"
        ]).body.resourceId

        when: "creating the risk with only partial risk values"
        def riskBody = [
            mitigation : [targetUri: "$baseUrl/controls/$controlId"],
            domains : [
                (dsgvoDomainId): [
                    reference: [targetUri: "$baseUrl/domains/$dsgvoDomainId"],
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
        post("/$type/$processId/risks", riskBody)

        then: "categories were fully initialized including the provided values"
        def retrievedRiskResponse = get("/$type/$processId/risks/$scenarioId")
        def risk = retrievedRiskResponse.body
        risk.scenario.targetUri ==~ /.*\/scenarios\/$scenarioId/

        def createdRiskValues = risk.domains.(dsgvoDomainId)
                .riskDefinitions.values()[0]
        def probability = createdRiskValues.probability
        def impactI = createdRiskValues.impactValues.find {it.category == "I"}
        def riskI = createdRiskValues.riskValues.find {it.category == "I"}
        def impactA = createdRiskValues.impactValues.find {it.category == "A"}
        def riskA = createdRiskValues.riskValues.find {it.category == "A"}
        def impactC = createdRiskValues.impactValues.find {it.category == "C"}

        // one json object for each category and one for probability was initialized:
        risk.domains.(dsgvoDomainId).riskDefinitions.values()[0].impactValues.size() == 4
        risk.domains.(dsgvoDomainId).riskDefinitions.values()[0].riskValues.size() == 4
        probability == [potentialProbability:2, effectiveProbability:2]

        impactI.size() == 3
        impactI.category == "I"
        impactI.potentialImpact == 1
        impactI.effectiveImpact == 1
        with(riskI) {
            size() == 4
            category == "I"
            riskTreatments == []
            inherentRisk == 1
            residualRisk == 1
        }

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

        riskI.userDefinedResidualRisk = 1
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

        put("/$type/$processId/risks/$scenarioId", risk, retrievedRiskResponse.getETag())

        then: "the changed risk values can be retrieved"
        def updatedRisk = get("/$type/$processId/risks/$scenarioId").body

        def updatedRiskValues = updatedRisk.domains.(dsgvoDomainId)
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
        updatedRiskI.userDefinedResidualRisk == 1
        updatedRiskI.residualRiskExplanation == NO_RISK
        updatedRiskI.riskTreatments ==~ [
            "RISK_TREATMENT_ACCEPTANCE",
            "RISK_TREATMENT_TRANSFER"
        ]
        updatedRiskI.riskTreatmentExplanation == PROBLEM
        updatedRiskI.residualRisk == 1

        // read-only values have not been changed:
        updatedProbability.potentialProbability == 2
        updatedImpactI.potentialImpact == 1

        and: "the first saved values are still present"
        updatedImpactA.category == "A"
        updatedImpactA.specificImpact == 1
        updatedRiskA.category == "A"
        updatedRiskA.residualRiskExplanation == PROBLEM
        updatedRiskA.riskTreatments ==~ ["RISK_TREATMENT_REDUCTION"]
        updatedRiskA.residualRisk == 1

        where:
        type|subType
        "processes" | "PRO_DataTransfer"
        "assets" | "AST_Datatype"
        "scopes" | "SCP_Controller"
    }

    def "create and update asset risk values"() {
        given: "a composite asset and a scenario"
        def assetId = post("/domains/$dsgvoDomainId/assets", [
            subType: "AST_Datatype",
            status: "NEW",
            riskValues: [
                DSRA : [
                    potentialImpacts: [
                        "C": 0,
                        "I": 1
                    ]
                ]
            ],
            name: "risk test process",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId

        def scenarioId = post("/domains/$dsgvoDomainId/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            subType: "SCN_Scenario",
            status: "NEW",
            riskValues: [
                DSRA : [
                    potentialProbability           : 2,
                    potentialProbabilityExplanation: "$IT_HAPPENED"
                ]
            ]
        ]).body.resourceId

        when: "creating the risk with only partial risk values"
        def riskBody = [
            domains : [
                (dsgvoDomainId): [
                    reference: [targetUri: "$baseUrl/domains/$dsgvoDomainId"],
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
        post("/assets/$assetId/risks", riskBody)

        then: "categories were fully initialized including the provided values"
        def retrievedRiskResponse = get("/assets/$assetId/risks/$scenarioId")
        def risk = retrievedRiskResponse.body
        risk.scenario.targetUri ==~ /.*\/scenarios\/$scenarioId/

        def retrievedAssetResponse = get("/assets/$assetId").body

        with(retrievedAssetResponse.domains.(dsgvoDomainId).riskValues.DSRA.potentialImpacts) {
            C == 0
            I == 1
        }

        def createdRiskValues = risk.domains.(dsgvoDomainId)
                .riskDefinitions.values()[0]
        def probability = createdRiskValues.probability
        def impactI = createdRiskValues.impactValues.find {it.category == "I"}
        def riskI = createdRiskValues.riskValues.find {it.category == "I"}
        def impactA = createdRiskValues.impactValues.find {it.category == "A"}
        def riskA = createdRiskValues.riskValues.find {it.category == "A"}
        def impactC = createdRiskValues.impactValues.find {it.category == "C"}

        // one json object for each category and one for probability was initialized:
        risk.domains.(dsgvoDomainId).riskDefinitions.values()[0].impactValues.size() == 4
        risk.domains.(dsgvoDomainId).riskDefinitions.values()[0].riskValues.size() == 4
        probability == [potentialProbability:2, effectiveProbability:2]

        impactI.size() == 3
        impactI.category == "I"
        impactI.potentialImpact == 1
        impactI.effectiveImpact == 1
        with(riskI) {
            size() == 4
            category == "I"
            riskTreatments == []
            inherentRisk == 1
            residualRisk == 1
        }

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

        riskI.userDefinedResidualRisk = 1
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

        put("/assets/$assetId/risks/$scenarioId", risk, retrievedRiskResponse.getETag())

        then: "the changed risk values can be retrieved"
        def updatedRisk = get("/assets/$assetId/risks/$scenarioId").body

        def updatedRiskValues = updatedRisk.domains.(dsgvoDomainId)
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
        updatedRiskI.userDefinedResidualRisk == 1
        updatedRiskI.residualRiskExplanation == NO_RISK
        updatedRiskI.riskTreatments ==~ [
            "RISK_TREATMENT_ACCEPTANCE",
            "RISK_TREATMENT_TRANSFER"
        ]
        updatedRiskI.riskTreatmentExplanation == PROBLEM
        updatedRiskI.residualRisk == 1

        // read-only values have not been changed:
        updatedProbability.potentialProbability == 2
        updatedImpactI.potentialImpact == 1

        and: "the first saved values are still present"
        updatedImpactA.category == "A"
        updatedImpactA.specificImpact == 1
        updatedRiskA.category == "A"
        updatedRiskA.residualRiskExplanation == PROBLEM
        updatedRiskA.riskTreatments ==~ ["RISK_TREATMENT_REDUCTION"]
        updatedRiskA.residualRisk == 1

        when: "updating scenario risk values"
        get("/domains/$dsgvoDomainId/scenarios/$scenarioId").with{
            body.riskValues.DSRA.potentialProbability = 3
            put(body._self, body, getETag())
        }

        then: "the risk has been updated"
        with(get("/assets/$assetId/risks/$scenarioId").body.domains[dsgvoDomainId].riskDefinitions.DSRA) {
            it.probability.potentialProbability == 3
        }

        when: "updating process risk values"
        get("/domains/$dsgvoDomainId/assets/$assetId").with{
            body.riskValues.DSRA.potentialImpacts.C = 2
            put(body._self, body, getETag())
        }

        then: "the risk has been updated"
        with(get("/assets/$assetId/risks/$scenarioId").body.domains[dsgvoDomainId].riskDefinitions.DSRA) {
            it.impactValues.find {it.category == "C"}.potentialImpact == 2
        }
    }

    def "create and update scope risk values"() {
        given: "a composite scope and a scenario"
        def scopeId = post("/domains/$dsgvoDomainId/scopes", [
            subType: "SCP_Controller",
            status: "NEW",
            riskValues: [
                DSRA : [
                    potentialImpacts: [
                        "C": 0,
                        "I": 1
                    ]
                ]
            ],
            name: "risk test scope",
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId

        def scenarioId = post("/domains/$dsgvoDomainId/scenarios", [
            name: "process risk test scenario",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            subType: "SCN_Scenario",
            status: "NEW",
            riskValues: [
                DSRA : [
                    potentialProbability           : 2,
                    potentialProbabilityExplanation: "$IT_HAPPENED"
                ]
            ]
        ]).body.resourceId

        when: "creating the risk with only partial risk values"
        def riskBody = [
            domains : [
                (dsgvoDomainId): [
                    reference: [targetUri: "$baseUrl/domains/$dsgvoDomainId"],
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
        post("/scopes/$scopeId/risks", riskBody)

        then: "categories were fully initialized including the provided values"
        def retrievedRiskResponse = get("/scopes/$scopeId/risks/$scenarioId")
        def risk = retrievedRiskResponse.body
        risk.scenario.targetUri ==~ /.*\/scenarios\/$scenarioId/

        def retrievedAssetResponse = get("/scopes/$scopeId").body

        with(retrievedAssetResponse.domains.(dsgvoDomainId).riskValues.DSRA.potentialImpacts) {
            C == 0
            I == 1
        }

        def createdRiskValues = risk.domains.(dsgvoDomainId)
                .riskDefinitions.values()[0]
        def probability = createdRiskValues.probability
        def impactI = createdRiskValues.impactValues.find {it.category == "I"}
        def riskI = createdRiskValues.riskValues.find {it.category == "I"}
        def impactA = createdRiskValues.impactValues.find {it.category == "A"}
        def riskA = createdRiskValues.riskValues.find {it.category == "A"}
        def impactC = createdRiskValues.impactValues.find {it.category == "C"}

        // one json object for each category and one for probability was initialized:
        risk.domains.(dsgvoDomainId).riskDefinitions.values()[0].impactValues.size() == 4
        risk.domains.(dsgvoDomainId).riskDefinitions.values()[0].riskValues.size() == 4
        probability == [potentialProbability:2, effectiveProbability:2]

        impactI.size() == 3
        impactI.category == "I"
        impactI.potentialImpact == 1
        impactI.effectiveImpact == 1
        with(riskI) {
            size() == 4
            category == "I"
            riskTreatments == []
            inherentRisk == 1
            residualRisk == 1
        }

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

        riskI.userDefinedResidualRisk = 1
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

        put("/scopes/$scopeId/risks/$scenarioId", risk, retrievedRiskResponse.getETag())

        then: "the changed risk values can be retrieved"
        def updatedRisk = get("/scopes/$scopeId/risks/$scenarioId").body

        def updatedRiskValues = updatedRisk.domains.(dsgvoDomainId)
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
        updatedRiskI.userDefinedResidualRisk == 1
        updatedRiskI.residualRiskExplanation == NO_RISK
        updatedRiskI.riskTreatments ==~ [
            "RISK_TREATMENT_ACCEPTANCE",
            "RISK_TREATMENT_TRANSFER"
        ]
        updatedRiskI.riskTreatmentExplanation == PROBLEM
        updatedRiskI.residualRisk == 1

        // read-only values have not been changed:
        updatedProbability.potentialProbability == 2
        updatedImpactI.potentialImpact == 1

        and: "the first saved values are still present"
        updatedImpactA.category == "A"
        updatedImpactA.specificImpact == 1
        updatedRiskA.category == "A"
        updatedRiskA.residualRiskExplanation == PROBLEM
        updatedRiskA.riskTreatments ==~ ["RISK_TREATMENT_REDUCTION"]
        updatedRiskA.residualRisk == 1

        when: "updating scenario risk values"
        get("/domains/$dsgvoDomainId/scenarios/$scenarioId").with{
            body.riskValues.DSRA.potentialProbability = 3
            put(body._self, body, getETag())
        }

        then: "the risk has been updated"
        with(get("/scopes/$scopeId/risks/$scenarioId").body.domains[dsgvoDomainId].riskDefinitions.DSRA) {
            it.probability.potentialProbability == 3
        }

        when: "updating process risk values"
        get("/domains/$dsgvoDomainId/scopes/$scopeId").with{
            body.riskValues.DSRA.potentialImpacts.C = 2
            put(body._self, body, getETag())
        }

        then: "the risk has been updated"
        with(get("/scopes/$scopeId/risks/$scenarioId").body.domains[dsgvoDomainId].riskDefinitions.DSRA) {
            it.impactValues.find {it.category == "C"}.potentialImpact == 2
        }
    }

    def "calculate impact inheritance for asset"() {
        when: "we build a chain from 0-2"
        def asset2Id = post("/domains/$dsgvoDomainId/assets", [
            name: "asset-2",
            subType: "AST_Datatype",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"],
        ]).body.resourceId

        def asset1Id = post("/domains/$dsgvoDomainId/assets", [
            name: "asset-1",
            subType: "AST_Datatype",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            links: [
                asset_asset_dat: [
                    [
                        target: [targetUri: "$baseUrl/assets/$asset2Id"]
                    ]
                ]
            ]
        ]).body.resourceId

        def assetWithImpactId = post("/domains/$dsgvoDomainId/assets", [
            name: "asset-0",
            subType: "AST_Datatype",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            riskValues: [
                DSRA : [
                    potentialImpacts: [
                        "C": 1,
                        "I": 2
                    ]
                ]
            ],
            links: [
                asset_asset_dat: [
                    [
                        target: [targetUri: "$baseUrl/assets/$asset1Id"]
                    ]
                ]
            ]
        ]).body.resourceId

        then: "the values of the dependents are set"
        with(get("/domains/$dsgvoDomainId/assets/$asset2Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 1
            potentialImpactsEffective.I == 2
            potentialImpactsCalculated.C == 1
            potentialImpactsCalculated.I == 2
        }
        with(get("/domains/$dsgvoDomainId/assets/$asset1Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 1
            potentialImpactsEffective.I == 2
            potentialImpactsCalculated.C == 1
            potentialImpactsCalculated.I == 2
        }
        with(get("/domains/$dsgvoDomainId/assets/$assetWithImpactId").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 1
            potentialImpactsEffective.I == 2
            potentialImpactsCalculated == [:]
        }

        when: "set the leaf to a new value"
        log.debug("--------------------------------------")
        get("/domains/$dsgvoDomainId/assets/$assetWithImpactId").with{
            body.riskValues.DSRA.potentialImpacts.C = 0
            body.riskValues.DSRA.potentialImpacts.I = 0
            body.riskValues.DSRA.potentialImpacts.A = 0
            put(body._self, body, getETag())
        }

        then: "the linked assets are updated"
        with(get("/domains/$dsgvoDomainId/assets/$asset1Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 0
            potentialImpactsEffective.I == 0
            potentialImpactsCalculated.C == 0
            potentialImpactsCalculated.I == 0
        }
        get("/domains/$dsgvoDomainId/assets/$asset2Id").body.riskValues.DSRA.with{
            potentialImpactsEffective.C == 0
            potentialImpactsEffective.I == 0
            potentialImpactsCalculated.C == 0
            potentialImpactsCalculated.I == 0
        }

        when: "we update again and add a category"
        log.debug("--------------------------------------")
        get("/domains/$dsgvoDomainId/assets/$assetWithImpactId").with{
            body.riskValues.DSRA.potentialImpacts.C = 0
            body.riskValues.DSRA.potentialImpacts.I = 1
            body.riskValues.DSRA.potentialImpacts.A = 0
            put(body._self, body, getETag())
        }

        then: "the linked assets are updated"
        with(get("/domains/$dsgvoDomainId/assets/$asset1Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 0
            potentialImpactsEffective.I == 1
            potentialImpactsEffective.A == 0
            potentialImpactsCalculated.C == 0
            potentialImpactsCalculated.I == 1
            potentialImpactsCalculated.A == 0
        }

        with(get("/domains/$dsgvoDomainId/assets/$asset2Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 0
            potentialImpactsEffective.I == 1
            potentialImpactsEffective.A == 0
            potentialImpactsCalculated.C == 0
            potentialImpactsCalculated.I == 1
            potentialImpactsCalculated.A == 0
        }

        when: "we update again"
        log.debug("--------------------------------------")
        get("/domains/$dsgvoDomainId/assets/$assetWithImpactId").with{
            body.riskValues.DSRA.potentialImpacts.C = 2
            body.riskValues.DSRA.potentialImpacts.I = 2
            body.riskValues.DSRA.potentialImpacts.A = 2
            put(body._self, body, getETag())
        }

        then: "the linked assets are updated"
        with(get("/domains/$dsgvoDomainId/assets/$asset1Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 2
            potentialImpactsEffective.I == 2
            potentialImpactsEffective.A == 2
            potentialImpactsCalculated.C == 2
            potentialImpactsCalculated.I == 2
            potentialImpactsCalculated.A == 2
        }

        with(get("/domains/$dsgvoDomainId/assets/$asset2Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 2
            potentialImpactsEffective.I == 2
            potentialImpactsEffective.A == 2
            potentialImpactsCalculated.C == 2
            potentialImpactsCalculated.I == 2
            potentialImpactsCalculated.A == 2
        }

        when: "we add a new leaf element in the chain"
        log.debug("--------------------------------------")
        def asset3Id = post("/domains/$dsgvoDomainId/assets", [
            name: "asset-3",
            subType: "AST_Datatype",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"],
        ]).body.resourceId

        get("/domains/$dsgvoDomainId/assets/$asset2Id").with{
            body.links = [
                asset_asset_dat: [
                    [
                        target: [targetUri: "$baseUrl/assets/$asset3Id"]
                    ]
                ]

            ]
            put(body._self, body, getETag())
        }

        then: "the new leaf element was updated"
        with(get("/domains/$dsgvoDomainId/assets/$asset3Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 2
            potentialImpactsEffective.I == 2
            potentialImpactsEffective.A == 2
            potentialImpactsCalculated.C == 2
            potentialImpactsCalculated.I == 2
            potentialImpactsCalculated.A == 2
        }

        when: "we add a circle in the chain by linking asset-3 to asset-2"
        log.debug("circle--------------------------------------")
        get("/domains/$dsgvoDomainId/assets/$asset3Id").with{

            body.links = [
                asset_asset_dat: [
                    [
                        target: [targetUri: "$baseUrl/assets/$assetWithImpactId"]
                    ]
                ]

            ]
            put(body._self, body, getETag())
        }

        then: "no updates were made"
        with(get("/domains/$dsgvoDomainId/assets/$asset3Id").body.riskValues.DSRA) {
            potentialImpactsCalculated == [:]
        }
        with(get("/domains/$dsgvoDomainId/assets/$assetWithImpactId").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 2
            potentialImpactsEffective.I == 2
            potentialImpactsEffective.A == 2
            potentialImpactsCalculated == [:]
        }

        with(get("/domains/$dsgvoDomainId/assets/$asset1Id").body.riskValues.DSRA) {
            potentialImpactsCalculated == [:]
        }
        with(get("/domains/$dsgvoDomainId/assets/$asset2Id").body.riskValues.DSRA) {
            potentialImpactsCalculated == [:]
        }

        when: "we remove the circle"
        log.debug("remove link--------------------------------------")
        get("/domains/$dsgvoDomainId/assets/$asset3Id").with{
            body.links = [:]
            put(body._self, body, getETag())
        }

        then: "the values are updated"
        with(get("/domains/$dsgvoDomainId/assets/$asset2Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 2
            potentialImpactsEffective.I == 2
            potentialImpactsEffective.A == 2
            potentialImpactsCalculated.C == 2
            potentialImpactsCalculated.I == 2
            potentialImpactsCalculated.A == 2
        }
        with(get("/domains/$dsgvoDomainId/assets/$asset1Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 2
            potentialImpactsEffective.I == 2
            potentialImpactsEffective.A == 2
            potentialImpactsCalculated.C == 2
            potentialImpactsCalculated.I == 2
            potentialImpactsCalculated.A == 2
        }
        with(get("/domains/$dsgvoDomainId/assets/$assetWithImpactId").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 2
            potentialImpactsEffective.I == 2
            potentialImpactsEffective.A == 2
            potentialImpactsCalculated == [:]
        }
        with(get("/domains/$dsgvoDomainId/assets/$asset3Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 2
            potentialImpactsEffective.I == 2
            potentialImpactsEffective.A == 2
            potentialImpactsCalculated.C == 2
            potentialImpactsCalculated.I == 2
            potentialImpactsCalculated.A == 2
        }

        when: "we create a risk for the leaf asset"
        def scenarioId = post("/domains/$dsgvoDomainId/scenarios", [
            name: "asset risk test scenario",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            subType: "SCN_Scenario",
            status: "NEW",
            riskValues: [
                DSRA: [
                    potentialProbability: 2,
                ]
            ]
        ]).body.resourceId

        post("/assets/$asset3Id/risks", [
            domains: [
                (dsgvoDomainId): [
                    reference: [targetUri: "$baseUrl/domains/$dsgvoDomainId"],
                    riskDefinitions: [
                        DSRA: [:]
                    ]
                ]
            ],
            scenario: [targetUri: "$baseUrl/scenarios/$scenarioId"]
        ])

        and: "and the root asset"
        post("/assets/$assetWithImpactId/risks", [
            domains: [
                (dsgvoDomainId): [
                    reference: [targetUri: "$baseUrl/domains/$dsgvoDomainId"],
                    riskDefinitions: [
                        DSRA: [:]
                    ]
                ]
            ],
            scenario: [targetUri: "$baseUrl/scenarios/$scenarioId"]
        ])

        def rootAssetRisk = get("/assets/$assetWithImpactId/risks/$scenarioId").body
        def leafAssetRIsk = get("/assets/$asset3Id/risks/$scenarioId").body

        then: "the leat and root assets' risks have identical values"
        rootAssetRisk.domains.(dsgvoDomainId).riskDefinitions.DSRA ==
                leafAssetRIsk.domains.(dsgvoDomainId).riskDefinitions.DSRA
    }

    def "calculate impact when deleting element in circle"() {
        given: "we build a chain from 0-3"
        def asset3Id = post("/domains/$dsgvoDomainId/assets", [
            name: "asset-3",
            subType: "AST_Datatype",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"],
        ]).body.resourceId

        def asset2Id = post("/domains/$dsgvoDomainId/assets", [
            name: "asset-0",
            subType: "AST_Datatype",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            links: [
                asset_asset_dat: [
                    [
                        target: [targetUri: "$baseUrl/assets/$asset3Id"]
                    ]
                ]
            ]

        ]).body.resourceId

        def asset1Id = post("/domains/$dsgvoDomainId/assets", [
            name: "asset-1",
            subType: "AST_Datatype",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            links: [
                asset_asset_dat: [
                    [
                        target: [targetUri: "$baseUrl/assets/$asset2Id"]
                    ]
                ]
            ]
        ]).body.resourceId

        def assetWithImpactId = post("/domains/$dsgvoDomainId/assets", [
            name: "asset-0",
            subType: "AST_Datatype",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            riskValues: [
                DSRA : [
                    potentialImpacts: [
                        "C": 2,
                        "I": 2
                    ]
                ]
            ],
            links: [
                asset_asset_dat: [
                    [
                        target: [targetUri: "$baseUrl/assets/$asset1Id"]
                    ]
                ]
            ]
        ]).body.resourceId

        when: "we add a circle in the chain by linking asset-3 to asset-withImpact"
        log.debug("circle--------------------------------------")
        get("/domains/$dsgvoDomainId/assets/$asset3Id").with{

            body.links = [
                asset_asset_dat: [
                    [
                        target: [targetUri: "$baseUrl/assets/$assetWithImpactId"]
                    ]
                ]

            ]
            put(body._self, body, getETag())
        }

        then: "no updates were made"
        with(get("/domains/$dsgvoDomainId/assets/$asset3Id").body.riskValues.DSRA) {
            potentialImpactsCalculated == [:]
        }

        when: "we remove the circle by deleting asset-3"
        log.debug("remove asset-3--------------------------------------")
        delete("/assets/$asset3Id")

        then: "the values are updated"
        with(get("/domains/$dsgvoDomainId/assets/$asset2Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 2
            potentialImpactsEffective.I == 2
            potentialImpactsCalculated.C == 2
            potentialImpactsCalculated.I == 2
        }
        with(get("/domains/$dsgvoDomainId/assets/$asset1Id").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 2
            potentialImpactsEffective.I == 2
            potentialImpactsCalculated.C == 2
            potentialImpactsCalculated.I == 2
        }
        with(get("/domains/$dsgvoDomainId/assets/$assetWithImpactId").body.riskValues.DSRA) {
            potentialImpactsEffective.C == 2
            potentialImpactsEffective.I == 2
            potentialImpactsCalculated == [:]
        }
    }
}
