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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.EntityType
import org.veo.core.entity.Key
import org.veo.core.repository.DomainRepository
import org.veo.rest.configuration.WebMvcSecurityConfiguration

@WithUserDetails("user@domain.example")
class SearchMvcITSpec extends VeoMvcSpec {

    @Autowired
    DomainRepository domainRepository

    String domainId
    String unitId

    def setup() {
        def client = clientDataRepository.save(newClient {
            id = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
        })
        domainId = domainRepository.save(newDomain {
            owner = client
            name = "search test domain"
        }).id.uuidValue()
        unitId = unitDataRepository.save(newUnit(client))
    }

    def 'find #type by status'() {
        given: "two #type with different status"
        post("/$type", [
            name: "one",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "SUB_MARINE",
                    status: "NEW"
                ]
            ]
        ])
        post("/$type", [
            name: "two",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "SUB_MARINE",
                    status: "OLD"
                ]
            ]
        ])

        when: "searching for the status of the second item one the collection endpoint"
        def results = parseJson(get("/$type?status=OLD"))

        then: "the second item is returned"
        results.items*.name == ["two"]

        when: "running the same search on the search endpoint"
        def searchUrl = parseJson(post("/$type/searches", [
            status: [
                values: ["OLD"]
            ]
        ])).searchUrl
        results = parseJson(get(searchUrl))

        then: "the result is the same"
        results.items*.name == ["two"]

        where:
        type << EntityType.ELEMENT_TYPES*.pluralTerm
    }
}
