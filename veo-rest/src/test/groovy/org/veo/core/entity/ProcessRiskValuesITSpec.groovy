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
package org.veo.core.entity

import static org.veo.core.entity.risk.RiskTreatmentOption.RISK_TREATMENT_ACCEPTANCE
import static org.veo.core.entity.risk.RiskTreatmentOption.RISK_TREATMENT_REDUCTION

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.annotation.Transactional

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.ImpactRef
import org.veo.core.entity.risk.ProbabilityRef
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.entity.risk.RiskRef
import org.veo.core.entity.riskdefinition.RiskDefinition
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@WithUserDetails("user@domain.example")
class ProcessRiskValuesITSpec extends VeoSpringSpec {

    public static final String BAD_FEELINGS = "I have a bad feeling about this"
    public static final String MURPHYS_LAW = "Anything that can go wrong will go wrong"
    public static final String RISK_ACCEPTANCE = "https://www.youtube.com/watch?v=9IG3zqvUqJY&t=157s"
    public static final String NO_RISK_NO_FUN = "No risk no fun. Our only problem: way too much fun!"

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    ProcessRepositoryImpl processRepository

    @Autowired
    ScenarioRepositoryImpl scenarioRepository

    @Autowired
    DomainRepositoryImpl domainRepository

    Client client

    Unit unit

    public static final String RISK_DEFINITION_ID = "r2d2"

    RiskDefinition riskDefinition

    Domain domain

    def setup() {
        createClient()
        this.riskDefinition = createRiskDefinition(RISK_DEFINITION_ID)
        this.domain = newDomain(client) {
            it.riskDefinitions = [ "r2d2":
                createRiskDefinition(RISK_DEFINITION_ID)
            ] as Map
        }
    }

    def "risk values can be modified"() {
        given: "A risk definition, a process with a risk and a scenario"
        def riskDef = domain.getRiskDefinitions().values().first()
        def riskDefRef = RiskDefinitionRef.from(riskDef)
        def extentOfDamage = new CategoryRef("D")

        def scenario1 = scenarioRepository.save(newScenario(unit))
        domain = domainRepository.save(domain)
        ProcessRisk risk
        processRepository.save(newProcess(unit) {
            associateWithDomain(domain, "PRO_DataProcessing", "NEW")
            risk = obtainRisk(scenario1).tap {
                designator = "RSK-1"
            }
        })

        when: "the risk is retrieved"
        ProcessRisk retrievedRisk1 = txTemplate.execute{
            Set<Process> processes = processRepository.findRisksWithValue(scenario1)
            def processRisk = processes.first().risks.first()
            return processRisk
        }

        then: "risk values are empty"
        retrievedRisk1 == risk
        retrievedRisk1.getRiskDefinitions(domain).size() == 0

        when: "blank risk values are added for a risk definition"
        txTemplate.execute{
            var process = processRepository.findById(retrievedRisk1.entity.id).orElseThrow()
            process.risks.first().defineRiskValues([
                newRiskValues(riskDefRef, domain)
            ] as Set)
            processRepository.save(process)
        }

        and: "the risk is retrieved"
        retrievedRisk1 = txTemplate.execute{
            Set<Process> processes = processRepository.findRisksWithValue(scenario1)
            return processes.first().risks.first().tap{
                it.getRiskDefinitions(domain)
            }
        }

        then: "risk values were initialized"
        retrievedRisk1 == risk
        retrievedRisk1.getRiskDefinitions(domain).size() == 1
        retrievedRisk1.getProbabilityProvider(riskDefRef, domain).getProbability() != null
        retrievedRisk1.getImpactProvider(riskDefRef, domain) != null
        retrievedRisk1.getRiskProvider(riskDefRef, domain) != null

        when: "risk values are modified"
        txTemplate.execute{
            def process = processRepository.findRisksWithValue(scenario1).first()
            process.risks.first().getProbabilityProvider(riskDefRef, domain).with {
                potentialProbability = new ProbabilityRef(2)
                specificProbability = new ProbabilityRef(1)
                specificProbabilityExplanation = BAD_FEELINGS
            }

            process.risks.first().getImpactProvider(riskDefRef, domain).with {
                it.setPotentialImpact(extentOfDamage, new ImpactRef(1))
                it.setSpecificImpact(extentOfDamage, new ImpactRef(3))
                it.setSpecificImpactExplanation(extentOfDamage, MURPHYS_LAW)
            }

            process.risks.first().getRiskProvider(riskDefRef, domain).with {
                it.setUserDefinedResidualRisk(extentOfDamage, new RiskRef(0))
                it.setResidualRiskExplanation(extentOfDamage, NO_RISK_NO_FUN)
                it.setRiskTreatmentExplanation(extentOfDamage, RISK_ACCEPTANCE)
                it.setRiskTreatments(extentOfDamage, [
                    RISK_TREATMENT_ACCEPTANCE,
                    RISK_TREATMENT_REDUCTION
                ] as Set )
            }
        }

        // retrieve in new transaction:
        def retrievedRisk2 = txTemplate.execute {
            processRepository.findRisksWithValue(scenario1).first().risks.first().tap{
                it.getRiskDefinitions(domain)
            }
        }

        then: "the persisted object reflects the changes"
        with(retrievedRisk2.getProbabilityProvider(riskDefRef, domain)) {
            it.potentialProbability == new ProbabilityRef(2)
            it.specificProbability == new ProbabilityRef(1)
            it.effectiveProbability == new ProbabilityRef(1)
            it.specificProbabilityExplanation == BAD_FEELINGS
        }

        with(retrievedRisk2.getImpactProvider(riskDefRef, domain)) {
            it.getPotentialImpact(extentOfDamage) == new ImpactRef(1)
            it.getSpecificImpact(extentOfDamage) == new ImpactRef(3)
            it.getSpecificImpactExplanation(extentOfDamage) == MURPHYS_LAW
        }

        with(retrievedRisk2.getRiskProvider(riskDefRef, domain)) {
            it.getUserDefinedResidualRisk(extentOfDamage) ==  new RiskRef(0)
            it.getResidualRiskExplanation(extentOfDamage) == NO_RISK_NO_FUN
            it.getRiskTreatmentExplanation(extentOfDamage) == RISK_ACCEPTANCE
            it.getRiskTreatments(extentOfDamage) == [
                RISK_TREATMENT_ACCEPTANCE,
                RISK_TREATMENT_REDUCTION
            ] as Set
        }
    }

    @Transactional
    void createClient() {
        client = clientRepository.save(newClient())
        def domain = domainRepository.save(newDomain(client))

        unit = unitRepository.save(newUnit(client) {
            addToDomains(domain)
        })
    }
}
