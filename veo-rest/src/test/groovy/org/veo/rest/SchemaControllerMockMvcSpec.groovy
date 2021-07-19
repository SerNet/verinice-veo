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

import org.veo.core.VeoMvcSpec
/**
 * Integration test for the schema controller. Uses mocked spring MVC environment.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class SchemaControllerMockMvcSpec extends VeoMvcSpec {

    @WithUserDetails("user@domain.example")
    def "get the schema for a process"() {
        when: "a request for a schema is made"
        def schema = parseJson(get('/schemas/process?domains=DSGVO'))

        then: "a correct response is returned"
        schema.title == "Process"

        and: "the custom links are present"
        schema.properties.links.properties.process_dataType != null

        and: "the custom aspects are present"
        schema.properties.customAspects.properties.process_processingDetails != null
    }

    @WithUserDetails("user@domain.example")
    def "get the schema for an asset"() {
        when: "a request for an asset is made"
        def schema = parseJson(get('/schemas/asset?domains=DSGVO'))

        then: "a correct response is returned"
        schema.title == "Asset"

        and: "the custom aspects are present"
        schema.properties.customAspects.properties.asset_details != null
    }
}