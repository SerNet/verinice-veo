/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.presenter.api.response.code

import org.veo.adapter.presenter.api.common.ModelObjectReference
import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.dto.CustomLinkDto
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer
import org.veo.adapter.presenter.api.response.transformer.EntitySchema
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer
import org.veo.core.entity.Asset
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Key
import org.veo.core.entity.transform.EntityFactory

import spock.lang.Specification

class CustomLinkTransformerSpec extends Specification {

    def referenceAssembler = Mock(ReferenceAssembler)
    def entityToDtoTransformer = new EntityToDtoTransformer(referenceAssembler)
    def dtoToEntityTransformer = new DtoToEntityTransformer()

    def "transform custom link entity to DTO"() {
        given: "a custom link"
        def targetAsset = Mock(Asset) {
            it.id >> Key.newUuid()
            it.modelInterface >> Asset
        }
        def link = Mock(CustomLink) {
            it.applicableTo >> ["asset", "process"]
            it.name >> "good name"
            it.target >> targetAsset
            it.getAllProperties() >> [
                "foo": "bar"
            ]
        }

        when: "transforming it to a DTO"
        def dto = entityToDtoTransformer.transformCustomLink2Dto(link)

        then: "all properties are transformed"
        with(dto) {
            applicableTo == Set.of("asset", "process")
            name == "good name"
            target == ModelObjectReference.from(targetAsset, referenceAssembler)
            with(attributes) {
                get("foo") == "bar"
            }
        }
    }

    def "transform custom link DTO to entity"() {
        given: "a custom link"
        def targetAsset = Mock(Asset) {
            it.id >> Key.newUuid()
            it.modelInterface >> Asset
        }
        def newLink = Mock(CustomLink)
        def schema = Mock(EntitySchema)
        def linkDto = new CustomLinkDto().tap {
            applicableTo = ["asset", "process"]
            name = "good name"
            target = ModelObjectReference.from(targetAsset, Mock(ReferenceAssembler))
            attributes = [
                "foo": "bar"
            ]
        }
        def context = Mock(DtoToEntityContext) {
            it.resolve(linkDto.target) >> targetAsset
            it.factory >> Mock(EntityFactory)
        }

        when: "transforming it to an entity"
        def entity = dtoToEntityTransformer.transformDto2CustomLink(context,linkDto, "good type", schema)

        then: "all properties are transformed"
        1 * context.factory.createCustomLink("good name", targetAsset, null) >> newLink
        entity == newLink
        1 * newLink.setType("good type")
        1 * newLink.setApplicableTo(Set.of("asset", "process"))
        1 * schema.applyLinkAttributes(["foo": "bar"], newLink)
    }
}
