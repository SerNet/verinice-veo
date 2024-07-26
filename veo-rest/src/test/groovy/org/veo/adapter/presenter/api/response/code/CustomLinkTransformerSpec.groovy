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
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer
import org.veo.core.entity.Asset
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Domain
import org.veo.core.entity.Key

import spock.lang.Specification

class CustomLinkTransformerSpec extends Specification {

    def referenceAssembler = Mock(ReferenceAssembler)
    def domainAssociationTransformer = Mock(DomainAssociationTransformer)
    def entityToDtoTransformer = new EntityToDtoTransformer(referenceAssembler, domainAssociationTransformer)

    def "transform custom link entity to DTO"() {
        given: "a custom link"
        def domain = Mock(Domain) {
            it.id >> Key.newUuid()
            it.idAsString >> it.id.uuidValue()
            it.modelInterface >> Domain
        }
        def targetAsset = Mock(Asset) {
            it.id >> Key.newUuid()
            it.idAsString >> it.id.uuidValue()
            it.modelInterface >> Asset
        }
        def link = Mock(CustomLink) {
            it.name >> "good name"
            it.target >> targetAsset
            it.attributes >> [:]
            it.domain >> domain
        }

        when: "transforming it to a DTO"
        def dto = entityToDtoTransformer.transformCustomLink2Dto(link)

        then: "all properties are transformed"
        with(dto) {
            target == IdRef.from(targetAsset, referenceAssembler)
            attributes == link.attributes
            domains*.id ==~ [domain.id.uuidValue()]
        }
    }
}
