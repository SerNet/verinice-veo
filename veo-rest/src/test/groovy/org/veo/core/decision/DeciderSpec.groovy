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
package org.veo.core.decision


import org.veo.core.entity.Domain
import org.veo.core.entity.Process
import org.veo.core.entity.ProcessRisk
import org.veo.core.entity.Scenario
import org.veo.core.entity.Unit
import org.veo.core.entity.decision.DecisionRef
import org.veo.core.entity.risk.CategorizedRiskValueProvider
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.DeterminedRiskImpl
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.entity.risk.RiskRef
import org.veo.core.entity.riskdefinition.RiskValue
import org.veo.core.usecase.decision.Decider
import org.veo.test.VeoSpec

class DeciderSpec extends VeoSpec {
    def piaMandatoryRef = new DecisionRef("piaMandatory")
    Decider decider = new Decider()

    Unit unit
    Domain domain
    RiskDefinitionRef riskDefinitionRef
    CategoryRef riskCategoryC
    CategoryRef riskCategoryI
    RiskRef riskValueLow
    RiskRef riskValueMedium
    RiskRef riskValueHigh
    RiskRef riskValueUltra
    Scenario scenarioA
    Scenario scenarioB

    def setup() {
        def client = newClient {}
        domain = newDomain(client)
        unit = newUnit(client)

        def riskDefinition = newRiskDefinition("testRiskDef") {
            categories = [
                newCategoryDefinition("C"),
                newCategoryDefinition("I"),
            ]
            riskValues = [
                new RiskValue("low"),
                new RiskValue("medium"),
                new RiskValue("high"),
                new RiskValue("ultra"),
            ]
        }
        domain.riskDefinitions = [
            (riskDefinition.id): riskDefinition
        ]

        riskDefinitionRef = RiskDefinitionRef.from(riskDefinition)
        riskCategoryC = CategoryRef.from(riskDefinition.categories[0])
        riskCategoryI = CategoryRef.from(riskDefinition.categories[1])
        riskValueLow = RiskRef.from(riskDefinition.riskValues[0])
        riskValueMedium = RiskRef.from(riskDefinition.riskValues[1])
        riskValueHigh = RiskRef.from(riskDefinition.riskValues[2])
        riskValueUltra = RiskRef.from(riskDefinition.riskValues[3])

        scenarioA = newScenario(unit)
        scenarioB = newScenario(unit)
    }

    def "cannot determine result without any risks"() {
        given:
        def process = createProcess([
            process_privacyImpactAssessment_listed: "process_privacyImpactAssessment_listed_positive",
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value == null
            decisiveRule.index == 0
            matchingRules*.index == [0, 5]
            agreeingRules*.index == [0]
        }
    }

    def "cannot determine result with a risk without any risk values"() {
        given:
        def process = createProcess([
            process_privacyImpactAssessment_listed: "process_privacyImpactAssessment_listed_positive",
        ])
        addRisk(process, [:])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value == null
            decisiveRule.index == 0
            matchingRules*.index == [0, 5]
            agreeingRules*.index == [0]
        }
    }

