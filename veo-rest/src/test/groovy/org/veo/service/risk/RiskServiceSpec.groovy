/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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


import static org.veo.core.entity.event.RiskEvent.ChangedValues.IMPACT_VALUES_CHANGED
import static org.veo.core.entity.event.RiskEvent.ChangedValues.PROBABILITY_VALUES_CHANGED
import static org.veo.core.entity.event.RiskEvent.ChangedValues.RISK_VALUES_CHANGED

import org.veo.core.entity.ProcessRisk
import org.veo.core.entity.event.RiskAffectingElementChangeEvent
import org.veo.core.entity.event.RiskChangedEvent
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.ImpactRef
import org.veo.core.entity.risk.PotentialProbabilityImpl
import org.veo.core.entity.risk.ProbabilityRef
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.entity.risk.RiskRef
import org.veo.core.repository.ProcessRepository
import org.veo.core.service.EventPublisher
import org.veo.persistence.entity.jpa.ClientData
import org.veo.persistence.entity.jpa.DomainData
import org.veo.persistence.entity.jpa.ProcessData
import org.veo.persistence.entity.jpa.ScenarioData
import org.veo.persistence.entity.jpa.UnitData
import org.veo.test.VeoSpec

import spock.lang.Unroll

class RiskServiceSpec extends VeoSpec {

    static final String RISK_DEF = "r2d2"
    RiskService sut
    ProcessRepository repo = Mock()
    EventPublisher publisher = Mock()
    ProcessData process
    ProcessRisk risk
    RiskDefinitionRef riskDefRef
    ScenarioData scenario
    DomainData domain
    UnitData unit
    ClientData client

    def setup() {
        sut = new RiskService(repo, publisher)
        this.client = newClient()
        this.unit = newUnit(client) {
            it.setClient(client)
        }
        def riskDefinition = createRiskDefinition(RISK_DEF)
        this.riskDefRef = RiskDefinitionRef.from(riskDefinition)

        this.domain = newDomain(client) {
            it.riskDefinitions = [(RISK_DEF): riskDefinition]
        }
        this.scenario = newScenario(unit) {
            addToDomains(domain)
        }
        this.process = newProcess(unit) {
            addToDomains(domain)
        }
        this.risk = process.obtainRisk(scenario, domain).tap {
            assignDesignator(it)
            defineRiskValues([
                newRiskValues(riskDefRef, domain)
            ] as Set)
            addToDomains(domain)
        }
    }

    @Unroll
    def "Events are NOT generated when transferring unchanged probability #prob to risk"() {
        when:
        risk.getProbabilityProvider(riskDefRef, domain).potentialProbability =
                previousProb?.with { new ProbabilityRef(it) }
        scenario.setPotentialProbability(domain, [
            (riskDefRef): new PotentialProbabilityImpl(prob?.with { new ProbabilityRef(it) })
        ])
        def categoryRef = new CategoryRef("C")
        risk.getRiskProvider(riskDefRef, domain)
                .getCategorizedRisks()
                .find({ it.category == categoryRef })
                .setInherentRisk(previousRiskVal?.with { new RiskRef(it) })

        sut.evaluateChangedRiskComponent(this.process)

        then:
        risk.getProbabilityProvider(riskDefRef, domain).potentialProbability ==
                prob?.with { new ProbabilityRef(it) }
        risk.getRiskProvider(riskDefRef, domain).getInherentRisk(categoryRef) ==
                riskVal?.with { new RiskRef(it) }
        1 * repo.findAllHavingRisks(_) >> [this.process]
        0 * publisher.publish(_)

        where:
        prob | previousProb | riskVal | previousRiskVal
        0    | 0            | null    | null
        1    | 1            | null    | null
        null | null         | null    | null
    }

    @Unroll
    def "Events are generated when transferring changed probability #prob to risk"() {
        when:
        risk.getProbabilityProvider(riskDefRef, domain).potentialProbability =
                previousProb?.with { new ProbabilityRef(it) }
        scenario.setPotentialProbability(domain, [
            (riskDefRef): new PotentialProbabilityImpl(prob?.with { new ProbabilityRef(it) })
        ])

        sut.evaluateChangedRiskComponent(this.process)

        then:
        risk.getProbabilityProvider(riskDefRef, domain).potentialProbability ==
                prob?.with { new ProbabilityRef(it) }
        1 * repo.findAllHavingRisks(_) >> [this.process]
        1 * publisher.publish({
            verifyAll(it, RiskChangedEvent) {
                changes ==~ [PROBABILITY_VALUES_CHANGED]
                source == sut
                clientId == process.owningClient.get().id
            }
        })
        1 * publisher.publish({
            verifyAll(it, RiskAffectingElementChangeEvent) {
                it.hasChangedRisks()
                it.changedRisks.size() == 1
                changes ==~ [
                    RISK_VALUES_CHANGED,
                    IMPACT_VALUES_CHANGED,
                    PROBABILITY_VALUES_CHANGED
                ]
                source == sut
                clientId == process.owningClient.get().id
            }
        })

        where:
        prob | previousProb
        1    | 2
        null | 2
        2    | null
    }

