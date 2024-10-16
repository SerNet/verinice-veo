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

import org.hibernate.Hibernate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.annotation.Transactional

import org.veo.core.VeoSpringSpec
import org.veo.core.repository.ControlRepository
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ScopeRiskData

@WithUserDetails("user@domain.example")
class ScopeRiskITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    ScopeRepositoryImpl scopeRepository

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
        def scenario1 = insertScenario(newScenario(unit))
        def domain1 = insertDomain(newDomain(client))
        ScopeRisk risk
        def scope1 = insertScope(newScope(unit) {
            associateWithDomain(domain1, "NormalScope", "NEW")
            risk = obtainRisk(scenario1).tap {
                designator = "RSK-1"
            }
        })
        def person1 = insertPerson(newPerson(unit))
        def control1 = insertControl(newControl(unit))

        when: "the risk is retrieved"
        ScopeRisk retrievedRisk1 = txTemplate.execute{
            Set<Scope> scopes = scopeRepository.findByRisk(scenario1)
            def scopeRisk = scopes.first().risks.first()
            // initialize hibernate proxy
            Hibernate.initialize(scopeRisk.scenario)
            return scopeRisk
        }

        then:
        retrievedRisk1 == risk
        retrievedRisk1.scenario == scenario1
        retrievedRisk1.entity == scope1
        def riskData = (ScopeRiskData) retrievedRisk1
        riskData.createdAt != null
        riskData.updatedAt != null

        when: "a control is added"
        txTemplate.execute{
            def scope = scopeRepository.findByRisk(scenario1).first()
            scope.risks.first().mitigate(control1)
        }

        and: "the risk that is mitigated by the control is found"
        // flush and load in new transaction
        def retrievedRisk = txTemplate.execute {
            def retrievedScope = scopeRepository.findByRisk(control1).first()
            def retrievedRisk = retrievedScope.risks.first()
            assert retrievedRisk.mitigation == control1
            assert retrievedRisk.entity == scope1
            return retrievedRisk
        }

        then:
        retrievedRisk == risk

        when: "a risk owner is added"
        txTemplate.execute{
            def scope = scopeRepository.findByRisk(scenario1).first()
            scope.risks.first().appoint(person1)
        }

        and: "the risk for the risk owner is found"
        // flush and load in new transaction
        def retrievedRisk2 = txTemplate.execute {
            def retrievedScope = scopeRepository.findByRisk(person1).first()
            def retrievedRisk2 = retrievedScope.risks.first()
            assert retrievedRisk2.mitigation == control1
            assert retrievedRisk2.riskOwner == person1
            assert retrievedRisk2.entity == scope1
            return retrievedRisk2
        }

        then:
        retrievedRisk2 == risk
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
    Scope insertScope(Scope scope) {
        scopeRepository.save(scope)
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
