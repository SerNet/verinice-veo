/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.EntityType

@WithUserDetails("user@domain.example")
class SearchMvcITSpec extends VeoMvcSpec {

    String domainId
    String unitId
    Client client

    def subTypes = [assets: "AST_Application",
        controls: "CTL_TOM",
        documents: "DOC_Document",
        incidents: "INC_Incident",
        persons: "PER_Person",
        processes: "PRO_DataProcessing",
        scenarios: "SCN_Scenario",
        scopes: "SCP_ResponsibleBody"]

    def setup() {
        client = createTestClient()
        domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
        client = clientRepository.getById(client.id)
        unitId = unitDataRepository.save(newUnit(client)).idAsString
    }

    def "search subtype and domain"() {
        given:
        newDomain(client) {
            name = "Domain 1"
            applyElementTypeDefinition(newElementTypeDefinition("scope", it) {
                subTypes = [
                    SCP_Scope: newSubTypeDefinition {
                    }
                ]
            })
            applyElementTypeDefinition(newElementTypeDefinition("scope", it) {
                subTypes = [
                    SCP_Institution: newSubTypeDefinition {
                    }
                ]
            })
        }

        client = clientRepository.save(client)
        def testDomain = client.domains.find{it.name == "Domain 1"}
        def testDomainId = testDomain.idAsString

        unitId = unitDataRepository.save(newUnit(client)).idAsString
        def scopeId1 = parseJson(post("/domains/$domainId/scopes", [
            name: "Scope-1",
            abbreviation: "DT",
            description: "from domain",
            owner: [targetUri: "/units/$unitId"],
            subType: "SCP_Scope",
            status: "NEW",
        ])).resourceId

        post("/domains/$testDomainId/scopes/$scopeId1",[subType: "SCP_Institution",
            status: "NEW"],200)

        when:
        def results = parseJson(get("/domains/$testDomainId/scopes?unit=$unitId&subType=SCP_Scope"))

        then:
        results.items.size() == 0
    }

    def 'find #type by status'() {
        given: "two #type with different status"
        String subType = subTypes.get(type)
        post("/domains/$domainId/$type", [
            name: "one",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: subType,
            status: "NEW"
        ])
        post("/domains/$domainId/$type", [
            name: "two",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: subType,
            status: "IN_PROGRESS"
        ])

        when: "searching for the status of the second item one the collection endpoint"
        def results = parseJson(get("/$type?status=IN_PROGRESS"))

        then: "the second item is returned"
        results.items*.name == ["two"]

        where:
        type << EntityType.ELEMENT_TYPES*.pluralTerm
    }

    def 'find by displayname'() {
        given: "a scenario with German characters in the name"
        post("/domains/$domainId/scenarios", [
            name: "Rechtswidrige bzw. unrechtmäßige Verabeitung personenbezogener Daten",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: 'SCN_Scenario',
            status: "NEW"
        ])

        when: "searching for the scenario"
        def results = parseJson(get("/scenarios?displayName=unrechtmäßige"))

        then: "the item is returned"
        results.items*.name == [
            "Rechtswidrige bzw. unrechtmäßige Verabeitung personenbezogener Daten"
        ]
    }
}
