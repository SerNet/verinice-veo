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
package org.veo.rest

import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.web.bind.MissingServletRequestParameterException

import org.veo.categories.MapGetProperties
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.exception.NotFoundException

import spock.util.mop.Use

/**
 * Integration test for the schema controller. Uses mocked spring MVC environment.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
@Use(MapGetProperties)
class SchemaControllerMockMvcSpec extends VeoMvcSpec {

    String domainId

    def setup() {
        domainId = createTestDomain(createTestClient(), DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
    }

    @WithUserDetails("user@domain.example")
    def "get the schema for a process"() {
        when: "a request for a schema is made"
        def schema = parseJson(get("/schemas/process?domains=$domainId"))

        then: "a correct response is returned"
        schema.title == "process"

        and: "the links are present"
        schema.properties.links.properties.process_dataType != null

        and: "the custom aspects are present"
        schema.properties.customAspects.properties.process_processingDetails != null

        and: "the domain association is present"
        with(schema.properties.domains.properties[domainId]) {
            it.properties.subType.enum.contains("PRO_DataTransfer")
        }
    }

    @WithUserDetails("user@domain.example")
    def "get the schema for an asset"() {
        when: "a request for the asset schama is made"
        def schema = parseJson(get("/schemas/asset?domains=$domainId"))

        then: "a correct response is returned"
        schema.title == "asset"

        and: "the custom aspects are present"
        schema.properties.customAspects.properties.asset_details != null
    }

    @WithUserDetails("user@domain.example")
    def "get the schema for an unknown domain"() {
        given:
        def someRandomUUID = UUID.randomUUID().toString()

        when: "a request for the asset schema is made"
        parseJson(get("/schemas/asset?domains=${someRandomUUID}", 404))

        then: "the domain is not found"
        thrown(NotFoundException)
    }

    @WithUserDetails("user@domain.example")
    def "domains parameter is required"() {
        when: "a request is made without specifying the domains parameter"
        get('/schemas/asset', 400)

        then:
        thrown(MissingServletRequestParameterException)
    }
}