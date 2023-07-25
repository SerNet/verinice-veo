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

import static java.time.temporal.ChronoUnit.MILLIS

import java.time.Instant

import org.hibernate.Hibernate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.annotation.Transactional

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.entity.risk.RiskValues
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.repository.ControlRepository
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ProcessRiskData
import org.veo.service.risk.RiskService

@WithUserDetails("user@domain.example")
class ProcessRiskITSpec extends VeoSpringSpec {

    @Autowired
    EntityFactory entityFactory

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    ProcessRepositoryImpl processRepository

    @Autowired
    PersonRepositoryImpl personRepository

    @Autowired
    ScenarioRepositoryImpl scenarioRepository

    @Autowired
    DomainRepositoryImpl domainRepository

    @Autowired
    ControlRepository controlRepository

    @Autowired
    RiskService riskService

    Client client

    Unit unit

    def setup() {
        createClient()
    }

    def "a risk can be modified persistently"() {
        given: "predefined entities"
        def beforeCreate = Instant.now()
        def scenario1 = insertScenario(newScenario(unit))
        def domain = insertDomain(newDomain(client))
        ProcessRisk risk
        def process1 = insertProcess(newProcess(unit) {
            associateWithDomain(domain, "PRO_DataProcessing", "NEW")
            risk = obtainRisk(scenario1, domain).tap {
                designator = "RSK-1"
            }
        })
        def person1 = insertPerson(newPerson(unit))
        def control1 = insertControl(newControl(unit))

        when: "the risk is retrieved"
        ProcessRisk retrievedRisk1 = txTemplate.execute {
            Set<Process> processes = processRepository.findByRisk(scenario1)
            def processRisk = processes.first().risks.first()
            assert processRisk.domains.first() == domain
            // initialize hibernate proxy
            Hibernate.initialize(processRisk.scenario)
            return processRisk
        }
        def riskData = (ProcessRiskData) retrievedRisk1
        def createdAt = riskData.createdAt
        def updatedAt = riskData.updatedAt
        def updatedBy = riskData.updatedBy

        then:
        retrievedRisk1 == risk
        retrievedRisk1.scenario == scenario1
        retrievedRisk1.domains.first() == domain
        retrievedRisk1.entity == process1
        createdAt != null
        createdAt > beforeCreate
        updatedAt != null
        updatedAt > beforeCreate
        updatedBy == "user@domain.example"

        when: "a control is added"
        txTemplate.execute {
            def process = processRepository.findByRisk(scenario1).first()
            process.risks.first().mitigate(control1)
        }

        and: "the risk that is mitigated by the control is found"
        // flush and load in new transaction
        def retrievedRisk = txTemplate.execute {
            def retrievedProcess = processRepository.findByRisk(control1).first()
            def retrievedRisk = retrievedProcess.risks.first()
            assert retrievedRisk.mitigation == control1
            assert retrievedRisk.entity == process1
            return retrievedRisk
        }

        then:
        retrievedRisk == risk

        when: "a risk owner is added"
        txTemplate.execute{
            def process = processRepository.findByRisk(scenario1).first()
            process.risks.first().appoint(person1)
        }

        and: "the risk for the risk owner is found"
        // flush and load in new transaction
        def retrievedRisk2 = txTemplate.execute {
            def retrievedProcess = processRepository.findByRisk(person1).first()
            def retrievedRisk2 = retrievedProcess.risks.first()
            assert retrievedRisk2.mitigation == control1
            assert retrievedRisk2.riskOwner == person1
            assert retrievedRisk2.entity == process1
            return retrievedRisk2
        }

        then:
        retrievedRisk2 == risk
    }

    def "a risk is only updated when it has really been modified"() {
        given: "a process with two risks"
        def beforeCreate = Instant.now()
        def domain = domainRepository.save(newDomain(client) {
            it.riskDefinitions = [
                "r2d2": createRiskDefinition("r2d2")
            ] as Map
        })
        def scenario1 = insertScenario(newScenario(unit))
        def scenario2 = insertScenario(newScenario(unit))
        def riskDefRef = RiskDefinitionRef.from(domain.getRiskDefinitions().values().first())
        ProcessRisk risk1
        ProcessRisk risk2
        insertProcess(newProcess(unit) {
            associateWithDomain(domain, "PRO_DataProcessing", "NEW")
            risk1 = obtainRisk(scenario1, domain).tap {
                designator = "RSK-1"
                defineRiskValues([
                    newRiskValues(riskDefRef, domain)
                ] as Set)
            }
            risk2 = obtainRisk(scenario2, domain).tap {
                designator = "RSK-2"
                defineRiskValues([
                    newRiskValues(riskDefRef, domain)
                ] as Set)
            }
        })

        // account for DB accuracy being less than nanos
        def risk1Created = risk1.createdAt.truncatedTo(MILLIS)
        def risk1Updated = risk1.updatedAt.truncatedTo(MILLIS)
        def risk2Created = risk2.createdAt.truncatedTo(MILLIS)
        def risk2Updated = risk2.updatedAt.truncatedTo(MILLIS)

        when: "risk1 is changed"
        def beforeUpdate = Instant.now().truncatedTo(MILLIS)
        txTemplate.execute {
            def process = processRepository.findByRisk(scenario1).first()
            def risk = process.risks.find({ it.scenario == scenario1 })
            def riskValue = RiskValues.from(risk, riskDefRef, domain)

            riskValue.setSpecificProbabilityExplanation('There... are... FOUR... lights!')
            process.updateRisk(
                    risk,
                    [domain] as Set,
                    null,
                    null,
                    [riskValue] as Set
                    )
            processRepository.save(process)

            riskService.evaluateChangedRiskComponent(process)
        }
        // retrieve in new transaction:
        def retrievedRisk1 = txTemplate.execute {
            def process = processRepository.findByRisk(scenario1).first()
            return process.risks.find({ it.scenario == scenario1 })
        }
        // retrieve in new transaction:
        def retrievedRisk2 = txTemplate.execute {
            def process = processRepository.findByRisk(scenario1).first()
            return process.risks.find({ it.scenario == scenario2 })
        }

        then: "risk1's audit data is updated"
        retrievedRisk1.updatedAt > risk1Updated
        retrievedRisk1.updatedAt > beforeUpdate
        retrievedRisk1.createdAt.truncatedTo(MILLIS) == risk1Created
        retrievedRisk1.createdAt > beforeCreate
        retrievedRisk1.createdAt < beforeUpdate

        and: "risk2's audit data is left unchanged"
        retrievedRisk2.updatedAt.truncatedTo(MILLIS) == risk2Updated
        retrievedRisk2.createdAt.truncatedTo(MILLIS) == risk2Created
        retrievedRisk2.createdAt > beforeCreate
        retrievedRisk2.updatedAt == retrievedRisk2.createdAt
    }

    @Transactional
    void createClient() {
        client = clientRepository.save(newClient())
        def domain = domainRepository.save(newDomain(client))

        unit = unitRepository.save(newUnit(client) {
            addToDomains(domain)
        })
    }

    @Transactional
    Process insertProcess(Process process) {
        processRepository.save(process)
    }

    @Transactional
    Person insertPerson(Person person) {
        personRepository.save(person)
    }

    @Transactional
    Scenario insertScenario(Scenario scenario) {
        scenarioRepository.save(scenario)
    }

    @Transactional
    Domain insertDomain(Domain domain) {
        domain.setOwner(client)
        domainRepository.save(domain)
    }

    @Transactional
    Control insertControl(Control control) {
        control.setOwner(unit)
        controlRepository.save(control)
    }
}
