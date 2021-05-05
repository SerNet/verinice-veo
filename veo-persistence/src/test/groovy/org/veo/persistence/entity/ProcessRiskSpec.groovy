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
package org.veo.persistence.entity


import org.veo.core.entity.Client
import org.veo.core.entity.ProcessRisk
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.ModelConsistencyException
import org.veo.test.VeoSpec

class ProcessRiskSpec extends VeoSpec {

    Client client
    Unit unit

    def setup() {
        this.client = newClient()
        this.unit = newUnit(client)
    }

    def "A process risk depends on a process, a scenario and optionally a control" () {
        given: "existing entities"
        def process = newProcess(unit)
        def scenario = newScenario(unit)
        def control = newControl(unit)
        def domain1 = newDomain()
        process.addToDomains(domain1)

        when: "a risk is created for these entities"
        def risk = process.newRisk(scenario, domain1)
        risk.mitigate(control)

        then: "the risk references all entities"
        risk.entity == process
        risk.scenario == scenario
        risk.mitigation == control
    }

    def "A risk does not have to be linked to a control"() {
        given: "an process and a scenario"
        def process = newProcess(unit)
        def scenario = newScenario(unit)
        def domain1 = newDomain()
        process.addToDomains(domain1)

        when: "a risk is created"
        def risk = process.newRisk(scenario, domain1)

        then: "the reference to a control may be left missing"
        risk.entity == process
        risk.scenario == scenario
    }

    def "Multiple risks can be added to a process and removed from it"() {
        given: "an process and two scenarios"
        def process = newProcess(unit)
        def scenario1 = newScenario(unit)
        def scenario2 = newScenario(unit)
        def domain1 = newDomain()
        process.addToDomains(domain1)

        when: "risks are added"
        def risks = process.newRisks([scenario1, scenario2] as Set, domain1)

        then: "the process has new risks"
        process.risks.size() == 2

        when: "risks are removed"
        process.removeRisks(risks)

        then: "the risks are gone"
        process.risks.size() == 0
    }

    def "A risk may be linked to a risk owner"() {
        given: "defined entities"
        def process = newProcess(unit)
        def scenario = newScenario(unit)
        def domain1 = newDomain()
        process.addToDomains(domain1)
        def person = newPerson(unit)

        when: "a risk is created and linked to the person"
        def risk = process.newRisk(scenario, domain1)
        risk.appoint(person)

        then: "the person is present"
        risk.entity == process
        risk.scenario == scenario
        risk.riskOwner == person
    }

    def "A risk may be linked to a composite of risk owners"() {
        given: "defined entities"
        def process = newProcess(unit)
        def scenario = newScenario(unit)
        def domain1 = newDomain()
        process.addToDomains(domain1)
        def person = newPerson(unit)
        def personComposite = newPerson(unit)
        personComposite.parts = [person]

        when: "a risk is created and linked to the personComposite"
        def risk = process.newRisk(scenario, domain1)
        risk.appoint(personComposite)

        then: "the personComposite is present"
        risk.entity == process
        risk.scenario == scenario
        risk.riskOwner == personComposite
        risk.riskOwner.parts.first() == person
    }

    def "The risk may apply to a composite of processes"() {
        given: "a composite of processes"
        def process1 = newProcess(unit)
        def process2 = newProcess(unit)
        def domain1 = newDomain()
        def processComposite = newProcess(unit) {
            name = "processcomposite"
        }
        processComposite.addToDomains(domain1)
        processComposite.setParts([process1, process2] as Set)
        def scenario = newScenario(unit)

        when: "a risk is created"
        def risk = processComposite.newRisk(scenario, domain1)

        then: "the composite of processes is a valid reference"
        risk.entity == processComposite
        risk.entity.name == "processcomposite"
        risk.entity.parts.contains(process1)
        risk.entity.parts.contains(process2)
    }

    def "The risk may apply to a composite of scenarios"() {
        given: "a composite of scenarios"
        def scenarioComposite = newScenario(unit)
        def scenario1 = newScenario(unit)
        def scenario2 = newScenario(unit)
        scenarioComposite.setParts([scenario1, scenario2] as Set)
        def process = newProcess(unit)
        def domain1 = newDomain()
        process.addToDomains(domain1)

        when: "a risk is created"
        def risk = process.newRisk(scenarioComposite, domain1)

        then: "the composite of scenarios is a valid reference"
        risk.scenario == scenarioComposite
        risk.scenario.parts.contains(scenario1)
        risk.scenario.parts.contains(scenario2)
    }

    def "The risk may apply to a composite of controls"() {
        given: "a composite of controls"
        def control1 = newControl(unit)
        def control2 = newControl(unit)
        def controlComposite = newControl(unit)
        controlComposite.setParts([control1, control2] as Set)
        def process1 = newProcess(unit)
        def scenario1 = newScenario(unit)
        def domain1 = newDomain()
        process1.addToDomains(domain1)

        when: "a risk is created"
        def risk = process1.newRisk(scenario1, domain1)
        risk.mitigate(controlComposite)

        then: "the composite of controls is a valid reference"
        risk.mitigation == controlComposite
        risk.mitigation.parts.contains(control1)
        risk.mitigation.parts.contains(control2)
    }

    def "There may be only one risk for the same process and scenario"() {
        given: ""
        def scenario1 = newScenario(unit)
        def process1 = newProcess(unit)
        def domain1 = newDomain()
        process1.addToDomains(domain1)
        def risk1 = process1.newRisk(scenario1, domain1)
        process1.addToDomains(domain1)
        def set = new HashSet<ProcessRisk>()
        set.add(risk1)

        when: "another risk is created"
        def risk2 = process1.newRisk(scenario1, domain1)
        set.add(risk2)

        then: "it has the same identity"
        risk1 == risk2
        set.size() == 1
        set.first() == risk1
    }

    def "A risk must belong to one or multiple domains"() {
        given: "predefined entities"
        def scenario1 = newScenario(unit)
        def process1 = newProcess(unit)
        def domain1 = newDomain()
        def domain2 = newDomain()
        def domainUnknown = newDomain()
        process1.addToDomains(domain1)
        process1.addToDomains(domain2)

        when: "a risk is created with two domains"
        def risk = process1.newRisk(scenario1, domain1)
        risk.addToDomains(domain2)

        then: "the domains are referenced by the risk"
        risk.domains.size() == 2

        when: "a domain can be removed"
        def domain1Removed = risk.removeFromDomains(domain1)

        then:
        domain1Removed
        risk.domains.size() == 1

        when: "the last domain is removed from the risk"
        risk.removeFromDomains(domain2)

        then: "the operation is prevented"
        thrown(ModelConsistencyException)

        when: "A risk is created for a domain that the process does not know about"
        process1.newRisk(scenario1, domainUnknown)

        then: "The operation is prevented"
        thrown(ModelConsistencyException)

        when: "A domain that is unknown to the process is added to an existing risk."
        risk.addToDomains(domainUnknown)

        then: "The operation is prevented"
        thrown(ModelConsistencyException)
    }
}
