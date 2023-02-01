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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.EntityType

@WithUserDetails("user@domain.example")
class TypeDefinitionsControllerMockMvcSpec extends VeoMvcSpec{
    def setup() {
        createTestClient()
    }

    def "provides types with working links"() {
        when:
        def result = parseJson(get("/types"))

        then:
        result.size() == EntityType.ELEMENT_TYPES.size()
        result.values().each {
            // Try using the collection URI (removing hateoas param placeholders).
            get(it.collectionUri.replaceFirst("\\{.*\\}", ""))
            // Try using the search URI.
            post(it.searchUri, [:])
            // Try using the schema URI.
            // TODO VEO-663 do the async thing
            assert mvc.perform(MockMvcRequestBuilders.get(it.schemaUri.replace("{domains}", "whatever")))
            .andReturn().response.status == 200
        }
    }
}