    def "cannot determine result with a null risk value"() {
        given:
        def process = createProcess([
            process_privacyImpactAssessment_listed: "process_privacyImpactAssessment_listed_positive",
        ])
        addRisk(process, [
            (riskCategoryC): null
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value == null
            decisiveRule.index == 0
            matchingRules*.index == [0, 5]
            agreeingRules*.index == [0]
        }
    }

    def "positive listing makes pia mandatory"() {
        given:
        def process = createProcess([
            process_privacyImpactAssessment_listed: "process_privacyImpactAssessment_listed_positive",
        ])
        addRisk(process, [
            (riskCategoryC): riskValueLow
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value
            decisiveRule.index == 5
            matchingRules*.index == [5]
            agreeingRules*.index == [5]
        }
    }

    def "cannot determine result with one criterion"() {
        given:
        def process = createProcess([
            process_privacyImpactAssessment_processingCriteria: [
                "process_privacyImpactAssessment_processingCriteria_automated",
            ],
        ])
        addRisk(process, [
            (riskCategoryC): riskValueLow
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value == null
            decisiveRule == null
            matchingRules*.index == []
            agreeingRules*.index == []
        }
    }

    def "two criteria make pia mandatory"() {
        given:
        def process = createProcess([
            process_privacyImpactAssessment_processingCriteria: [
                "process_privacyImpactAssessment_processingCriteria_automated",
                "process_privacyImpactAssessment_processingCriteria_specialCategories"
            ],
        ])
        addRisk(process, [
            (riskCategoryC): riskValueLow
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value
            decisiveRule.index == 6
            matchingRules*.index == [6]
            agreeingRules*.index == [6]
        }
    }

    def "three criteria make pia mandatory"() {
        given:
        def process = createProcess([
            process_privacyImpactAssessment_processingCriteria: [
                "process_privacyImpactAssessment_processingCriteria_automated",
                "process_privacyImpactAssessment_processingCriteria_vulnerability",
                "process_privacyImpactAssessment_processingCriteria_newTechnology",
            ],
        ])
        addRisk(process, [
            (riskCategoryC): riskValueLow
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value
            decisiveRule.index == 6
            matchingRules*.index == [6]
            agreeingRules*.index == [6]
        }
    }

    def "high risk makes pia mandatory"() {
        given:
        def process = createProcess([:])
        addRisk(process, [
            (riskCategoryC): riskValueLow
        ])
        addRisk(process, [
            (riskCategoryI): riskValueHigh
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value
            decisiveRule.index == 4
            matchingRules*.index == [4]
            agreeingRules*.index == [4]
        }
    }

    def "ultra high risk makes pia mandatory"() {
        given:
        def process = createProcess([:])
        addRisk(process, [
            (riskCategoryC): riskValueUltra
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value
            decisiveRule.index == 4
            matchingRules*.index == [4]
            agreeingRules*.index == [4]
        }
    }

    def "PIA not required with negative list and high risk"() {
        given:
        def process = createProcess([
            process_privacyImpactAssessment_listed: "process_privacyImpactAssessment_listed_negative",
        ])
        addRisk(process, [
            (riskCategoryC): riskValueHigh
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value == false
            decisiveRule.index == 1
            matchingRules*.index == [1, 4]
            agreeingRules*.index == [1]
        }
    }

    def "cannot determine result with low and medium risks and no attributes"() {
        given:
        def process = createProcess([:])
        addRisk(process, [
            (riskCategoryC): riskValueLow,
            (riskCategoryI): riskValueMedium
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value == null
            decisiveRule == null
            matchingRules*.index == []
            agreeingRules*.index == []
        }
    }

    def "multiple agreeing rules are reported"() {
        given:
        def process = createProcess([
            process_privacyImpactAssessment_listed: "process_privacyImpactAssessment_listed_negative",
            process_privacyImpactAssessment_processingOperationAccordingArt35: true,
            process_privacyImpactAssessment_otherExclusions: true,
        ])
        addRisk(process, [
            (riskCategoryC): riskValueHigh
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value == false
            decisiveRule.index == 1
            matchingRules*.index == [1, 2, 3, 4]
            agreeingRules*.index == [1, 2, 3]
        }
    }

    def "art 35 makes pia optional"() {
        when:
        def process = createProcess([
            process_privacyImpactAssessment_processingOperationAccordingArt35: true,
            process_privacyImpactAssessment_listed: "process_privacyImpactAssessment_listed_positive",
        ])
        addRisk(process, [
            (riskCategoryC): riskValueLow
        ])
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value == false
            decisiveRule.index == 2
            matchingRules*.index == [2, 5]
            agreeingRules*.index == [2]
        }
    }

    def "other exclusions make pia optional"() {
        when:
        def process = createProcess([
            process_privacyImpactAssessment_otherExclusions: true,
            process_privacyImpactAssessment_listed: "process_privacyImpactAssessment_listed_positive",
        ])
        addRisk(process, [
            (riskCategoryC): riskValueLow
        ])
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value == false
            decisiveRule.index == 3
            matchingRules*.index == [3, 5]
            agreeingRules*.index == [3]
        }
    }

    def "no pia custom aspect means no result"() {
        given:
        def process = createProcess([:])
        process.customAspects = []
        addRisk(process, [
            (riskCategoryC): riskValueLow
        ])

        when:
        def decisions = decider.decide(process, domain)

        then:
        with(decisions[piaMandatoryRef]) {
            value == null
            decisiveRule == null
            matchingRules*.index == []
            agreeingRules*.index == []
        }
    }

    Process createProcess(Map<String, Object> piaAttributes) {
        return newProcess(unit) {
            domains = [domain]
            setSubType(domain, "PRO_DataProcessing", "NEW")
            addToCustomAspects(newCustomAspect("process_privacyImpactAssessment") {
                attributes = piaAttributes
            })
        }
    }

    private ProcessRisk addRisk(Process process, Map<CategoryRef, RiskRef> inherentRisks) {
        process.obtainRisk(scenarioA, domain).tap {
            defineRiskValues([
                newRiskValues(riskDefinitionRef, domain)
            ] as Set)
            getRiskProvider(riskDefinitionRef).tap { CategorizedRiskValueProvider riskValueProvider ->
                inherentRisks.forEach{category, riskValue ->
                    riskValueProvider.categorizedRisks
                            .find { it.category == category }
                            .with { it as DeterminedRiskImpl }
                            .setInherentRisk(riskValue)
                }
            }
        }
    }
}
