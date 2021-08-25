/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.rest.common

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Catalog
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.EntityType
import org.veo.core.entity.Key
import org.veo.rest.configuration.TypeExtractor
import org.veo.rest.configuration.WebMvcSecurityConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = WebMvcSecurityConfiguration)
class ReferenceAssemblerImplITSpec extends VeoSpringSpec {

    @Autowired
    TypeExtractor typeExtractor

    def "creates and parses URLs for #type.simpleName"() {
        given:
        def referenceAssembler = new ReferenceAssemblerImpl(typeExtractor)
        def entity = createEntity(type)

        when: "creating a target URL"
        def targetUrl = referenceAssembler.targetReferenceOf(entity)
        then: "it can be parsed back"
        if(targetUrl != null) {
            assert referenceAssembler.parseType(targetUrl) == type
        }

        when: "generating collection & search URLs"
        referenceAssembler.resourcesReferenceOf(type)
        referenceAssembler.searchesReferenceOf(type)

        then:
        notThrown(Exception)

        where:
        type << EntityType.TYPES
    }

    def createEntity(type) {
        def entity = Stub(type) {
            getId () >> Key.newUuid()
            getModelInterface() >> type
        }
        if (type == CatalogItem) {
            def catalog = Stub(Catalog) {
                getId() >> Key.newUuid()
            }
            entity.getCatalog() >> catalog
        }
        entity
    }
}
