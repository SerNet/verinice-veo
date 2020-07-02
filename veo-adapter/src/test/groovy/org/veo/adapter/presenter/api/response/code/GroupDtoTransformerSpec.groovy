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


import org.veo.adapter.presenter.api.common.ModelObjectReference
import org.veo.adapter.presenter.api.response.groups.AssetGroupDto
import org.veo.adapter.presenter.api.response.groups.DocumentGroupDto
import org.veo.adapter.presenter.api.response.groups.EntityLayerSupertypeGroupDto
import org.veo.adapter.presenter.api.response.groups.ProcessGroupDto
import org.veo.adapter.presenter.api.response.transformer.DtoEntityToTargetContext
import org.veo.adapter.presenter.api.response.transformer.DtoTargetToEntityContext
import org.veo.adapter.presenter.api.response.transformer.DtoTargetToEntityTransformer
import org.veo.core.entity.*
import org.veo.core.entity.groups.AssetGroup
import org.veo.core.entity.groups.DocumentGroup
import org.veo.core.entity.groups.ProcessGroup
import org.veo.core.entity.impl.AssetImpl
import org.veo.core.entity.impl.ProcessImpl
import org.veo.core.entity.impl.UnitImpl
import spock.lang.Specification

class GroupDtoTransformerSpec extends Specification {
    def unitName = "Test unit"
    def unitId = "2e63d3f8-b326-4304-84e6-c12efbbcaaa4"
    def subUnitName = "Test subunit"
    def subUnitId = "fb329c3e-b87b-44d2-a680-e2d12539f3f7"
    def clientName = "New Client"
    def clientId = "c6960c88-1c71-4e0f-b429-0746d362f12b"
    def domainName = "New Domain"
    def domainId = "202ef4bc-102b-4feb-bbec-1366bcbdac0f"
    def domainDescription = "This is a domain."

    def createUnit() {
        def unit = new UnitImpl(Key.uuidFrom(unitId), unitName, null)

        def subUnit = new UnitImpl(Key.uuidFrom(subUnitId), subUnitId, null)

        unit.setUnits([subUnit] as Set)
        subUnit.setParent(unit)
        return unit
    }


    def "Transform simple group to DTO and back"() {
        given: "A unit with a group"
        Unit unit = createUnit()

        AssetGroup assetGroup = new AssetGroup()
        assetGroup.setId(Key.newUuid())

        Asset asset = new AssetImpl(Key.newUuid(), "AssetGroupInstanceName", unit)
        assetGroup.setInstance(asset)

        when: "the group is transformed into a DTO"
        def dto = EntityLayerSupertypeGroupDto.from(assetGroup, DtoEntityToTargetContext.completeTransformationContext)

        then: "The DTO contains all required data"
        dto.name == assetGroup.name
        dto.owner.displayName == unitName

        when: "the DTO is tranformed into the entity"
        def context = DtoTargetToEntityContext.completeTransformationContext
        context.addEntity(unit)
        def group = DtoTargetToEntityTransformer.transformDto2EntityLayerSupertype(context, dto)

        then: "The Unit contains all required data"
        group.name == assetGroup.name
        group.modelType == ModelPackage.ELEMENT_ASSET+"_GROUP"
        group.owner == unit
    }

