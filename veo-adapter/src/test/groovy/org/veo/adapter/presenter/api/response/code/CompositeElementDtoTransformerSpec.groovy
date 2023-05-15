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
package org.veo.adapter.presenter.api.response.code

import java.time.Instant

import org.veo.adapter.presenter.api.common.IdRef
import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer
import org.veo.core.entity.Asset
import org.veo.core.entity.Document
import org.veo.core.entity.Key
import org.veo.core.entity.Unit

import spock.lang.Specification

class CompositeElementDtoTransformerSpec extends Specification {
    def unitName = "Test unit"
    def unitId = "2e63d3f8-b326-4304-84e6-c12efbbcaaa4"
    def subUnitName = "Test subunit"
    def subUnitId = "fb329c3e-b87b-44d2-a680-e2d12539f3f7"

    def refAssembler = Mock(ReferenceAssembler)
    def domainAssociationTransformer = Mock(DomainAssociationTransformer)
    def entityToDtoTransformer = new EntityToDtoTransformer(refAssembler, domainAssociationTransformer)

    def createUnit() {
        Unit subUnit = Mock()

        subUnit.getClient() >> null
        subUnit.getDomains() >> []
        subUnit.getName() >> subUnitName
        subUnit.getId() >> Key.uuidFrom(subUnitId)
        subUnit.getUnits() >> []
        subUnit.getModelInterface() >> Unit.class
        subUnit.getDisplayName() >> subUnitName

        Unit unit = Mock()
        unit.getClient() >> null
        unit.getDomains() >> []
        unit.getParent() >> null
        unit.getName() >> unitName
        unit.getId() >> Key.uuidFrom(unitId)
        unit.getUnits() >> [subUnit]
        unit.getModelInterface() >> Unit.class
        unit.getDisplayName() >> unitName

        subUnit.getParent() >> unit
        return unit
    }

    def "Transform simple composite element to DTO"() {
        given: "A unit with a composite element"
        Unit unit = createUnit()

        Asset compositeAsset = Mock()

        //        compositeAsset.setId(Key.newUuid())

        compositeAsset.getOwner() >> unit
        compositeAsset.getName() >> "Composite Asset"
        compositeAsset.getId() >> Key.newUuid()
        compositeAsset.getDomains() >> []
        compositeAsset.getLinks() >> []
        compositeAsset.getLinks() >> []
        compositeAsset.getCustomAspects() >> []
        compositeAsset.getOwner() >> unit
        compositeAsset.getModelInterface >> Asset.class
        compositeAsset.getParts() >> []
        compositeAsset.getCreatedAt() >> Instant.now()
        compositeAsset.getUpdatedAt() >> Instant.now()

        when: "the composite element is transformed into a DTO"
        def dto = entityToDtoTransformer.transformAsset2Dto(compositeAsset)

        then: "The DTO contains all required data"
        dto.name == compositeAsset.name
        dto.owner.displayName == unitName
    }

    def "Transform composite element with parts to DTO"() {
        given: "A composite element with two parts"
        Asset asset1 = Mock(Asset) {
            it.id >> Key.newUuid()
            it.displayName >> "Asset 1"
            it.modelInterface >> Asset
        }

        Asset asset2 = Mock(Asset) {
            it.id >> Key.newUuid()
            it.displayName >> "Asset 2"
            it.modelInterface >> Asset
        }

        Asset compositeAsset = Mock(Asset) {
            it.id >> Key.newUuid()
            it.name >> "Composite Asset"
            it.domains >> []
            it.links >> []
            it.customAspects >> []
            it.parts >> ([asset1, asset2] as Set)
            it.modelInterface >> Asset.class
            it.createdAt >> Instant.now()
            it.updatedAt >> Instant.now()
            it.modelType >> Asset.SINGULAR_TERM
        }

        when: "the composite element is transformed to a DTO"
        def compositeAssetDto = entityToDtoTransformer.transformAsset2Dto(compositeAsset)

        then: "the DTO contains references to both parts"
        compositeAssetDto.parts.size() == 2
        compositeAssetDto.parts.sort {
            it.displayName
        }*.displayName == [
            asset1.displayName,
            asset2.displayName
        ]
    }

    def "Transform composite element that contains itself"() {
        given: "A composite element that contains itself"
        Asset compositeAsset = Mock()
        compositeAsset.id >> (Key.newUuid())
        compositeAsset.name >> "Composite Asset"
        compositeAsset.domains >> []
        compositeAsset.links >> []
        compositeAsset.customAspects >> []
        compositeAsset.modelInterface >> Document.class
        compositeAsset.parts >> ([compositeAsset] as Set)
        compositeAsset.createdAt >> Instant.now()
        compositeAsset.updatedAt >> Instant.now()

        when: "the composite element is transformed"
        def dto = entityToDtoTransformer.transformAsset2Dto(compositeAsset)

        then: "The composite element contains itself as it is not forbidden"
        dto.parts == [
            IdRef.from(compositeAsset, refAssembler)
        ] as Set
    }
}