    @Unroll
    def "Risk events are generated when probability #prob and risk #riskVal changed from (#previousProb/#previousRiskVal)"() {
        when:
        def categoryRef = new CategoryRef("C")
        risk.getProbabilityProvider(riskDefRef, domain).potentialProbability =
                previousProb?.with { new ProbabilityRef(it) }
        risk.getImpactProvider(riskDefRef, domain).setSpecificImpact(categoryRef, new ImpactRef(1))
        scenario.setPotentialProbability(domain, [
            (riskDefRef): new PotentialProbabilityImpl(prob?.with { new ProbabilityRef(it) })
        ])
        risk.getRiskProvider(riskDefRef, domain)
                .getCategorizedRisks()
                .find({ it.category == categoryRef })
                .setInherentRisk(previousRiskVal?.with { new RiskRef(it) })

        sut.evaluateChangedRiskComponent(this.process)

        then:
        risk.getProbabilityProvider(riskDefRef, domain).potentialProbability ==
                (prob?.with { new ProbabilityRef(it) })
        risk.getRiskProvider(riskDefRef, domain).getInherentRisk(categoryRef) == (riskVal?.with { new RiskRef(it) })
        1 * repo.findAllHavingRisks(_) >> [this.process]
        1 * publisher.publish({
            verifyAll(it, RiskChangedEvent) {
                changes ==~ [
                    PROBABILITY_VALUES_CHANGED,
                    RISK_VALUES_CHANGED
                ]
                source == sut
                clientId == process.owningClient.get().id
            }
        })
        1 * publisher.publish({
            verifyAll(it, RiskAffectingElementChangeEvent) {
                it.hasChangedRisks()
                it.changedRisks.size() == 1
                changes ==~ [
                    RISK_VALUES_CHANGED,
                    IMPACT_VALUES_CHANGED,
                    PROBABILITY_VALUES_CHANGED
                ]
                source == sut
                clientId == process.owningClient.get().id
            }
        })

        where:
        prob | previousProb | riskVal | previousRiskVal
        1    | 0            | 0       | 2
        0    | 1            | 0       | 2
        null | 2            | null    | 1
        2    | null         | 1       | null
    }

    @Unroll
    def "Risk events are generated for changed risk #riskVal from specific probability/impact #prob/#imp"() {
        when:
        def categoryRef = new CategoryRef(cat)
        risk.getRiskProvider(riskDefRef, domain)
                .getCategorizedRisks()
                .find({ it.category == categoryRef })
                .setInherentRisk(previousRiskVal?.with { new RiskRef(it) })
        risk.getProbabilityProvider(riskDefRef, domain).setSpecificProbability(prob?.with { new ProbabilityRef(it) })
        risk.getImpactProvider(riskDefRef, domain).setSpecificImpact(categoryRef, imp?.with { new ImpactRef(it) })

        sut.evaluateChangedRiskComponent(this.process)

        then:
        risk.getRiskProvider(riskDefRef, domain).getInherentRisk(categoryRef) == (riskVal?.with { new RiskRef(it) })
        1 * repo.findAllHavingRisks(_) >> [this.process]
        1 * publisher.publish({
            verifyAll(it, RiskChangedEvent) {
                changes ==~ [RISK_VALUES_CHANGED]
                source == sut
                clientId == process.owningClient.get().id
            }
        })
        1 * publisher.publish({
            verifyAll(it, RiskAffectingElementChangeEvent) {
                it.hasChangedRisks()
                it.changedRisks.size() == 1
                changes ==~ [
                    RISK_VALUES_CHANGED,
                    IMPACT_VALUES_CHANGED,
                    PROBABILITY_VALUES_CHANGED
                ]
                source == sut
                clientId == process.owningClient.get().id
            }
        })

        where:
        cat | prob | imp  | riskVal | previousRiskVal
        "C" | 2    | 2    | 2       | 0
        "C" | 2    | 1    | 1       | 0
        "C" | 1    | 2    | 1       | 0
        "C" | 0    | 3    | 1       | 0
        "I" | 0    | 0    | 0       | null
        "I" | 2    | 1    | 1       | null
        "I" | 1    | 2    | 1       | 2
        "A" | 2    | 1    | 1       | 2
        "A" | 1    | 2    | 1       | 2
        "R" | 2    | 1    | 1       | 2
        "R" | 1    | 2    | 1       | 2
        "C" | null | 3    | null    | 0
        "C" | 0    | null | null    | 0
    }

