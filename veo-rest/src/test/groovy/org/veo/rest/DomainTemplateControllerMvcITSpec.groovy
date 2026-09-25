/*
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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
 */
package org.veo.rest

import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec

@WithUserDetails("admin")
class DomainTemplateControllerMvcITSpec extends VeoMvcSpec {

    def setup() {
        createTestClient()
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
    }

    def "list all domain templates"() {
        given:
        createTestDomainTemplate(TEST_DOMAIN_TEMPLATE_ID)

        when:
        def templates = parseJson(get("/domain-templates"))

        then:
        templates.size() == 2
        with(templates.find {
            it.name == "DS-GVO"
        }) {
            id instanceof String
            templateVersion == "1.4.0"
            createdAt instanceof String
            _self.endsWith("/domain-templates/$id")
        }
        with(templates.find {
            it.name == "test-domain"
        }) {
            id instanceof String
            templateVersion == "1.0.0"
            createdAt instanceof String
            _self.endsWith("/domain-templates/$id")
        }
    }
}
