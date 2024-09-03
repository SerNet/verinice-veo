/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade.
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
package org.veo.persistence.entity

import org.veo.core.entity.Client
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.DeterminedRiskImpl
import org.veo.core.entity.risk.ImpactImpl
import org.veo.core.entity.risk.ImpactRef
import org.veo.core.entity.risk.ProbabilityImpl
import org.veo.core.entity.risk.ProbabilityRef
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.entity.risk.RiskRef
import org.veo.core.entity.riskdefinition.CategoryLevel
import org.veo.core.entity.riskdefinition.ProbabilityLevel
import org.veo.core.entity.riskdefinition.RiskValue
import org.veo.persistence.entity.jpa.ScopeRiskData
import org.veo.test.VeoSpec

class ScopeRiskSpec extends VeoSpec {

    Client client
    Unit unit

    def setup() {
        this.client = newClient()
        this.unit = newUnit(client)
    }

    def "risk values for risk categories without a matrix are ignored"() {
        given: "a scope, a scenario and a risk definition with two categories, one of them not supporting risks"
        def riskDefId = "r"
        def domain = newDomain(client) {
            riskDefinitions = [
                (riskDefId): newRiskDefinition(riskDefId) {
                    riskValues = [
                        new RiskValue(0,"","low"),
                        new RiskValue(1,"","high")
                    ]
                    categories = [
                        newCategoryDefinition("C") {
                            potentialImpacts = [
                                new CategoryLevel(),
                                new CategoryLevel()
                            ]
                            valueMatrix = null
                        },
                        newCategoryDefinition("D") {
                            potentialImpacts = [
                                new CategoryLevel(),
                                new CategoryLevel()
                            ]
                            valueMatrix = []
                        }
                    ]
                    probability.levels = [
                        new ProbabilityLevel("unlikely"),
                        new ProbabilityLevel("likely"),
                    ]
                }
            ]
        }
        def scope = newScope(unit) {
            associateWithDomain(domain, "Multiverse", "NEW")
        }
        def scenario = newScenario(unit)

        when: "trying to update the risk with values for both categories"
        def risk = scope.obtainRisk(scenario, domain)

        def riskDefRef = new RiskDefinitionRef(riskDefId)
        def catC = new CategoryRef("C")
        def catD = new CategoryRef("D")

        scope.updateRisk(risk, [domain] as Set, null, null, [
            newRiskValues(riskDefRef, domain) {
                probability = new ProbabilityImpl().tap{
                    specificProbability = new ProbabilityRef(BigDecimal.valueOf(1))
                }
                impactCategories = [
                    new ImpactImpl(catC).tap{
                        specificImpact = new ImpactRef(BigDecimal.valueOf(1))
                    },
                    new ImpactImpl(catD).tap{
                        specificImpact = new ImpactRef(BigDecimal.valueOf(1))
                    }
                ]
                categorizedRisks = [
                    new DeterminedRiskImpl(catC).tap{
                        userDefinedResidualRisk = new RiskRef(BigDecimal.valueOf(1))
                    },
                    new DeterminedRiskImpl(catD).tap{
                        userDefinedResidualRisk = new RiskRef(BigDecimal.valueOf(1))
                    }
                ]
            },
        ] as Set)

        then: "the risk values for category C are silently ignored"
        with (risk as ScopeRiskData) {
            riskAspects.size() == 1
            with (riskAspects.first()) {
                impactCategories*.category*.idRef == ['D']
                riskCategories*.category*.idRef == ['D']
            }
        }

        when: "retrieving the specific probability and the values for category D"
        def specifProbability = risk.getProbabilityProvider(riskDefRef, domain).getSpecificProbability().idRef.longValue()
        def specificImpactD = risk.getImpactProvider(riskDefRef, domain).getSpecificImpact(catD).idRef.longValue()
        def risidualRiskD = risk.getRiskProvider(riskDefRef, domain).getUserDefinedResidualRisk(catD).idRef.longValue()

        then: "the values are correct"
        specifProbability == 1
        specificImpactD == 1
        risidualRiskD == 1

        when: "trying to retrieve impact values for category C"
        risk.getImpactProvider(riskDefRef, domain).getSpecificImpact(catC)

        then: "an exception is thrown"
        NotFoundException e = thrown()
        e.message == 'Impact category C does not exist for for risk aspect.'

        when: "trying to retrieve risk values for category C"
        risk.getRiskProvider(riskDefRef, domain).getUserDefinedResidualRisk(catC)

        then: "an exception is thrown"
        e = thrown()
        e.message == 'Risk category C does not exist for risk aspect.'
    }
}
