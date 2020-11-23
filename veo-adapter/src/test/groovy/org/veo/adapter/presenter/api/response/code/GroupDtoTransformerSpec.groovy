/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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

import java.time.Instant

import org.veo.adapter.presenter.api.common.ModelObjectReference
import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.dto.full.FullAssetGroupDto
import org.veo.adapter.presenter.api.dto.full.FullEntityLayerSupertypeGroupDto
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext
import org.veo.adapter.presenter.api.response.transformer.SubTypeTransformer
import org.veo.core.entity.Asset
import org.veo.core.entity.Document
import org.veo.core.entity.EntityTypeNames
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.groups.AssetGroup
import org.veo.core.entity.transform.EntityFactory

import spock.lang.Specification

class GroupDtoTransformerSpec extends Specification {
    def unitName = "Test unit"
    def unitId = "2e63d3f8-b326-4304-84e6-c12efbbcaaa4"
    def subUnitName = "Test subunit"
    def subUnitId = "fb329c3e-b87b-44d2-a680-e2d12539f3f7"

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

    def "Transform simple group to DTO"() {
        given: "A unit with a group"
        Unit unit = createUnit()

        AssetGroup assetGroup = Mock()
        ReferenceAssembler assembler = Mock()

        //        assetGroup.setId(Key.newUuid())

        assetGroup.getOwner() >> unit
        assetGroup.getName() >> "AssetGroupInstanceName"
        assetGroup.getId() >> Key.newUuid()
        assetGroup.getDomains() >> []
        assetGroup.getLinks() >> []
        assetGroup.getLinks() >> []
        assetGroup.getCustomAspects() >> []
        assetGroup.getOwner() >> unit
        assetGroup.getModelInterface >> Asset.class
        assetGroup.getMembers() >> []
        assetGroup.getCreatedAt() >> Instant.now()
        assetGroup.getUpdatedAt() >> Instant.now()


        when: "the group is transformed into a DTO"
        def dto = FullEntityLayerSupertypeGroupDto.from(assetGroup, assembler)

        then: "The DTO contains all required data"
        dto.name == assetGroup.name
        dto.owner.displayName == unitName

    }

    def "Transform group with members to DTO"() {
        given: "A group with two members"
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

        AssetGroup assetGroup = Mock(AssetGroup) {
            it.id >> Key.newUuid()
            it.name >> "AssetGroup"
            it.domains >> []
            it.links >> []
            it.customAspects >> []
            it.members >> ([asset1, asset2] as Set)
            it.modelInterface >> Asset.class
            it.createdAt >> Instant.now()
            it.updatedAt >> Instant.now()
            it.modelType >> EntityTypeNames.ASSET
        }

        ReferenceAssembler assembler = Mock()

        when: "the group is transformed to a DTO"
        def assetGroupDto = FullEntityLayerSupertypeGroupDto.from(assetGroup, assembler)
        then: "the DTO contains references to both members"
        assetGroupDto.members.size() == 2
        assetGroupDto.members.sort { it.displayName }*.displayName == [
            asset1.displayName,
            asset2.displayName
        ]
    }

    def "Transform group DTO with members to entity"() {
        given: "an asset group DTO with two members"
        def asset1Ref = Mock(ModelObjectReference)
        def asset2Ref = Mock(ModelObjectReference)
        def asset1 = Mock(Asset)
        def asset2 = Mock(Asset)
        def newAssetGroupEntity = Mock(AssetGroup) {
            it.modelType >> EntityTypeNames.ASSET
        }
        def context = Mock(DtoToEntityContext) {
            it.factory >> Mock(EntityFactory)
            it.subTypeTransformer >> Mock(SubTypeTransformer)
        }
        def assetGroupId = Key.newUuid()
        def assetGroupDto = new FullAssetGroupDto().tap {
            id = assetGroupId.uuidValue()
            name = "AssetGroup"
            setMembers([
                asset1Ref,
                asset2Ref
            ] as Set)
        }

        when: "transforming the DTO to an entity"
        def result = assetGroupDto.toEntity(context)

        then: "the group is transformed with members"
        1 * context.factory.createAssetGroup(assetGroupId, "AssetGroup", null) >> newAssetGroupEntity
        1 * context.resolve(asset1Ref) >> asset1
        1 * context.resolve(asset2Ref) >> asset2
        result == newAssetGroupEntity
        1 * newAssetGroupEntity.setMembers([asset1, asset2].toSet())
        1 * context.subTypeTransformer.mapSubTypesToEntity(assetGroupDto, newAssetGroupEntity)
    }

    def "Transform group that contains itself"() {
        given: "A group that contains itself"

        AssetGroup assetGroup = Mock()
        assetGroup.id >> (Key.newUuid())
        assetGroup.name >> "AssetGroupInstanceName"
        assetGroup.domains >> []
        assetGroup.links >> []
        assetGroup.customAspects >> []
        assetGroup.modelInterface >> Document.class
        assetGroup.members >> ([assetGroup] as Set)
        assetGroup.createdAt >> Instant.now()
        assetGroup.updatedAt >> Instant.now()

        def refAssembler = Mock(ReferenceAssembler)

        when: "the group is transformed"
        def ag = FullEntityLayerSupertypeGroupDto.from(assetGroup, refAssembler)

        then: "The group contains it self as it is not forbidden"
        ag.members == [
            ModelObjectReference.from(assetGroup, refAssembler)
        ] as Set
    }
}