    def "Transform group with members to DTO and back"() {
        given: "Some groups with members"

        Asset asset = new AssetImpl(Key.newUuid(), "AssetName", null)
        Asset asset1 = new AssetImpl(Key.newUuid(), "AssetName1", null)
        Asset asset2 = new AssetImpl(Key.newUuid(), "AssetName2", null)

        Process process = new ProcessImpl(Key.newUuid(), "Process", null)


        AssetGroup assetGroup = new AssetGroup()
        assetGroup.setId(Key.newUuid())
        Asset assetInstance = new AssetImpl(Key.newUuid(), "AssetGroupInstanceName", null)
        assetGroup.setInstance(assetInstance)
        assetGroup.setMembers([asset1, asset2] as Set)

        ProcessGroup processGroup = new ProcessGroup()
        processGroup.setId(Key.newUuid())
        processGroup.setInstance(new ProcessImpl(Key.newUuid(), "ProcessGroupInstanceName", null))
        processGroup.setMembers([process] as Set)


        when: "the groups are transformed to DTOs"
        AssetGroupDto ag = EntityLayerSupertypeGroupDto.from(assetGroup, DtoEntityToTargetContext.completeTransformationContext)
        ProcessGroupDto pg =EntityLayerSupertypeGroupDto.from(processGroup, DtoEntityToTargetContext.completeTransformationContext)
        then: "the two assets and the group are also transformed with members"
        ag.members.size()==2
        ag.members*.displayName as Set == [asset1.name, asset2.name] as Set

        pg.members.size() == 1
        pg.members.first().displayName == process.name


        when: "the DTOs are transformed back into entities"
        def context = DtoTargetToEntityContext.completeTransformationContext
        context.addEntity(asset)
        context.addEntity(asset1)
        context.addEntity(asset2)
        context.addEntity(process)
        AssetGroup eag = DtoTargetToEntityTransformer.transformDto2EntityLayerSupertype(context, ag)
        ProcessGroup epg = DtoTargetToEntityTransformer.transformDto2EntityLayerSupertype(context, pg)

        then: "the two assets and the group are also transformed with members"

        eag.members.size()==2
        eag.members*.name as Set == [asset1.name, asset2.name] as Set

        epg.members.size() == 1
        epg.members.first().name == process.name
    }

    def "Transform circular structure to DTO and back"() {
        given: "some groups"

        DocumentGroup documentGroup1 = new DocumentGroup()
        documentGroup1.setId(Key.newUuid())
        documentGroup1.setName("DocumentGroup1")

        DocumentGroup documentGroup2 = new DocumentGroup()
        documentGroup2.setId(Key.newUuid())
        documentGroup2.setName("DocumentGroup2")

        DocumentGroup documentGroup3 = new DocumentGroup()
        documentGroup3.setId(Key.newUuid())
        documentGroup3.setName("DocumentGroup3")

        when: "the groups buils a chircle"
        documentGroup1.setMembers([documentGroup2] as Set)
        documentGroup2.setMembers([documentGroup3] as Set)
        documentGroup3.setMembers([documentGroup1] as Set)

        and: "the groups are transformed"
        DocumentGroupDto dtoDG1 = EntityLayerSupertypeGroupDto.from(documentGroup1, DtoEntityToTargetContext.completeTransformationContext)
        DocumentGroupDto dtoDG2 = EntityLayerSupertypeGroupDto.from(documentGroup2, DtoEntityToTargetContext.completeTransformationContext)
        DocumentGroupDto dtoDG3 = EntityLayerSupertypeGroupDto.from(documentGroup3, DtoEntityToTargetContext.completeTransformationContext)

        then: "all members are set"
        dtoDG1.members == [
            ModelObjectReference.from(documentGroup2)
        ] as Set
        dtoDG2.members == [
            ModelObjectReference.from(documentGroup3)
        ] as Set
        dtoDG3.members == [
            ModelObjectReference.from(documentGroup1)
        ] as Set


        when: "A transformation context is prepared"
        def transformationContext = DtoTargetToEntityContext.completeTransformationContext
        [
            documentGroup1,
            documentGroup2,
            documentGroup3
        ].forEach(transformationContext.&addEntity)
        and: "The DTOs are tranformed back"
        DocumentGroup dG1 = DtoTargetToEntityTransformer.transformDto2EntityLayerSupertype(transformationContext, dtoDG1)
        DocumentGroup dG2 = DtoTargetToEntityTransformer.transformDto2EntityLayerSupertype(transformationContext, dtoDG2)
        DocumentGroup dG3 = DtoTargetToEntityTransformer.transformDto2EntityLayerSupertype(transformationContext, dtoDG3)

        then: "all members are set"
        dG1.members == [dG2] as Set
        dG2.members == [dG3] as Set
        dG3.members == [dG1] as Set
    }

    def "Transform group that contains itself"() {
        given: "A group that contains itself"

        AssetGroup assetGroup = new AssetGroup()
        assetGroup.setId(Key.newUuid())
        assetGroup.setName("AssetGroupInstanceName")


        assetGroup.setMembers([assetGroup] as Set)
        when: "the group is transformed"
        AssetGroupDto ag = EntityLayerSupertypeGroupDto.from(assetGroup, DtoEntityToTargetContext.completeTransformationContext)

        then: "The group contains it self as it is not forbidden"
        ag.members == [
            ModelObjectReference.from(assetGroup)
        ] as Set
    }
}
