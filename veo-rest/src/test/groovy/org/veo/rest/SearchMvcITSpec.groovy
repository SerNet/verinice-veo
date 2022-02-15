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
import org.veo.core.entity.EntityType

@WithUserDetails("user@domain.example")
class SearchMvcITSpec extends VeoMvcSpec {

    String domainId
    String unitId

    def subTypes = [assets: "AST_Application",
        controls: "CTL_TOM",
        documents: "DOC_Document",
        incidents: "INC_Incident",
        persons: "PER_Person",
        processes: "PRO_DataProcessing",
        scenarios: "SCN_Scenario",
        scopes: "SCP_ResponsibleBody"]

    def setup() {
        def client = createTestClient()
        domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).id.uuidValue()
        unitId = unitDataRepository.save(newUnit(client)).id.uuidValue()
    }

    def 'find #type by status'() {
        given: "two #type with different status"
        String subType = subTypes.get(type)
        post("/$type", [
            name: "one",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: subType,
                    status: "NEW"
                ]
            ]
        ])
        post("/$type", [
            name: "two",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: subType,
                    status: "IN_PROGRESS"
                ]
            ]
        ])

        when: "searching for the status of the second item one the collection endpoint"
        def results = parseJson(get("/$type?status=IN_PROGRESS"))

        then: "the second item is returned"
        results.items*.name == ["two"]

        when: "running the same search on the search endpoint"
        def searchUrl = parseJson(post("/$type/searches", [
            status: [
                values: ["IN_PROGRESS"]
            ]
        ])).searchUrl
        results = parseJson(get(new URI(searchUrl)))

        then: "the result is the same"
        results.items*.name == ["two"]

        where:
        type << EntityType.ELEMENT_TYPES*.pluralTerm
    }
}
