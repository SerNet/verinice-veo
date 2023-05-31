/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
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
package org.veo.adapter.presenter.api.response.code

import org.veo.adapter.presenter.api.common.IdRef
import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.dto.CustomLinkDto
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer
import org.veo.core.entity.Asset
import org.veo.core.entity.CustomLink
import org.veo.core.entity.DomainBase
import org.veo.core.entity.Key
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.entity.transform.IdentifiableFactory
import org.veo.core.usecase.service.IdRefResolver

import spock.lang.Specification

class CustomLinkTransformerSpec extends Specification {

    def referenceAssembler = Mock(ReferenceAssembler)
    def factory = Mock(EntityFactory)
    def domainAssociationTransformer = Mock(DomainAssociationTransformer)
    def idRefResolver = Mock(IdRefResolver)
    def entityToDtoTransformer = new EntityToDtoTransformer(referenceAssembler, domainAssociationTransformer)
    def dtoToEntityTransformer = new DtoToEntityTransformer(factory, Mock(IdentifiableFactory), domainAssociationTransformer)

    def "transform custom link entity to DTO"() {
        given: "a custom link"
        def targetAsset = Mock(Asset) {
            it.id >> Key.newUuid()
            it.modelInterface >> Asset
        }
        def link = Mock(CustomLink) {
            it.name >> "good name"
            it.target >> targetAsset
            it.attributes >> [:]
            it.domains >> []
        }

        when: "transforming it to a DTO"
        def dto = entityToDtoTransformer.transformCustomLink2Dto(link)

        then: "all properties are transformed"
        with(dto) {
            target == IdRef.from(targetAsset, referenceAssembler)
            attributes == link.attributes
        }
    }

    def "transform custom link DTO to entity"() {
        given: "a custom link"
        def domain = Mock(DomainBase)
        def targetAsset = Mock(Asset) {
            it.id >> Key.newUuid()
            it.modelInterface >> Asset
        }
        def newLink = Mock(CustomLink)
        def linkDto = new CustomLinkDto().tap {
            target = IdRef.from(targetAsset, Mock(ReferenceAssembler))
            attributes = [:]
        }

        when: "transforming it to an entity"
        def entity = dtoToEntityTransformer.transformDto2CustomLink(linkDto, "goodType", idRefResolver, domain)

        then: "all properties are transformed"
        1 * idRefResolver.resolve(linkDto.target) >> targetAsset
        1 * factory.createCustomLink(targetAsset, null, "goodType", domain) >> newLink
        entity == newLink
        1 * newLink.setAttributes(linkDto.attributes)
    }
}
