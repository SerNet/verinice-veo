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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.repository.ControlRepository
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ProcessRiskData

@SpringBootTest(classes = ProcessRiskITSpec.class)
@ActiveProfiles(["test"])
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

    Client client

    Unit unit

    def setup() {
        createClient()
    }

    def "a risk can be modified persistently"() {

        given: "predefined entities"
        def process1 = newProcess(unit)
        def scenario1 = newScenario(unit)
        def domain1 = newDomain(client)
        process1.addToDomains(domain1)
        def control1 = newControl(unit)
        def person1 = newPerson(unit)

        def risk = txTemplate.execute{
            scenario1 = insertScenario(scenario1)
            domain1 = insertDomain(domain1)
            process1 = insertProcess(process1)
            person1 = insertPerson(person1)
            control1 = insertControl(control1)

            process1 = processRepository.findById(process1.getId()).get()
            process1.newRisk(scenario1, domain1).tap {
                designator = "RSK-1"
            }
        }

        when: "the risk is retrieved"
        ProcessRisk retrievedRisk1 = txTemplate.execute{
            Set<Process> processes = processRepository.findByRisk(scenario1)
            def processRisk = processes.first().risks.first()
            assert processRisk.domains.first() == domain1
            return processRisk
        }

        then:
        retrievedRisk1 == risk
        retrievedRisk1.scenario == scenario1
        retrievedRisk1.domains.first() == domain1
        retrievedRisk1.entity == process1
        retrievedRisk1.scenario == scenario1
        def riskData = (ProcessRiskData) retrievedRisk1
        riskData.createdAt != null
        riskData.updatedAt != null

        when: "a control is added"
        txTemplate.execute{
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

    @Transactional
    void createClient() {
        client = clientRepository.save(newClient())
        def domain = domainRepository.save(newDomain{
            owner = this.client
        })

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
