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

class EvaluationRestTest extends VeoRestTest {
    def PIA = "process_privacyImpactAssessment"
    String unitUri
    String unitId
    def decisions

    def setup() {
        def domain = get("/domains/$dsgvoDomainId").body
        decisions = domain.decisions
        unitId = postNewUnit().resourceId
        unitUri = "http://localhost/units/" + unitId
    }

    def "piaMandatory decision result is persisted"() {
        given:
        def decision = decisions.piaMandatory

        when: "creating a data processing with two criteria"
        def processId = post("/domains/$dsgvoDomainId/processes", [
            name: "data processing",
            customAspects: [
                (PIA): [
                    ("${PIA}_processingCriteria"): [
                        "${PIA}_processingCriteria_automated",
                        "${PIA}_processingCriteria_specialCategories",
                    ],
                ]
            ],
            subType: "PRO_DataProcessing",
            status: "NEW",
            owner: [targetUri: unitUri]
        ]).body.resourceId

        then: "result is undetermined due to missing risks"
        with(get("/domains/$dsgvoDomainId/processes/$processId").body.decisionResults.piaMandatory) {
            value == null
            decision.rules[decisiveRule].description.en == "Missing risk analysis"
            matchingRules.collect { decision.rules[it].description.en } ==~ [
                "Missing risk analysis",
                "Two or more criteria applicable",
                "DPIA-relevant attributes incomplete",
            ]
            agreeingRules.collect { decision.rules[it].description.en } ==~ [
                "Missing risk analysis",
                "DPIA-relevant attributes incomplete",
            ]
        }

        when: "adding a risk"
        def processETagBeforeRiskAddition = get("/domains/$dsgvoDomainId/processes/$processId").getETag()
        addRiskValue(processId, 1)

        then: "the result has changed"
        with(get("/domains/$dsgvoDomainId/processes/$processId")) {
            getETag() != processETagBeforeRiskAddition
            with(body.decisionResults.piaMandatory) {
                value == true
                decision.rules[decisiveRule].description.en == "Two or more criteria applicable"
                matchingRules.collect { decision.rules[it].description.en } ==~ [
                    "Two or more criteria applicable",
                    "DPIA-relevant attributes incomplete",
                ]
                agreeingRules.collect { decision.rules[it].description.en } ==~ [
                    "Two or more criteria applicable"
                ]
            }
        }

        when: "adding PIA related attributes"
        put("/domains/$dsgvoDomainId/processes/$processId", [
            name: "data processing",
            customAspects: [
                (PIA): [
                    ("${PIA}_listed"): "${PIA}_listed_positive",
                    ("${PIA}_processingCriteria"): [
                        "${PIA}_processingCriteria_automated",
                        "${PIA}_processingCriteria_specialCategories",
                    ],
                ]
            ],
            subType: "PRO_DataProcessing",
            status: "NEW",
            owner: [targetUri: unitUri]
        ], get("/domains/$dsgvoDomainId/processes/$processId").getETag())

        then: "added attribute is taken into consideration"
        with(get("/domains/$dsgvoDomainId/processes/$processId").body.decisionResults.piaMandatory) {
            value == true
            decision.rules[decisiveRule].description.en == "Processing on list of the kinds of processing operations subject to a Data Protection Impact Assessment"
            matchingRules.collect { decision.rules[it].description.en } ==~ [
                "Processing on list of the kinds of processing operations subject to a Data Protection Impact Assessment",
                "Two or more criteria applicable",
                "DPIA-relevant attributes incomplete",
            ]
            agreeingRules.collect { decision.rules[it].description.en } ==~ [
                "Processing on list of the kinds of processing operations subject to a Data Protection Impact Assessment",
                "Two or more criteria applicable",
            ]
        }

        and: "inspection suggests creating a DPIA part"
        with(get("/processes/$processId/inspection?domain=$dsgvoDomainId").body) {
            size() == 1
            with(it[0]) {
                description.en == "Data Protection Impact Assessment was not carried out, but it is mandatory."
                severity == "WARNING"
                suggestions.size() == 1
                with(suggestions[0]) {
                    type == "addPart"
                    partSubType == "PRO_DPIA"
                }
            }
        }
    }

