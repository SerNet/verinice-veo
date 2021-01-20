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
package org.veo.persistence.entity

import org.veo.core.entity.Asset
import org.veo.core.entity.AssetRisk
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Scenario
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.ModelConsistencyException
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.entity.jpa.AssetRiskData
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

import spock.lang.Specification

class AssetRiskSpec extends Specification {

    EntityFactory entityFactory

    Client client

    Unit unit

    def setup() {
        entityFactory = new EntityDataFactory()
        this.client = entityFactory.createClient(Key.newUuid(), "client")
        this.unit = entityFactory.createUnit(Key.newUuid(), "unit", null)
        this.unit.setClient(client)
    }

    private Control control(String name) {
        entityFactory.createControl(Key.newUuid(), name, unit)
    }

    private Scenario scenario(String name) {
        entityFactory.createScenario(Key.newUuid(), name, unit)
    }

    private Asset asset(String name) {
        entityFactory.createAsset(Key.newUuid(), name, unit)
    }

    private Domain domain(String name) {
        entityFactory.createDomain(Key.newUuid(), name)
    }

    def "An asset risk depends on an asset, a scenario and optionally a control" () {
        given: "existing entities"
        def asset = asset("asset1")
        def scenario = scenario("scenario1")
        def control = control("control")
        def domain1 = domain("domain1")
        asset.addToDomains(domain1)

        when: "a risk is created for these entities"
        def risk = asset.newRisk(scenario, domain1)
        risk.mitigate(control)

        then: "the risk references all entities"
        risk.asset == asset
        risk.scenario == scenario
        risk.mitigation == control
    }

    def "A risk does not have to be linked to a control"() {
        given: "an asset and a scenario"
        def asset = asset("asset1")
        def scenario = scenario("scenario1")
        def domain1 = domain("domain1")
        asset.addToDomains(domain1)

        when: "a risk is created"
        def risk = asset.newRisk(scenario, domain1)

        then: "the reference to a control may be left missing"
        risk.asset == asset
        risk.scenario == scenario
    }

    def "Multiple risks can be added to an asset and removed from it"() {
        given: "an asset and two scenarios"
        def asset = asset("asset1")
        def scenario1 = scenario("scenario1")
        def scenario2 = scenario("scenario2")
        def domain1 = domain("domain1")
        asset.addToDomains(domain1)

        when: "risks are added"
        def risks = asset.newRisks([scenario1, scenario2] as Set, domain1)

        then: "the asset has new risks"
        asset.risks.size() == 2

        when: "risks are removed"
        asset.removeRisks(risks)

        then: "the risks are gone"
        asset.risks.size() == 0
    }

    def "A risk may be linked to a risk owner"() {
        given: "defined entities"
        def asset = asset("asset1")
        def scenario = scenario("scenario1")
        def domain1 = domain("domain1")
        asset.addToDomains(domain1)
        def person = entityFactory.createPerson(Key.newUuid(), "person1", unit)

        when: "a risk is created and linked to the person"
        def risk = asset.newRisk(scenario, domain1)
        risk.appoint(person)

        then: "the person is present"
        risk.asset == asset
        risk.scenario == scenario
        risk.riskOwner == person
    }

    def "A risk may be linked to a composite of risk owners"() {
        given: "defined entities"
        def asset = asset("asset1")
        def scenario = scenario("scenario1")
        def domain1 = domain("domain1")
        asset.addToDomains(domain1)
        def person = entityFactory.createPerson(Key.newUuid(), "person1", unit)
        def personComposite = entityFactory.createPerson(Key.newUuid(), "personComposite1", unit)
        personComposite.parts = [person]

        when: "a risk is created and linked to the personComposite"
        def risk = asset.newRisk(scenario, domain1)
        risk.appoint(personComposite)

        then: "the personComposite is present"
        risk.asset == asset
        risk.scenario == scenario
        risk.riskOwner == personComposite
        risk.riskOwner.parts.first() == person
    }

    def "The risk may apply to a composite of assets"() {
        given: "a composite of assets"
        def asset1 = asset("asset1")
        def asset2 = asset("asset2")
        def domain1 = domain("domain1")
        def assetComposite = entityFactory.createAsset(Key.newUuid(), "assetcomposite", unit)
        assetComposite.addToDomains(domain1)
        assetComposite.setParts([asset1, asset2] as Set)
        def scenario = scenario("scenario1")

        when: "a risk is created"
        def risk = assetComposite.newRisk(scenario, domain1)

        then: "the composite of assets is a valid reference"
        risk.asset == assetComposite
        risk.asset.name == "assetcomposite"
        risk.asset.parts.contains(asset1)
        risk.asset.parts.contains(asset2)
    }

    def "The risk may apply to a composite of scenarios"() {
        given: "a composite of scenarios"
        def scenarioComposite = entityFactory.createScenario(Key.newUuid(), "scenariocomposite", unit)
        def scenario1 = scenario("scenario1")
        def scenario2 = scenario("scenario2")
        scenarioComposite.setParts([scenario1, scenario2] as Set)
        def asset = asset("asset1")
        def domain1 = domain("domain1")
        asset.addToDomains(domain1)

        when: "a risk is created"
        def risk = asset.newRisk(scenarioComposite, domain1)

        then: "the composite of scenarios is a valid reference"
        risk.scenario == scenarioComposite
        risk.scenario.parts.contains(scenario1)
        risk.scenario.parts.contains(scenario2)
    }

    def "The risk may apply to a composite of controls"() {
        given: "a composite of controls"
        def control1 = control("control1")
        def control2 = control("control2")
        def controlComposite = entityFactory.createControl(Key.newUuid(), "controlcomposite", unit)
        controlComposite.setParts([control1, control2] as Set)
        def asset1 = asset("asset1")
        def scenario1 = scenario("scenario1")
        def domain1 = domain("domain1")
        asset1.addToDomains(domain1)

        when: "a risk is created"
        def risk = asset1.newRisk(scenario1, domain1)
        risk.mitigate(controlComposite)

        then: "the composite of controls is a valid reference"
        risk.mitigation == controlComposite
        risk.mitigation.parts.contains(control1)
        risk.mitigation.parts.contains(control2)
    }

    def "There may be only one risk for the same asset and scenario"() {
        given: ""
        def scenario1 = scenario("scenario1")
        def asset1 = asset("asset1")
        def domain1 = domain("domain1")
        asset1.addToDomains(domain1)
        def risk1 = asset1.newRisk(scenario1, domain1)
        asset1.addToDomains(domain1)
        def set = new HashSet<AssetRisk>()
        set.add(risk1)

        when: "another risk is created"
        def risk2 = asset1.newRisk(scenario1, domain1)
        set.add(risk2)

        then: "it has the same identity"
        risk1 == risk2
        set.size() == 1
        set.first() == risk1
    }

    def "A risk must belong to one or multiple domains"() {
        given: "predefined entities"
        def scenario1 = scenario("scenario1")
        def asset1 = asset("asset1")
        def domain1 = domain("domain1")
        def domain2 = domain("domain2")
        def domainUnknown = domain("domain3")
        asset1.addToDomains(domain1)
        asset1.addToDomains(domain2)

        when: "a risk is created with two domains"
        def risk = asset1.newRisk(scenario1, domain1)
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

        when: "A risk is created for a domain that the asset does not know about"
        asset1.newRisk(scenario1, domainUnknown)

        then: "The operation is prevented"
        thrown(ModelConsistencyException)

        when: "A domain that is unknown to the asset is added to an existing risk."
        risk.addToDomains(domainUnknown)

        then: "The operation is prevented"
        thrown(ModelConsistencyException)
    }
}
