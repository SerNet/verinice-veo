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


import static org.veo.core.entity.event.RiskEvent.ChangedValues.PROBABILITY_VALUES_CHANGED
import static org.veo.core.entity.event.RiskEvent.ChangedValues.RISK_VALUES_CHANGED

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ContextConfiguration

import org.veo.core.AbstractPerformaceITSpec
import org.veo.core.entity.Process
import org.veo.core.entity.ProcessRisk
import org.veo.core.entity.Scenario
import org.veo.core.entity.event.RiskAffectingElementChangeEvent
import org.veo.core.entity.event.RiskChangedEvent
import org.veo.core.entity.event.RiskEvent
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.ImpactRef
import org.veo.core.entity.risk.PotentialProbabilityImpl
import org.veo.core.entity.risk.ProbabilityRef
import org.veo.core.entity.risk.ProcessImpactValues
import org.veo.core.entity.risk.RiskDefinitionRef

import net.ttddyy.dsproxy.QueryCountHolder

@WithUserDetails("user@domain.example")
@ContextConfiguration(classes = Config.class)
class RiskServiceITSpec extends AbstractPerformaceITSpec  {

    private static final String BETRAECHTLICH = 'beträchtlich'
    private static final String EXISTENZBEDROHEND = 'existenzbedrohend'
    private static final String GERING = 'gering'
    private static final String HOCH = 'hoch'
    private static final String MITTEL = 'mittel'
    private static final String SEHR_HOCH = 'sehr hoch'
    private static final String VERNACHLAESSIGBAR = 'vernachlässigbar'

    @Autowired
    RiskService riskService
    @Autowired
    RiskEventListener listener

    static class Config {
        @Bean
        public RiskEventListener listener() {
            return new RiskEventListener()
        }
    }

    static class RiskEventListener {
        def receivedEvents = new ArrayList<RiskEvent>()