    def "piaMandatory decision and inspection can be evaluated for transient process"() {
        given: "a transient process"
        def decision = decisions.piaMandatory
        def process = [
            name: "transient process",
            customAspects: [
                (PIA): [
                    ("${PIA}_listed"): "${PIA}_listed_positive"
                ]
            ],
            subType: "PRO_DataProcessing",
            status: "NEW",
            owner: [targetUri: unitUri]
        ]

        expect: "non-persistent evaluation to consider custom aspect attribute and missing risk values"
        with(post("/domains/$dsgvoDomainId/processes/evaluation", process, 200).body) {
            with(decisionResults.piaMandatory) {
                value == null
                decision.rules[decisiveRule].description.en == "Missing risk analysis"
                matchingRules.collect{decision.rules[it].description.en} == [
                    "Missing risk analysis",
                    "Processing on list of the kinds of processing operations subject to a Data Protection Impact Assessment",
                    "DPIA-relevant attributes incomplete",
                ]
                agreeingRules.collect{decision.rules[it].description.en} == [
                    "Missing risk analysis",
                    "DPIA-relevant attributes incomplete",
                ]
            }
            inspectionFindings.empty
        }

        and: "no process has been persisted"
        get("/domains/$dsgvoDomainId/processes?unit=$unitId").body.totalItemCount == 0

        when: "persisting the process and adding a risk"
        def processId = post("/domains/$dsgvoDomainId/processes", process).body.resourceId
        process = get("/domains/$dsgvoDomainId/processes/$processId").body
        addRiskValue(processId, 1)

        then: "non-persistent evaluation returns different results due to added risk"
        with(post("/domains/$dsgvoDomainId/processes/evaluation", process, 200).body) {
            with(decisionResults.piaMandatory) {
                value == true
                decision.rules[decisiveRule].description.en == "Processing on list of the kinds of processing operations subject to a Data Protection Impact Assessment"
                matchingRules.collect { decision.rules[it].description.en } == [
                    "Processing on list of the kinds of processing operations subject to a Data Protection Impact Assessment",
                    "DPIA-relevant attributes incomplete",
                ]
                agreeingRules.collect { decision.rules[it].description.en } == [
                    "Processing on list of the kinds of processing operations subject to a Data Protection Impact Assessment"
                ]
            }
            inspectionFindings.size() == 1
            with(inspectionFindings[0]) {
                description.en == "Data Protection Impact Assessment was not carried out, but it is mandatory."
                severity == "WARNING"
                suggestions.size() == 1
                with(suggestions[0]) {
                    type == "addPart"
                    partSubType == "PRO_DPIA"
                }
            }
        }

        when: "adding an attribute to the transient process"
        def processUpdateTimeBeforeEvaluation = get("/domains/$dsgvoDomainId/processes/$processId").body.updatedAt
        process.customAspects[PIA]["${PIA}_otherExclusions"] = true

        then: "non-persistent evaluation returns different results due to added attribute"
        with(post("/domains/$dsgvoDomainId/processes/evaluation", process, 200).body) {
            with(decisionResults.piaMandatory) {
                value == false
                decision.rules[decisiveRule].description.en == "Other exclusions"
                matchingRules.collect { decision.rules[it].description.en } == [
                    "Other exclusions",
                    "Processing on list of the kinds of processing operations subject to a Data Protection Impact Assessment",
                    "DPIA-relevant attributes incomplete"
                ]
                agreeingRules.collect { decision.rules[it].description.en } == [
                    "Other exclusions"
                ]
            }
            inspectionFindings.empty
        }

        and: "changes to the process have not been persisted"
        with(get("/domains/$dsgvoDomainId/processes/$processId").body) {
            customAspects[owner.PIA].size() == 1
            updatedAt == processUpdateTimeBeforeEvaluation
        }

        and: "risk is still attached to the process"
        // TODO VEO-1890 use domain-specific risks endpoint
        get("/processes/$processId/risks").body.size() == 1
    }

    def "piaMandatory decision and inspection can be evaluated for process with a pia"() {
        given: "a persisted process with a DPIA as its part"
        def dpia = [
            name: "dpia",
            subType: "PRO_DPIA",
            status: "NEW",
            owner: [targetUri: unitUri]
        ]
        def dpiaId = post("/domains/$dsgvoDomainId/processes", dpia).body.resourceId

        def process = [
            name: "process with a DPIA",
            customAspects: [
                (PIA): [
                    ("${PIA}_listed"): "${PIA}_listed_positive"
                ]
            ],
            subType: "PRO_DataProcessing",
            status: "NEW",
            owner: [targetUri: unitUri],
            parts: [
                [targetUri: "http://localhost/processes/$dpiaId"]
            ]
        ]
        def processId = post("/domains/$dsgvoDomainId/processes", process).body.resourceId
        process = get("/domains/$dsgvoDomainId/processes/$processId").body
        addRiskValue(processId, 1)

        expect: "a DPIA is required and the existing one is accepted"
        with(post("/domains/$dsgvoDomainId/processes/evaluation", process, 200).body) {
            with(decisionResults.piaMandatory) {
                value == true
            }
            inspectionFindings.empty
        }
    }

    private void addRiskValue(String processId, Integer userDefinedResidualRisk) {
        post("/domains/$dsgvoDomainId/scopes", [
            name: "risky scope",
            subType: "SCP_Scope",
            status: "NEW",
            riskDefinition: "DSRA",
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
                (dsgvoDomainId): [
                    reference: [targetUri: "http://localhost/domains/$dsgvoDomainId"],
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
                                    userDefinedResidualRisk: userDefinedResidualRisk
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ])
    }
}
