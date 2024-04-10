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
package org.veo.rest.common.marshalling

import org.springframework.beans.factory.annotation.Autowired

import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.core.VeoSpringSpec
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Domain
import org.veo.core.entity.DomainBase
import org.veo.core.entity.Entity
import org.veo.core.entity.EntityType
import org.veo.core.entity.Identifiable
import org.veo.core.entity.Key
import org.veo.core.entity.Profile
import org.veo.core.entity.ProfileItem
import org.veo.core.entity.SymIdentifiable
import org.veo.rest.configuration.TypeExtractor

class ReferenceAssemblerImplITSpec extends VeoSpringSpec {

    @Autowired
    TypeExtractor typeExtractor

    ReferenceAssembler referenceAssembler

    def setup() {
        referenceAssembler = new ReferenceAssemblerImpl(typeExtractor)
    }

    def "creates and parses URLs for #type.simpleName"() {
        given:
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

    def "parsed namespace type for #url is #type"() {
        expect:
        referenceAssembler.parseNamespaceType(url) == type

        where:
        url                                                                                                                                                           | type
        'http://localhost:9000/domains/37aa44d5-3707-416a-864c-839f97535a06/catalog-items/a149f709-3055-40a2-867a-aaf6b2ccc36d'                                       | Domain
        'http://localhost:9000/domains/37aa44d5-3707-416a-864c-839f97535a06/profiles/02557ca8-a579-4e53-a161-b1433b62eb77/items/a149f709-3055-40a2-867a-aaf6b2ccc36d' | Profile
    }

    def createEntity(Class<Entity> type) {
        Stub(type) {
            getModelInterface() >> type
            if (it instanceof Identifiable) {
                id >> Key.newUuid()
                idAsString >> it.id.uuidValue()
            }
            if (it instanceof SymIdentifiable) {
                symbolicId >> Key.newUuid()
                symbolicIdAsString >> it.symbolicId.uuidValue()
            }
            if (it instanceof CatalogItem) {
                domainBase >> Stub(Domain) {
                    id >> Key.newUuid()
                    idAsString >> it.id.uuidValue()
                }
            }
            if (it instanceof Profile) {
                owner >> Stub(DomainBase) {
                    id >> Key.newUuid()
                    idAsString >> it.id.uuidValue()
                }
            }
            if (it instanceof ProfileItem) {
                owner >> Stub(Profile) {
                    id >> Key.newUuid()
                    idAsString >> it.id.uuidValue()
                }
                domainBase >> Stub(Domain) {
                    id >> Key.newUuid()
                    idAsString >> it.id.uuidValue()
                }
            }
        }
    }
}
