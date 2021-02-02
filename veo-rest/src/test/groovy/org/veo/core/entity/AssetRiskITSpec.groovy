/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.entity

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.usecase.repository.ControlRepository
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.AssetRiskData

@SpringBootTest(classes = AssetRiskITSpec.class)
@ComponentScan("org.veo")
@ActiveProfiles(["test"])
class AssetRiskITSpec extends VeoSpringSpec {

    @Autowired
    EntityFactory entityFactory

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    AssetRepositoryImpl assetRepository

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

    private Control control(String name) {
        entityFactory.createControl( name, unit)
    }

    private Scenario scenario(String name) {
        entityFactory.createScenario( name, unit)
    }

    private Asset asset(String name) {
        entityFactory.createAsset( name, unit)
    }

    private Domain domain(String name) {
        entityFactory.createDomain( name)
    }

    private Person person(String name) {
        entityFactory.createPerson( name, unit)
    }

    def "a risk can be modified persistently"() {

        given: "predefined entities"
        def asset1 = asset("asset1")
        def scenario1 = scenario("scenario1")
        def domain1 = domain("domain1")
        asset1.addToDomains(domain1)
        def control1 = control("control1")
        def person1 = person("person1")

        def risk = txTemplate.execute{
            scenario1 = insertScenario(scenario1)
            domain1 = insertDomain(domain1)
            asset1 = insertAsset(asset1)
            person1 = insertPerson(person1)
            control1 = insertControl(control1)

            asset1 = assetRepository.findById(asset1.getId()).get()
            asset1.newRisk(scenario1, domain1)
        }

        when: "the risk is retrieved"
        AssetRisk retrievedRisk1 = txTemplate.execute{
            Set<Asset> assets = assetRepository.findByRisk(scenario1)
            def assetRisk = assets.first().risks.first()
            assert assetRisk.domains.first() == domain1
            return assetRisk
        }

        then:
        retrievedRisk1 == risk
        retrievedRisk1.scenario == scenario1
        retrievedRisk1.domains.first() == domain1
        retrievedRisk1.entity == asset1
        retrievedRisk1.scenario == scenario1
        def riskData = (AssetRiskData) retrievedRisk1
        riskData.createdAt != null
        riskData.updatedAt != null

        when: "a control is added"
        txTemplate.execute{
            def asset = assetRepository.findByRisk(scenario1).first()
            asset.risks.first().mitigate(control1)
        }

        and: "the risk that is mitigated by the control is found"
        // flush and load in new transaction
        def retrievedRisk = txTemplate.execute {
            def retrievedAsset = assetRepository.findByRisk(control1).first()
            def retrievedRisk = retrievedAsset.risks.first()
            assert retrievedRisk.mitigation == control1
            assert retrievedRisk.entity == asset1
            return retrievedRisk
        }

        then:
        retrievedRisk == risk

        when: "a risk owner is added"
        txTemplate.execute{
            def asset = assetRepository.findByRisk(scenario1).first()
            asset.risks.first().appoint(person1)
        }

        and: "the risk for the risk owner is found"
        // flush and load in new transaction
        def retrievedRisk2 = txTemplate.execute {
            def retrievedAsset = assetRepository.findByRisk(person1).first()
            def retrievedRisk2 = retrievedAsset.risks.first()
            assert retrievedRisk2.mitigation == control1
            assert retrievedRisk2.riskOwner == person1
            assert retrievedRisk2.entity == asset1
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
    Asset insertAsset(Asset asset) {
        assetRepository.save(asset)
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