    @Unroll
    def "Risk events are NOT generated for unchanged risk #riskVal from probability/impact: #prob/#imp"() {
        when:
        def categoryRef = new CategoryRef(cat)
        risk.getRiskProvider(riskDefRef, domain)
                .getCategorizedRisks()
                .find({ it.category == categoryRef })
                .setInherentRisk(previousRiskVal?.with { new RiskRef(it) })
        risk.getProbabilityProvider(riskDefRef, domain).setSpecificProbability(prob?.with { new ProbabilityRef(it) })
        risk.getImpactProvider(riskDefRef, domain).setSpecificImpact(categoryRef, imp?.with { new ImpactRef(it) })

        sut.evaluateChangedRiskComponent(this.process)

        then:
        risk.getRiskProvider(riskDefRef, domain).getInherentRisk(categoryRef) == (riskVal?.with { new RiskRef(it) })
        1 * repo.findAllHavingRisks(_) >> [this.process]
        0 * publisher.publish(*_)

        where:
        cat | prob | imp  | riskVal | previousRiskVal
        "C" | 0    | 0    | 0       | 0
        "C" | 3    | 0    | 0       | 0
        "C" | 3    | 3    | 3       | 3
        "I" | 3    | null | null    | null
        "A" | null | 0    | null    | null
        "R" | null | null | null    | null
    }

    def "Risk events are published for multiple risks"() {
        when:
        def categoryRef = new CategoryRef("C")
        def scenario2 = newScenario(unit) {
            addToDomains(domain)
        }
        def risk2 = process.obtainRisk(scenario2, domain).tap {
            assignDesignator(it)
            defineRiskValues([
                newRiskValues(riskDefRef, domain)
            ] as Set)
            addToDomains(domain)
        }

        risk.getImpactProvider(riskDefRef, domain).setSpecificImpact(categoryRef, new ImpactRef(1))
        risk2.getImpactProvider(riskDefRef, domain).setSpecificImpact(categoryRef, new ImpactRef(1))

        risk.getProbabilityProvider(riskDefRef, domain).potentialProbability =
                (new ProbabilityRef(3))
        risk2.getProbabilityProvider(riskDefRef, domain).potentialProbability =
                (new ProbabilityRef(1))

        scenario.setPotentialProbability(domain, [
            (riskDefRef): new PotentialProbabilityImpl(new ProbabilityRef(1))
        ])
        scenario2.setPotentialProbability(domain, [
            (riskDefRef): new PotentialProbabilityImpl(new ProbabilityRef(3))
        ])

        sut.evaluateChangedRiskComponent(this.process)

        then:
        verifyAll {
            risk.getProbabilityProvider(riskDefRef, domain).potentialProbability.idRef == 1
            risk.getRiskProvider(riskDefRef, domain).getInherentRisk(categoryRef).idRef == 0

            risk2.getProbabilityProvider(riskDefRef, domain).potentialProbability.idRef == 3
            risk2.getRiskProvider(riskDefRef, domain).getInherentRisk(categoryRef).idRef == 2
        }

        1 * repo.findAllHavingRisks(_) >> [this.process]
        2 * publisher.publish({
            verifyAll(it, RiskChangedEvent) {
                changes ==~ [
                    PROBABILITY_VALUES_CHANGED,
                    RISK_VALUES_CHANGED
                ]
                source == sut
                clientId == process.owningClient.get().id
            }
        })
        1 * publisher.publish({
            verifyAll(it, RiskAffectingElementChangeEvent) {
                it.hasChangedRisks()
                it.changedRisks.size() == 2
                changes ==~ [
                    RISK_VALUES_CHANGED,
                    IMPACT_VALUES_CHANGED,
                    PROBABILITY_VALUES_CHANGED
                ]
                source == sut
                clientId == process.owningClient.get().id
            }
        })
    }
}
