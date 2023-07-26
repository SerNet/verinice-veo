/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.ClientBoundaryViolationException

/**
 * Integration test for the catalog. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class CatalogControllerMockMvcITSpec extends CatalogSpec {

    @WithUserDetails("user@domain.example")
    def "retrieve a catalog item"() {
        given: "a saved catalogitem with a catalog"

        when: "a request is made to the server"
        def results = get("/catalogs/${domain.id.uuidValue()}/items/${item1.id.uuidValue()}")

        then: "the eTag is set"
        getETag(results) != null

        and:
        def result = parseJson(results)
        result.id == item1.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a catalog item wrong client"() {
        given: "a saved catalogitem with a catalog"

        when: "a request is made to the server"
        get("/catalogs/${domain3.id.uuidValue()}/items/${otherItem.id.uuidValue()}", 404)

        then: "the data is rejected"
        thrown(NotFoundException)
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all items for a catalog"() {
        given: "the created catalogitems"

        when: "a request is made to the server"
        def result = parseJson(get("/catalogs/${domain.idAsString}/items"))

        then: "the domains are returned"
        result.size() == domain.catalogItems.size()

        when: "the catalog item 'item4' is retrieved from the list of items"
        def item4FromResult = result.find { it.id == item4.id.uuidValue() }

        then: "the catalog item contains the element's description"
        item4FromResult.description == item4.description
        item4FromResult._self == "http://localhost/catalogs/${domain.idAsString}/items/${item4.id.uuidValue()}"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all items for a catalog wrong client"() {
        given: "the created catalogitems"

        when: "a request is made to the server"
        get("/catalogs/${domain3.idAsString}/items", 404)

        then: "the data is rejected"
        thrown(NotFoundException)
    }
}