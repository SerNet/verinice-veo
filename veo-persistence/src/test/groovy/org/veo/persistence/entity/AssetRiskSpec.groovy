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

import org.veo.core.entity.AssetRisk
import org.veo.core.entity.Client
import org.veo.core.entity.Unit
import org.veo.test.VeoSpec

class AssetRiskSpec extends VeoSpec {

    Client client
    Unit unit

    def setup() {
        this.client = newClient()
        this.unit = newUnit(client)
    }

    def "An asset risk depends on an asset, a scenario and optionally a control" () {
        given: "existing entities"
        def asset = newAsset(unit)
        def scenario = newScenario(unit)
        def control = newControl(unit)
        def domain1 = newDomain(client)
        asset.associateWithDomain(domain1, "NormalAsset", "NEW")

        when: "a risk is created for these entities"
        def risk = asset.obtainRisk(scenario)
        risk.mitigate(control)

        then: "the risk references all entities"
        risk.entity == asset
        risk.scenario == scenario
        risk.mitigation == control
    }

    def "A risk does not have to be linked to a control"() {
        given: "an asset and a scenario"
        def asset = newAsset(unit)
        def scenario = newScenario(unit)
        def domain1 = newDomain(client)
        asset.associateWithDomain(domain1, "NormalAsset", "NEW")

        when: "a risk is created"
        def risk = asset.obtainRisk(scenario)

        then: "the reference to a control may be left missing"
        risk.entity == asset
        risk.scenario == scenario
    }

    def "Multiple risks can be added to an asset and removed from it"() {
        given: "an asset and two scenarios"
        def asset = newAsset(unit)
        def scenario1 = newScenario(unit)
        def scenario2 = newScenario(unit)
        def domain1 = newDomain(client)
        asset.associateWithDomain(domain1, "NormalAsset", "NEW")

        when: "risks are added"
        def risks = asset.obtainRisks([scenario1, scenario2] as Set, [] as Set)

        then: "the asset has new risks"
        asset.risks.size() == 2

        when: "risks are removed"
        asset.removeRisks(risks)

        then: "the risks are gone"
        asset.risks.size() == 0
    }

    def "A risk may be linked to a risk owner"() {
        given: "defined entities"
        def asset = newAsset(unit)
        def scenario = newScenario(unit)
        def domain1 = newDomain(client)
        asset.associateWithDomain(domain1, "NormalAsset", "NEW")
        def person = newPerson(unit)

        when: "a risk is created and linked to the person"
        def risk = asset.obtainRisk(scenario)
        risk.appoint(person)

        then: "the person is present"
        risk.entity == asset
        risk.scenario == scenario
        risk.riskOwner == person
    }

    def "A risk may be linked to a composite of risk owners"() {
        given: "defined entities"
        def asset = newAsset(unit)
        def scenario = newScenario(unit)
        def domain1 = newDomain(client)
        asset.associateWithDomain(domain1, "NormalAsset", "NEW")
        def person = newPerson(unit)
        def personComposite = newPerson(unit)
        personComposite.parts = [person]

        when: "a risk is created and linked to the personComposite"
        def risk = asset.obtainRisk(scenario)
        risk.appoint(personComposite)

        then: "the personComposite is present"
        risk.entity == asset
        risk.scenario == scenario
        risk.riskOwner == personComposite
        risk.riskOwner.parts.first() == person
    }

    def "The risk may apply to a composite of assets"() {
        given: "a composite of assets"
        def asset1 = newAsset(unit)
        def asset2 = newAsset(unit)
        def domain1 = newDomain(client)
        def assetComposite = newAsset(unit) {
            name = "assetcomposite"
        }
        assetComposite.associateWithDomain(domain1, "NormalAsset", "NEW")
        assetComposite.setParts([asset1, asset2] as Set)
        def scenario = newScenario(unit)

        when: "a risk is created"
        def risk = assetComposite.obtainRisk(scenario)

        then: "the composite of assets is a valid reference"
        risk.entity == assetComposite
        risk.entity.name == "assetcomposite"
        risk.entity.parts.contains(asset1)
        risk.entity.parts.contains(asset2)
    }

    def "The risk may apply to a composite of scenarios"() {
        given: "a composite of scenarios"
        def scenarioComposite = newScenario(unit)
        def scenario1 = newScenario(unit)
        def scenario2 = newScenario(unit)
        scenarioComposite.setParts([scenario1, scenario2] as Set)
        def asset = newAsset(unit)
        def domain1 = newDomain(client)
        asset.associateWithDomain(domain1, "NormalAsset", "NEW")

        when: "a risk is created"
        def risk = asset.obtainRisk(scenarioComposite)

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
        def asset1 = newAsset(unit)
        def scenario1 = newScenario(unit)
        def domain1 = newDomain(client)
        asset1.associateWithDomain(domain1, "NormalAsset", "NEW")

        when: "a risk is created"
        def risk = asset1.obtainRisk(scenario1)
        risk.mitigate(controlComposite)

        then: "the composite of controls is a valid reference"
        risk.mitigation == controlComposite
        risk.mitigation.parts.contains(control1)
        risk.mitigation.parts.contains(control2)
    }

    def "There may be only one risk for the same asset and scenario"() {
        given: ""
        def scenario1 = newScenario(unit)
        def asset1 = newAsset(unit)
        def domain1 = newDomain(client)
        asset1.associateWithDomain(domain1, "NormalAsset", "NEW")
        def risk1 = asset1.obtainRisk(scenario1)
        def set = new HashSet<AssetRisk>()
        set.add(risk1)

        when: "another risk is created"
        def risk2 = asset1.obtainRisk(scenario1)
        set.add(risk2)

        then: "it has the same identity"
        risk1 == risk2
        set.size() == 1
        set.first() == risk1
    }
}
