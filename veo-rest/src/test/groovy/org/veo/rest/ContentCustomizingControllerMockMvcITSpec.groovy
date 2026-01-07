/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.method.annotation.HandlerMethodValidationException

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.persistence.access.ClientRepositoryImpl

@WithUserDetails("user@domain.example")
class ContentCustomizingControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    TransactionTemplate txTemplate
    private Domain testDomain
    private Client client

    def setup() {
        txTemplate.execute {
            def rd = createRiskDefinition("rid")

            this.client = createTestClient()
            newDomain(client) {
                name = "Domain 1"
                riskDefinitions = ["rid":rd] as Map
            }
        }
        client = clientRepository.save(client)
        testDomain = client.domains.find{it.name == "Domain 1"}
    }

    def "normal user can change colors and translations"() {
        when: "changing color and translations"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.riskValues.first().translations.(DE).name = "a new name"
        rd.riskValues.first().htmlColor = "#000000"
        rd.probability.translations.(DE).name = "a new prop"
        rd.probability.levels.first().htmlColor = "#000000"
        rd.probability.levels.first().translations.(DE).name = "a new level name"
        rd.categories.find{ it.id == "C" }.translations.(DE).name = "my new dimension name"
        rd.categories.find{ it.id == "C" }.potentialImpacts.first().translations.(DE).name = "my new impact name"
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd)

        then: "the change is persisted"
        with(parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid) {
            riskValues.first().translations.(super.DE).name == "a new name"
            riskValues.first().htmlColor == "#000000"
            probability.translations.(super.DE).name == "a new prop"
            probability.levels.first().htmlColor == "#000000"
            probability.levels.first().translations.(super.DE).name == "a new level name"
            categories.find{ it.id == "C" }.translations.(super.DE).name == "my new dimension name"
            categories.find{ it.id == "C" }.potentialImpacts.first().translations.(super.DE).name == "my new impact name"
        }

        when: "adding the definition as a new definition"
        rd.id = "newId"
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/newId", rd, 201)

        then: "the change is persisted"
        with(parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.newId) {
            riskValues.first().translations.(super.DE).name == "a new name"
            riskValues.first().htmlColor == "#000000"
        }
    }

    def "invalid color codes are rejected"() {
        when:
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.riskValues.first().htmlColor = "#foo"

        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, 400)

        then:
        HandlerMethodValidationException e = thrown()
        with(e.parameterValidationResults.first()  ) {
            with(it.resolvableErrors.first()) {
                it.defaultMessage == 'must match "#[0-9a-fA-F]{6}"'
            }
        }

        when:
        rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.probability.levels.first().htmlColor = "#foo"

        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, 400)

        then:
        e = thrown()
        with(e.parameterValidationResults.first()  ) {
            with(it.resolvableErrors.first()) {
                it.defaultMessage == 'must match "#[0-9a-fA-F]{6}"'
            }
        }

        when:
        rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.categories.first().potentialImpacts.first().htmlColor = "#foo"

        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, 400)

        then:
        e = thrown()
        with(e.parameterValidationResults.first()  ) {
            with(it.resolvableErrors.first()) {
                it.defaultMessage == 'must match "#[0-9a-fA-F]{6}"'
            }
        }
    }

    def "normal user can add or remove riskvalues"() {
        when: "adding a riskvalue"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.riskValues.add(["ordinalValue":4,"htmlColor": "#101010", "symbolicRisk": "risky"])

        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],200)

        then: "the change is persisted"
        with(parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid) {
            riskValues.size() == 5
            riskValues[4].symbolicRisk == "risky"
            riskValues[4].ordinalValue == 4
        }

        when: "removing a riskvalue"
        rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.riskValues.removeLast()
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],200)

        then: "the change is persisted"
        parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid.riskValues.size() == 4
    }

    def"normal user can add a category"() {
        when: "we add a category"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.categories.add(["id": "NEW",
            "translations":["DE": ["name": "new kid on the block"]],
            "potentialImpacts":[
                ["ordinalValue":0,"htmlColor": "#101010"]
            ]
        ])
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],200)

        then: "the change is persisted"
        with(parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid) {
            categories.find{ it.id == "NEW" }.translations.(super.DE).name == "new kid on the block"
            categories.find{ it.id == "NEW" }.potentialImpacts.first().htmlColor == "#101010"
        }
    }

    def"normal user can remove a category"() {
        when: "we remove a category"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.categories.removeLast()
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],200)

        then: "the change is persisted"
        parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid.categories.size() == 4
    }

    def "normal user can add or remove impact of category"() {
        when: "we add a category impact"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.categories.find{ it.id == "C" }.potentialImpacts.add(["ordinalValue":0,"htmlColor": "#101010"])
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],200)

        then: "the change is persisted"
        parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid.categories.find{ it.id == "C" }.potentialImpacts.size() == 5

        when: "we remove a category impact"
        rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.categories.find{ it.id == "C" }.potentialImpacts.removeLast()
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],200)

        then: "the change is persisted"
        parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid.categories.find{ it.id == "C" }.potentialImpacts.size() == 4
    }

    def "normal user can add or remove probability"() {
        when: "we add a probability"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.probability.levels.add(["ordinalValue":0,"htmlColor": "#101010"])
        rd.categories.forEach{ it.valueMatrix = null }
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],200)

        then: "the change is persisted"
        parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid.probability.levels.size() == 5

        when: "we remove the probability"
        rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.probability.levels.removeLast()
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],200)

        then: "the change is persisted"
        parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid.probability.levels.size() == 4
    }

    def "normal user can change the riskMatrix"() {
        when: "we change the a value in the risk matrix"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.categories.find{ it.id == "D" }.valueMatrix[0].addFirst(["ordinalValue":3,"htmlColor": "#101010", "symbolicRisk": "symbolic_risk_4"])
        rd.categories.find{ it.id == "D" }.valueMatrix[0].removeLast()
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],200)

        then: "the change is persisted"
        with(parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid) {
            rd.categories.find{ it.id == "D" }.valueMatrix[0].first().ordinalValue == 3
        }
    }
}
