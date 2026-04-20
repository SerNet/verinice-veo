/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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
package org.veo.adapter.presenter.api.response.transformer

import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.state.ElementTypeDefinitionState
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.service.DomainTemplateIdGenerator
import org.veo.core.usecase.service.DomainStateMapper
import org.veo.core.usecase.service.RefResolverFactory
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

import spock.lang.Specification

class DomainStateMapperSpec extends Specification {

    Domain domain = Mock(Domain)

    EntityFactory entityFactory = new EntityDataFactory()
    RefResolverFactory refResolverFactory = Mock()
    DomainTemplateIdGenerator domainTemplateIdGenerator = Mock()
    DomainStateMapper domainStateMapper = new DomainStateMapper(refResolverFactory, entityFactory, domainTemplateIdGenerator)

    def "changes to the source don't affect the mapping target"() {
        given:
        ElementTypeDefinitionState source = new ElementTypeDefinitionDto().tap {
            links = ['foo': new LinkDefinition()]
        }

        when: "the sub types are mapped"
        def target = domainStateMapper.toElementTypeDefinition(ElementType.ASSET, source, domain)

        then:
        target.links.keySet() ==~ ['foo']

        when:
        source.links.put('bar', new LinkDefinition())

        then:
        target.links.keySet() ==~ ['foo']
    }
}