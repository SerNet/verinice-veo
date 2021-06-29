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

import org.apache.commons.codec.digest.DigestUtils
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.entity.Catalog
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
    def "retrieve a catalog"() {
        given: "a catalog"

        when: "a request is made to the server"
        def result = parseJson(get("/catalogs/${catalog.id.uuidValue()}"))

        then: "the catalog is found"
        result.id == catalog.id.uuidValue()
        result.domainTemplate.targetUri == "http://localhost/domains/"+domain.id.uuidValue()

        and: "it contains a reference to its items"
        result.catalogItems.size() == catalog.catalogItems.size()
        result.catalogItems*.targetUri.find {
            it.contains(item1.dbId)
        } != null
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a catalog for wrong client"() {
        given: "a catalog"

        when: "a request is made to the server"
        def results = get("/catalogs/${catalog1.id.uuidValue()}", false)

        then: "the data is rejected"
        ClientBoundaryViolationException ex = thrown()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all catalogs"() {
        given: "two catalogs"

        Catalog catalog1 = newCatalog(domain1) {
            name = 'c'
        }

        txTemplate.execute {
            domain1 = domainRepository.save(domain1)
        }
        catalog1 = domain1.catalogs.first()

        when: "a request is made to the server"
        def result = parseJson(get("/catalogs?"))

        then: "the catalogs are found"
        result.size() == 2
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a catalog item"() {
        given: "a saved catalogitem with a catalog"

        when: "a request is made to the server"
        def results = get("/catalogs/${catalog.id.uuidValue()}/items/${item1.id.uuidValue()}")
        String expectedETag = DigestUtils.sha256Hex(item1.id.uuidValue() + "_" + salt + "_" + Long.toString(domain.version))

        then: "the eTag is set"
        String eTag = getETag(results)
        eTag != null
        getTextBetweenQuotes(eTag) == expectedETag
        and:
        def result = parseJson(results)
        result.id == item1.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a catalog item wrong client"() {
        given: "a saved catalogitem with a catalog"

        when: "a request is made to the server"
        def results = get("/catalogs/${catalog1.id.uuidValue()}/items/${otherItem.id.uuidValue()}", false)

        then: "the data is rejected"
        NotFoundException ex = thrown()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all items for a catalog"() {
        given: "the created catalogitems"

        when: "a request is made to the server"
        def result = parseJson(get("/catalogs/${catalog.dbId}/items"))

        then: "the domains are returned"
        result.size == catalog.catalogItems.size()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all items for a catalog wrong client"() {
        given: "the created catalogitems"

        when: "a request is made to the server"
        def results = get("/catalogs/${catalog1.dbId}/items", false)

        then: "the data is rejected"
        NotFoundException ex = thrown()
    }
}