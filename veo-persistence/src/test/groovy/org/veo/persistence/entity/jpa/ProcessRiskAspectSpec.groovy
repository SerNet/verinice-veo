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

import spock.lang.Ignore

class ProcessRiskAspectSpec extends VeoSpec {


    Client client
    Unit unit
    public static final String RISK_DEF_ID = "r2d2"

    RiskReferenceFactory factory = RiskReferenceFactory.getInstance()
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

    @Ignore
    // FIXME VEO-1105
    def "A risk aspect must match its containing scope"() {
        given: "a scope with a referenced risk definition"
        when: "a risk aspect is added to the process"
        then: "the aspect was added"
    }

    @Ignore
    // FIXME VEO-1105
    def "A risk aspect cannot be added outside of scope"() {
        given: "a scope without reference to a risk definition"
        when: "a risk aspect is added to the process"
        then: "the aspect cannot be added"
    }

    def "The risk object contains the applicable impact categories"() {
        given: "a process risk"
        def process = newProcess(unit)
        process.addToDomains(domain)

        def rd = domain.getRiskDefinitions().values().first()
        def rdRef = RiskDefinitionRef.from(rd)
        def risk = createRisk(process, domain)

        when: "get the list of impact categories"
        def categories = risk.getImpactProvider(rdRef).getAvailableCategories()

        then: "the expected categories are returned"
        categories*.idRef.toSet() == ["C", "I", "A", "R"] as Set
    }

    def "Potential probability/impact #potProb/#potC and specific values #specProb/#specC are evaluated to effective values: #effProb/#effC"() {
        given: "a process risk"
        def process = newProcess(unit)
        process.addToDomains(domain)
        def rd = domain.getRiskDefinitions().values().first()
        def rdRef = RiskDefinitionRef.from(rd)
        def risk = createRisk(process, domain)

        when: "default and specific values are set"
        def probability = risk.getProbabilityProvider(rdRef)
        probability.potentialProbability = factory.createProbabilityRef(potProb)
        probability.specificProbability = factory.createProbabilityRef(specProb)

        def confidentiality = factory.createCategoryRef("C")
        def impact = risk.getImpactProvider(rdRef)
        impact.setPotentialImpact(confidentiality, factory.createImpactRef(potC))
        impact.setSpecificImpact(confidentiality, factory.createImpactRef(specC))

        then: "specific values override the default ones"
        probability.effectiveProbability == factory.createProbabilityRef(effProb)
        impact.getEffectiveImpact(confidentiality) == factory.createImpactRef(effC)

        where:
        potProb   | specProb   | effProb   | potC   | specC   | effC
        0         | null       | 0         | 0      | null    | 0
        null      | 1          | 1         | null   | 2       | 2
        0         | 1          | 1         | 0      | 1       | 1
        3         | 2          | 2         | 0      | 1       | 1
        2         | 2          | 2         | 3      | 3       | 3
    }

    private createRisk(Process process, Domain domain) {
        def scenario = newScenario(unit)
        def control = newControl(unit)
        process.addToDomains(domain)
        def risk = process.obtainRisk(scenario, domain)
        risk = risk.mitigate(control)
        risk
    }


}
