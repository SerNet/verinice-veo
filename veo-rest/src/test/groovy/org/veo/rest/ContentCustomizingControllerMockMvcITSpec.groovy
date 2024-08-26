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
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.persistence.access.ClientRepositoryImpl

import spock.lang.Ignore
import spock.lang.IgnoreRest

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
        rd.riskValues.first().htmlColor = "#00000"
        rd.probability.translations.(DE).name = "a new prop"
        rd.probability.levels.first().htmlColor = "#00000"
        rd.probability.levels.first().translations.(DE).name = "a new level name"
        rd.categories.find{ it.id == "C" }.translations.(DE).name = "my new dimension name"
        rd.categories.find{ it.id == "C" }.potentialImpacts.first().translations.(DE).name = "my new impact name"
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd)

        then: "the change is persisted"
        with(parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid) {
            riskValues.first().translations.(DE).name == "a new name"
            riskValues.first().htmlColor == "#00000"
            probability.translations.(DE).name == "a new prop"
            probability.levels.first().htmlColor == "#00000"
            probability.levels.first().translations.(DE).name == "a new level name"
            categories.find{ it.id == "C" }.translations.(DE).name == "my new dimension name"
            categories.find{ it.id == "C" }.potentialImpacts.first().translations.(DE).name == "my new impact name"
        }

        when: "adding the definition as a new definition"
        rd.id = "newId"
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/newId", rd, 201)

        then: "the change is persisted"
        with(parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.newId) {
            riskValues.first().translations.(DE).name == "a new name"
            riskValues.first().htmlColor == "#00000"
        }
    }

    def "normal user cannot add or remove riskvalues"() {
        when: "adding a riskvalue"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.riskValues.add(["ordinalValue":5,"htmlColor": "#101010", "symbolicRisk": "risky"])

        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],422)

        then: "unsupported change"
        UnprocessableDataException ex = thrown()
        ex.message == "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: [NEW_RISK_DEFINITION, TRANSLATION_DIFF, COLOR_DIFF]"

        when: "removing a riskvalue"
        rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.riskValues.removeLast()
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],422)

        then: "unsupported change"
        ex = thrown()
        ex.message == "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: [NEW_RISK_DEFINITION, TRANSLATION_DIFF, COLOR_DIFF]"
    }

    def"normal user cannot add or remove category"() {
        when: "we add a category"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.categories.add(["id": "NEW",
            "potentialImpacts":[
                ["ordinalValue":0,"htmlColor": "#101010"]
            ]
        ])
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],422)

        then: "unsupported change"
        UnprocessableDataException ex = thrown()
        ex.message == "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: [NEW_RISK_DEFINITION, TRANSLATION_DIFF, COLOR_DIFF]"

        when: "we remove a category"
        rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.categories.removeLast()
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],422)

        then: "unsupported change"
        ex = thrown()
        ex.message == "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: [NEW_RISK_DEFINITION, TRANSLATION_DIFF, COLOR_DIFF]"
    }

    def "normal user cannot add or remove impact of category"() {
        when: "we add a category impact"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.categories.find{ it.id == "C" }.potentialImpacts.add(["ordinalValue":0,"htmlColor": "#101010"])
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],422)

        then: "unsupported change"
        UnprocessableDataException ex = thrown()
        ex.message == "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: [NEW_RISK_DEFINITION, TRANSLATION_DIFF, COLOR_DIFF]"

        when: "we remove a category impact"
        rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.categories.find{ it.id == "C" }.potentialImpacts.removeLast()
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],422)

        then: "unsupported change"
        ex = thrown()
        ex.message == "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: [NEW_RISK_DEFINITION, TRANSLATION_DIFF, COLOR_DIFF]"
    }

    def "normal user cannot add or remove probability"() {
        when: "we add a probability"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.probability.levels.add(["ordinalValue":0,"htmlColor": "#101010"])
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],422)

        then: "unsupported change"
        UnprocessableDataException ex = thrown()
        ex.message == "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: [NEW_RISK_DEFINITION, TRANSLATION_DIFF, COLOR_DIFF]"

        when: "we remove the probability"
        rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.probability.levels.removeLast()
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],422)

        then: "unsupported change"
        ex = thrown()
        ex.message == "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: [NEW_RISK_DEFINITION, TRANSLATION_DIFF, COLOR_DIFF]"
    }

    def "normal user cannot change the riskMatrix"() {
        when: "we change the a value in the risk matrix"
        def rd = parseJson(get("/domains/${testDomain.idAsString}")).riskDefinitions.rid
        rd.categories.find{ it.id == "D" }.valueMatrix[0].addFirst(["ordinalValue":5,"htmlColor": "#101010", "symbolicRisk": "risky"])
        rd.categories.find{ it.id == "D" }.valueMatrix[0].removeLast()
        put("/content-customizing/domains/${testDomain.idAsString}/risk-definitions/rid", rd, [:],422)

        then: "unsupported change"
        UnprocessableDataException ex = thrown()
        ex.message == "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: [NEW_RISK_DEFINITION, TRANSLATION_DIFF, COLOR_DIFF]"
    }
}
