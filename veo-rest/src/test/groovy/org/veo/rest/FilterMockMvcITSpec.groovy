/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
import org.veo.core.entity.ElementType
import org.veo.core.repository.UnitRepository

@WithUserDetails("user@domain.example")
class FilterMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository

    private UUID unitId
    private UUID domainId

    def setup() {
        def client = createTestClient()
        def domain = domainDataRepository.save(newDomain(client))
        domainId = domain.id
        client = clientRepository.getById(client.id)
        unitId = unitRepository.save(newUnit(client) {
            domains = [domain]
        }).id
    }

    def "filter #type.pluralTerm by abbreviation"() {
        given:
        txTemplate.execute {
            domainDataRepository.findById(domainId).get().getElementTypeDefinition(type).with {
                subTypes["megaType"] = newSubTypeDefinition()
            }
        }
        post("/domains/$domainId/$type.pluralTerm", [
            name: "number one",
            abbreviation: "n1",
            subType: "megaType",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"]
        ])
        post("/domains/$domainId/$type.pluralTerm", [
            name: "number two",
            abbreviation: "n2",
            subType: "megaType",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"]
        ])
        post("/domains/$domainId/$type.pluralTerm", [
            name: "something else",
            abbreviation: "se",
            subType: "megaType",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"]
        ])

        expect: "elements to be found by partial abbreviation search string"
        with(parseJson(get("/domains/$domainId/$type.pluralTerm?abbreviation=n"))) {
            items*.name ==~ ["number one", "number two"]
        }

        and: "by full abbreviation"
        with(parseJson(get("/domains/$domainId/$type.pluralTerm?abbreviation=n2"))) {
            items*.name ==~ ["number two"]
        }

        and: "filter should be case-insensitive"
        with(parseJson(get("/domains/$domainId/$type.pluralTerm?abbreviation=N2"))) {
            items*.name ==~ ["number two"]
        }

        where:
        type << ElementType.values()
    }
}