        @EventListener
        def listen(RiskEvent event) {
            receivedEvents.add(event)
        }
    }

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
            associateWithDomain(domain, "NormalProcess", "NEW")
            setImpactValues(domain, impactValues)
        }

        Scenario scenario = scenarioDataRepository.save(newScenario(unit) {
            associateWithDomain(domain, "NormalScenario", "NEW")
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
        QueryCountHolder.clear()

        when: 'running the risk service on the changed process'
        executeInTransaction {
            riskService.evaluateChangedRiskComponent(processDataRepository.findById(process.idAsString).orElseThrow())
        }
        def queryCounts = QueryCountHolder.grandTotal

        risk = executeInTransaction {
            process = processDataRepository.findByIdsWithRiskValues(Set.of(process.idAsString)).first()
            process.risks.first().tap {
                it.getRiskDefinitions(domain)
            }
        }
        def oldRiskVersion = risk.version

        then: 'the effective and inherent risk values are set'
        with(risk.getProbabilityProvider(riskDefinitionRef, domain)) {
            effectiveProbability.idRef == 0
        }
        with(risk.getImpactProvider(riskDefinitionRef, domain)) {
            with (getEffectiveImpact(confidentialityRef)) {
                it.idRef == 0
                confidentiality.getLevel(it.idRef as int).orElseThrow().name == VERNACHLAESSIGBAR
            }
            with (getEffectiveImpact(availabilityRef)) {
                it.idRef == 2
                availability.getLevel(it.idRef as int).orElseThrow().name == BETRAECHTLICH
            }
        }
        with(risk.getRiskProvider(riskDefinitionRef, domain)) {
            with(getInherentRisk(confidentialityRef)) {
                it.idRef == 0
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == GERING
            }
            with(getInherentRisk(availabilityRef)) {
                it.idRef == 1
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == MITTEL
            }
        }
        oldRiskVersion == 1

        and: "events were published as required"
        listener.receivedEvents.size() == 2

        def riskValueEvent = listener.receivedEvents.find { it instanceof RiskChangedEvent }
        with (riskValueEvent) {
            def event = it as RiskChangedEvent
            event.source.class == RiskService
            event.riskAffectedId == process.id
            event.scenarioId == scenario.id
            event.changes ==~ [RISK_VALUES_CHANGED]
            event.domainId == domain.id
            event.riskDefinition == riskDefinitionRef
            event.clientId == client.id
        }

        with (listener.receivedEvents.find { it instanceof RiskAffectingElementChangeEvent }) {
            def event = it as RiskAffectingElementChangeEvent
            event.entityType == Process
            event.entityId == process.id
            event.source.class == RiskService
            event.hasChangedRisks()
            event.clientId == client.id
            event.changedRisks ==~ [riskValueEvent]
        }
        and: "the DB operations are within reasonable limits"
        verifyAll {
            queryCounts.select == 6
            queryCounts.insert == 1
            queryCounts.update == 2
            queryCounts.time < 500
        }

        when: "changing the scenario's potential probability and running the risk service"
        listener.receivedEvents.clear()
        executeInTransaction {
            scenario = scenarioDataRepository.findById(scenario.idAsString).get().tap {
                riskValuesAspects.first().potentialProbability[riskDefinitionRef].potentialProbability = probabilityOften
            }
            process = processDataRepository.findById(process.idAsString).get()
            QueryCountHolder.clear()
            riskService.evaluateChangedRiskComponent(process)
        }
        queryCounts = QueryCountHolder.grandTotal
        executeInTransaction {
            risk = executeInTransaction {
                process = processDataRepository.findById(process.idAsString).get()
                process.risks.first().tap {
                    //initialize lazy associations
                    it.getRiskDefinitions(domain)
                }
            }
        }

        then: 'the effective and inherent risk values are updated accordingly'
        with(risk.getProbabilityProvider(riskDefinitionRef, domain)) {
            effectiveProbability.idRef == 2
        }
        with(risk.getImpactProvider(riskDefinitionRef, domain)) {
            with (getEffectiveImpact(confidentialityRef)) {
                it.idRef == 0
                confidentiality.getLevel(it.idRef as int).orElseThrow().name == VERNACHLAESSIGBAR
            }
            with (getEffectiveImpact(availabilityRef)) {
                it.idRef == 2
                availability.getLevel(it.idRef as int).orElseThrow().name == BETRAECHTLICH
            }
        }
        with(risk.getRiskProvider(riskDefinitionRef, domain)) {
            with(getInherentRisk(confidentialityRef)) {
                it.idRef == 0
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == GERING
            }
            with(getInherentRisk(availabilityRef)) {
                it.idRef == 2
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == HOCH
            }
        }

        and: "events were published as required"
        listener.receivedEvents.size() == 2

        def riskProbabilityEvent = listener.receivedEvents.find { it instanceof RiskChangedEvent }
        with (riskProbabilityEvent) {
            def event = it as RiskChangedEvent
            event.source.class == RiskService
            event.changes ==~ [
                PROBABILITY_VALUES_CHANGED,
                RISK_VALUES_CHANGED
            ]
            event.riskAffectedId == process.id
            event.scenarioId == scenario.id
            event.domainId == domain.id
            event.riskDefinition == riskDefinitionRef
            event.clientId == client.id
        }

        with (listener.receivedEvents.find { it instanceof RiskAffectingElementChangeEvent }) {
            def event = it as RiskAffectingElementChangeEvent
            event.entityType == Process
            event.entityId == process.id
            event.source.class == RiskService
            event.hasChangedRisks()
            event.clientId == client.id
            event.changedRisks ==~ [riskProbabilityEvent]
        }
        and: "the DB operations are within reasonable limits"
        verifyAll {
            queryCounts.select == 5
            queryCounts.insert == 1
            queryCounts.update == 3
            queryCounts.time < 500
        }

        when: "changing the risk's specific impact and running the risk service"
        listener.receivedEvents.clear()
        risk.getImpactProvider(riskDefinitionRef, domain).setSpecificImpact(availabilityRef, ImpactRef.from(highAvailabilityImpact))
        executeInTransaction {
            process = processDataRepository.save(process)
            QueryCountHolder.clear()
            riskService.evaluateChangedRiskComponent(process)
            queryCounts = QueryCountHolder.grandTotal
            process = processDataRepository.save(process)
            risk = process.risks.first()
        }

        then: 'the effective and inherent risk values are updated accordingly'
        with(risk.getProbabilityProvider(riskDefinitionRef, domain)) {
            effectiveProbability.idRef == 2
        }
        with(risk.getImpactProvider(riskDefinitionRef, domain)) {
            with (getEffectiveImpact(confidentialityRef)) {
                it.idRef == 0
                confidentiality.getLevel(it.idRef as int).orElseThrow().name == VERNACHLAESSIGBAR
            }
            with (getEffectiveImpact(availabilityRef)) {
                it.idRef == 3
                availability.getLevel(it.idRef as int).orElseThrow().name == EXISTENZBEDROHEND
            }
        }
        with(risk.getRiskProvider(riskDefinitionRef, domain)) {
            with(getInherentRisk(confidentialityRef)) {
                it.idRef == 0
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == GERING
            }
            with(getInherentRisk(availabilityRef)) {
                it.idRef == 3
                riskDefinition.getRiskValue(it.idRef as int).orElseThrow().name == SEHR_HOCH
            }
        }
        oldRiskVersion < risk.getVersion()
        risk.version == 3

        and: "events were published as required"
        listener.receivedEvents.size() == 2

        def riskImpactEvent = listener.receivedEvents.find { it instanceof RiskChangedEvent }
        with (riskImpactEvent) {
            def event = it as RiskChangedEvent
            event.source.class == RiskService
            // Note: IMPACT_VALUES_CHANGED is not set here because that wasn't done by the risk service.
            // The event with this type should have been published by the use-case that sets the specificImpact.
            // TODO VEO-1361
            event.changes ==~ [RISK_VALUES_CHANGED]
            event.riskAffectedId == process.id
            event.scenarioId == scenario.id
            event.domainId == domain.id
            event.riskDefinition == riskDefinitionRef
            event.clientId == client.id
        }

        with (listener.receivedEvents.find { it instanceof RiskAffectingElementChangeEvent }) {
            def event = it as RiskAffectingElementChangeEvent
            event.entityType == Process
            event.entityId == process.id
            event.source.class == RiskService
            event.hasChangedRisks()
            event.clientId == client.id
            event.changedRisks ==~ [riskImpactEvent]
        }
        and: "the DB operations are within reasonable limits"
        verifyAll {
            queryCounts.select == 3
            queryCounts.insert == 0
            queryCounts.update == 1
            queryCounts.time < 500
        }
    }
}
