/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.persistence.entity.jpa

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.entity.riskdefinition.RiskDefinition
import org.veo.test.VeoSpec

class ProcessRiskAspectSpec extends VeoSpec {

    Client client
    Unit unit
    public static final String RISK_DEF_ID = "r2d2"

    RiskReferenceFactoryImpl factory = RiskReferenceFactoryImpl.getInstance()
    RiskDefinition riskDefinition
    Domain domain

    def setup() {
        this.client = newClient()
        this.unit = newUnit(client)
        this.riskDefinition = createRiskDefinition(RISK_DEF_ID)
        this.domain = newDomain(client) {
            it.riskDefinitions = [ "r2d2":
                createRiskDefinition(RISK_DEF_ID)
            ] as Map
        }
    }

    def "The risk object contains the applicable impact categories"() {
        given: "a process risk"
        def process = newProcess(unit)
        process.associateWithDomain(domain, "NormalProcess", "NEW")

        def rd = domain.getRiskDefinitions().values().first()
        def rdRef = RiskDefinitionRef.from(rd)
        def risk = createRisk(process, domain, rdRef)

        when: "get the list of impact categories"
        def categories = risk.getImpactProvider(rdRef, domain).getAvailableCategories()

        then: "the expected categories are returned"
        categories*.idRef ==~ ["D"]
    }

    def "Potential probability/impact #potProb/#potD and specific values #specProb/#specD are evaluated to effective values: #effProb/#effD"() {
        given: "a process risk"
        def process = newProcess(unit)
        process.associateWithDomain(domain, "NormalProcess", "NEW")
        def rd = domain.getRiskDefinitions().values().first()
        def rdRef = RiskDefinitionRef.from(rd)
        def risk = createRisk(process, domain, rdRef)

        when: "default and specific values are set"
        def probability = risk.getProbabilityProvider(rdRef, domain)
        probability.potentialProbability = factory.createProbabilityRef(potProb)
        probability.specificProbability = factory.createProbabilityRef(specProb)

        def confidentiality = factory.createCategoryRef("D")
        def impact = risk.getImpactProvider(rdRef, domain)
        impact.setPotentialImpact(confidentiality, factory.createImpactRef(potD))
        impact.setSpecificImpact(confidentiality, factory.createImpactRef(specD))

        then: "specific values override the default ones"
        probability.effectiveProbability == factory.createProbabilityRef(effProb)
        impact.getEffectiveImpact(confidentiality) == factory.createImpactRef(effD)

        where:
        potProb   | specProb   | effProb   | potD   | specD   | effD
        0         | null       | 0         | 0      | null    | 0
        null      | 1          | 1         | null   | 2       | 2
        0         | 1          | 1         | 0      | 1       | 1
        3         | 2          | 2         | 0      | 1       | 1
        2         | 2          | 2         | 3      | 3       | 3
    }

    private createRisk(Process process, Domain domain, RiskDefinitionRef rdRef) {
        def scenario = newScenario(unit)
        def control = newControl(unit)
        def risk = process.obtainRisk(scenario)
        risk = risk.mitigate(control)
        risk.defineRiskValues([
            newRiskValues(rdRef, domain)
        ] as Set)
        risk
    }
}
