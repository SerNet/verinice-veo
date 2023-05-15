/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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

import org.veo.adapter.presenter.api.dto.AbstractProcessDto
import org.veo.adapter.presenter.api.dto.ProcessDomainAssociationDto
import org.veo.adapter.presenter.api.dto.full.FullAssetDto
import org.veo.core.entity.Asset
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.ref.ITypedId
import org.veo.core.usecase.service.EntityStateMapper
import org.veo.core.usecase.service.IdRefResolver

import spock.lang.Specification

class EntityStateMapperSpec extends Specification {

    Domain domain0 = Mock(Domain) { it.id >> Key.newUuid() }
    Domain domain1 = Mock(Domain) { it.id >> Key.newUuid() }
    IdRefResolver idRefResolver = Mock() {
        resolve(domain0.id.uuidValue(), Domain) >> domain0
        resolve(domain1.id.uuidValue(), Domain) >> domain1
    }
    EntityStateMapper entityStateMapper = new EntityStateMapper()

    def "maps domains from DTO to entity"() {
        given: "a process with two domains and a DTO with different sub types for those domains"
        AbstractProcessDto dto = Mock()
        Process entity = Mock()
        entity.modelInterface >> Process
        dto.domains >> [
            (domain0.id.uuidValue()): Mock(ProcessDomainAssociationDto) {
                subType >> "foo"
                status >> "NEW_FOO"
                riskValues >> [:]
            },
            (domain1.id.uuidValue()): Mock(ProcessDomainAssociationDto) {
                subType >> "bar"
                status >> "NEW_BAR"
                riskValues >> [:]
            }
        ]
        dto.customLinkStates >> []
        dto.customAspectStates >> []

        when: "the sub types are mapped"
        entityStateMapper.mapState(dto, entity, idRefResolver)

        then: "it is set"
        1 * entity.findSubType(domain0) >> Optional.empty()
        1 * entity.findSubType(domain1) >> Optional.empty()
        1 * entity.associateWithDomain(domain0, "foo", "NEW_FOO")
        1 * entity.associateWithDomain(domain1, "bar", "NEW_BAR")
        1 * entity.getLinks() >> []
        1 * entity.getCustomAspects() >> []
    }

    def "Transform composite element DTO with parts to entity"() {
        given: "an asset composite element DTO with two parts"
        def asset1Ref = Mock(ITypedId)
        def asset2Ref = Mock(ITypedId)

        def asset1 = Mock(Asset)
        def asset2 = Mock(Asset)
        def newCompositeAssetEntity = Mock(Asset) {
            it.modelType >> Asset.SINGULAR_TERM
        }

        def compositeAssetId = Key.newUuid()
        def compositeAssetDto = new FullAssetDto().tap {
            id = compositeAssetId.uuidValue()
            name = "Composite Asset"
            setParts([
                asset1Ref,
                asset2Ref
            ] as Set)
        }

        when: "transforming the DTO to an entity"
        entityStateMapper.mapState(compositeAssetDto, newCompositeAssetEntity, idRefResolver)

        then: "the composite element is transformed with parts"
        1 * idRefResolver.resolve(Set.of(asset1Ref, asset2Ref)) >> [asset1, asset2]
        1 * newCompositeAssetEntity.setParts([asset1, asset2].toSet())
        1 * newCompositeAssetEntity.getLinks() >> []
        1 * newCompositeAssetEntity.getCustomAspects() >> []
    }
}
