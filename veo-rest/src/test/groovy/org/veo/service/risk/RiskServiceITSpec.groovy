/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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
package org.veo.service.risk

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Process
import org.veo.core.entity.ProcessRisk
import org.veo.core.entity.Scenario
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.ImpactRef
import org.veo.core.entity.risk.PotentialProbabilityImpl
import org.veo.core.entity.risk.ProbabilityRef
import org.veo.core.entity.risk.ProcessImpactValues
import org.veo.core.entity.risk.RiskDefinitionRef

@WithUserDetails("user@domain.example")
class RiskServiceITSpec extends VeoSpringSpec {

    private static final String BETRAECHTLICH = 'beträchtlich'
    private static final String EXISTENZBEDROHEND = 'existenzbedrohend'
    private static final String GERING = 'gering'
    private static final String HOCH = 'hoch'
    private static final String MITTEL = 'mittel'
    private static final String SEHR_HOCH = 'sehr hoch'
    private static final String VERNACHLAESSIGBAR = 'vernachlässigbar'


    @Autowired
    RiskService riskService

    def "Calculate risk values for process"() {
        given:
        def client = createTestClient()
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        def riskDefinitionId = 'DSRA'
        def riskDefinition = domain.riskDefinitions.get(riskDefinitionId)
        def confidentiality = riskDefinition.getCategory('C').orElseThrow()
        def confidentialityRef = CategoryRef.from(confidentiality)
        def availability = riskDefinition.getCategory('A').orElseThrow()
        def availabilityRef = CategoryRef.from(availability)
        def riskDefinitionRef = RiskDefinitionRef.from(riskDefinition)

        def unit = unitDataRepository.save(newUnit(client))
        ProcessImpactValues processImpactValues = new ProcessImpactValues()
        def lowConfidentialityImpact = confidentiality.getLevel(0).orElseThrow()
        def limitedAvailabilityImpact = availability.getLevel(2).orElseThrow()
        def highAvailabilityImpact = availability.getLevel(3).orElseThrow()
        def probabilityRare = ProbabilityRef.from(riskDefinition.getProbability().getLevel(0).orElseThrow())
        def probabilityOften = ProbabilityRef.from(riskDefinition.getProbability().getLevel(2).orElseThrow())

        processImpactValues.potentialImpacts = [
            (confidentialityRef) : ImpactRef.from(lowConfidentialityImpact),
            (availabilityRef): ImpactRef.from(limitedAvailabilityImpact)
        ]
        Map impactValues = [
            (riskDefinitionRef) : processImpactValues
        ]
        Process process = newProcess(unit) {
            addToDomains(domain)
            setImpactValues(domain, impactValues)
        }

        Scenario scenario = scenarioDataRepository.save(newScenario(unit) {
            addToDomains(domain)
            setPotentialProbability(domain, [(riskDefinitionRef): new PotentialProbabilityImpl().tap {
                    potentialProbability = probabilityRare
                }])
        })
        ProcessRisk risk = process.obtainRisk(scenario, domain).tap {
            assignDesignator(it)
            defineRiskValues([
                newRiskValues(riskDefinitionRef, domain)
            ] as Set)
        }
        process = processDataRepository.save(process)
        when: 'running the risk service on the changed process'
        executeInTransaction {
            riskService.evaluateChangedRiskComponent(process)
        }
        risk = executeInTransaction {
            process = processDataRepository.findByIdsWithRiskValues(Set.of(process.idAsString)).first()
            process.risks.first()
        }
        then: 'the effective and inherent risk values are set'
        with(risk.getProbabilityProvider(riskDefinitionRef)) {
            effectiveProbability.idRef == 0
        }
        with(risk.getImpactProvider(riskDefinitionRef)) {
            with (getEffectiveImpact(confidentialityRef)) {
                it.idRef == 0
                confidentiality.getLevel(it.idRef as int).orElseThrow().name == VERNACHLAESSIGBAR
            }
            with (getEffectiveImpact(availabilityRef)) {
                it.idRef == 2
                availability.getLevel(it.idRef as int).orElseThrow().name == BETRAECHTLICH
            }
        }
        with(risk.getRiskProvider(riskDefinitionRef)) {
            with(getInherentRisk(confidentialityRef)) {
                it.idRef == 0
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == GERING
            }
            with(getInherentRisk(availabilityRef)) {
                it.idRef == 1
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == MITTEL
            }
        }
        when: "changing the scenario's potential probability and running the risk service"
        executeInTransaction {
            scenario = scenarioDataRepository.findById(scenario.idAsString).get().tap {
                riskValuesAspects.first().potentialProbability[riskDefinitionRef].potentialProbability = probabilityOften
            }
            process = processDataRepository.findById(process.idAsString).get()
            riskService.evaluateChangedRiskComponent(process)
        }
        executeInTransaction {
            risk = executeInTransaction {
                process = processDataRepository.findById(process.idAsString).get()
                process.risks.first().tap {
                    //initialize lazy associations
                    it.riskDefinitions
                }
            }
        }
        then: 'the effective and inherent risk values are updated accordingly'
        with(risk.getProbabilityProvider(riskDefinitionRef)) {
            effectiveProbability.idRef == 2
        }
        with(risk.getImpactProvider(riskDefinitionRef)) {
            with (getEffectiveImpact(confidentialityRef)) {
                it.idRef == 0
                confidentiality.getLevel(it.idRef as int).orElseThrow().name == VERNACHLAESSIGBAR
            }
            with (getEffectiveImpact(availabilityRef)) {
                it.idRef == 2
                availability.getLevel(it.idRef as int).orElseThrow().name == BETRAECHTLICH
            }
        }
        with(risk.getRiskProvider(riskDefinitionRef)) {
            with(getInherentRisk(confidentialityRef)) {
                it.idRef == 0
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == GERING
            }
            with(getInherentRisk(availabilityRef)) {
                it.idRef == 2
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == HOCH
            }
        }
        when: "changing the risk's specific impact and running the risk service"
        risk.getImpactProvider(riskDefinitionRef).setSpecificImpact(availabilityRef, ImpactRef.from(highAvailabilityImpact))
        executeInTransaction {
            process = processDataRepository.save(process)
            riskService.evaluateChangedRiskComponent(process)
            process = processDataRepository.save(process)
            risk = process.risks.first()
        }
        then: 'the effective and inherent risk values are updated accordingly'
        with(risk.getProbabilityProvider(riskDefinitionRef)) {
            effectiveProbability.idRef == 2
        }
        with(risk.getImpactProvider(riskDefinitionRef)) {
            with (getEffectiveImpact(confidentialityRef)) {
                it.idRef == 0
                confidentiality.getLevel(it.idRef as int).orElseThrow().name == VERNACHLAESSIGBAR
            }
            with (getEffectiveImpact(availabilityRef)) {
                it.idRef == 3
                availability.getLevel(it.idRef as int).orElseThrow().name == EXISTENZBEDROHEND
            }
        }
        with(risk.getRiskProvider(riskDefinitionRef)) {
            with(getInherentRisk(confidentialityRef)) {
                it.idRef == 0
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == GERING
            }
            with(getInherentRisk(availabilityRef)) {
                it.idRef == 3
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == SEHR_HOCH
            }
        }
    }
}
