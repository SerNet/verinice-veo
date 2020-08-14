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
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext
import org.veo.core.entity.*
import org.veo.core.entity.groups.AssetGroup
import org.veo.core.entity.groups.DocumentGroup
import org.veo.core.entity.groups.ProcessGroup
import org.veo.core.entity.transform.EntityFactory

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
    def assetId

    def createUnit() {
        Unit subUnit = Mock()

        subUnit.getClient() >> null
        subUnit.getDomains() >> []
        subUnit.getName() >> subUnitName
        subUnit.getId() >> Key.uuidFrom(subUnitId)
        subUnit.getUnits() >> []
        subUnit.getModelInterface() >> Unit.class


        Unit unit = Mock()
        unit.getClient() >> null
        unit.getDomains() >> []
        unit.getParent() >> null
        unit.getName() >> unitName
        unit.getId() >> Key.uuidFrom(unitId)
        unit.getUnits() >> [subUnit]
        unit.getModelInterface() >> Unit.class

        subUnit.getParent() >> unit
        return unit
    }

    def "Transform simple group to DTO and back"() {
        given: "A unit with a group"
        Unit unit = createUnit()

        AssetGroup assetGroup = Mock()

        //        assetGroup.setId(Key.newUuid())

        assetGroup.getOwner()>>unit
        assetGroup.getName()>>"AssetGroupInstanceName"
        assetGroup.getId()>>Key.newUuid()
        assetGroup.getDomains() >> []
        assetGroup.getLinks() >> []
        assetGroup.getLinks() >> []
        assetGroup.getCustomAspects() >> []
        assetGroup.getOwner() >> unit
        assetGroup.getModelInterface >> Asset.class
        assetGroup.getMembers() >> []



        when: "the group is transformed into a DTO"
        def dto = EntityLayerSupertypeGroupDto.from(assetGroup, EntityToDtoContext.completeTransformationContext)

        then: "The DTO contains all required data"
        dto.name == assetGroup.name
        dto.owner.displayName == unitName

    }

    def "Transform group with members to DTO and back"() {
        given: "Some groups with members"

        def aid= Key.newUuid()
        def a1id= Key.newUuid()
        def a2id= Key.newUuid()
        def pid= Key.newUuid()
        def agid= Key.newUuid()
        def pgid= Key.newUuid()


        Asset asset = Mock(Asset)
        asset.id >> aid
        asset.domains >> []
        asset.links >> []
        asset.customAspects >> []
        asset.domains >> []
        asset.clientName >> "AssetName"
        asset.modelInterface >> Asset.class

        Asset asset1 = Mock(Asset)
        asset1.id >> a1id
        asset1.domains >> []
        asset1.links >> []
        asset1.customAspects >> []
        asset1.clientName >> "AssetName1"
        asset1.modelInterface >> Asset.class

        Asset asset2 = Mock(Asset)
        asset2.id >> a2id
        asset2.links >> []
        asset2.customAspects >> []
        asset2.domains >> []
        asset2.clientName >> "AssetName2"
        asset2.modelInterface >> Asset.class

        Process process = Mock(Process)
        process.id >> pid
        process.domains >> []
        process.links >> []
        process.customAspects >> []
        process.name >> "Process"
        process.modelInterface >> Process.class


        AssetGroup assetGroup = Mock(AssetGroup)
        assetGroup.id >> agid
        assetGroup.name >> "AssetGroup"
        assetGroup.domains >> []
        assetGroup.links >> []
        assetGroup.customAspects >> []
        assetGroup.members>>([asset1, asset2] as Set)
        assetGroup.modelInterface >> Asset.class

        ProcessGroup processGroup = Mock()
        processGroup.id >> pgid
        processGroup.name >> "ProcessGroupInstanceName"
        processGroup.domains >> []
        processGroup.links >> []
        processGroup.customAspects >> []
        processGroup.members >>([process] as Set)
        processGroup.modelInterface >> Process.class

        EntityFactory factory = Mock()
        factory.createAsset() >> asset
        factory.createAssetGroup() >> assetGroup
        factory.createProcessGroup() >> processGroup

        when: "the groups are transformed to DTOs"
        AssetGroupDto ag = EntityLayerSupertypeGroupDto.from(assetGroup, EntityToDtoContext.completeTransformationContext)
        ProcessGroupDto pg =EntityLayerSupertypeGroupDto.from(processGroup, EntityToDtoContext.completeTransformationContext)
        then: "the two assets and the group are also transformed with members"
        ag.members.size()==2
        ag.members*.displayName as Set == [asset1.name, asset2.name] as Set

        pg.members.size() == 1
        pg.members.first().displayName == process.name



        when: "the DTOs are transformed back into entities"
        def context = new DtoToEntityContext(factory)
        context.addEntity(asset)
        context.addEntity(asset1)
        context.addEntity(asset2)
        context.addEntity(process)
        AssetGroup eag = DtoToEntityTransformer.transformDto2EntityLayerSupertype(context, ag)
        ProcessGroup epg = DtoToEntityTransformer.transformDto2EntityLayerSupertype(context, pg)

        then: "the two assets and the group are also transformed with members"

        eag.members.size()==2
        eag.members*.name as Set == [asset1.name, asset2.name] as Set

        epg.members.size() == 1
        epg.members.first().name == process.name
    }

    def "Transform circular structure to DTO and back"() {
        given: "some groups"

        DocumentGroup documentGroup1 = Mock()
        documentGroup1.id >> Key.newUuid()
        documentGroup1.name>>"DocumentGroup1"
        documentGroup1.domains >> []
        documentGroup1.links >> []
        documentGroup1.customAspects >> []
        documentGroup1.modelInterface >> Document.class

        DocumentGroup documentGroup2 = Mock()
        documentGroup2.id >> Key.newUuid()
        documentGroup2.name>>"DocumentGroup1"
        documentGroup2.domains >> []
        documentGroup2.links >> []
        documentGroup2.customAspects >> []
        documentGroup2.modelInterface >> Document.class


        DocumentGroup documentGroup3 = Mock()
        documentGroup3.id >> Key.newUuid()
        documentGroup3.name>>"DocumentGroup1"
        documentGroup3.domains >> []
        documentGroup3.links >> []
        documentGroup3.customAspects >> []
        documentGroup3.modelInterface >> Document.class

        documentGroup1.members >> ([documentGroup2] as Set)
        documentGroup3.members >> ([documentGroup1] as Set)

        documentGroup2.members >> ([documentGroup3] as Set)

        when: "the groups are transformed"
        DocumentGroupDto dtoDG1 = EntityLayerSupertypeGroupDto.from(documentGroup1, EntityToDtoContext.completeTransformationContext)
        DocumentGroupDto dtoDG2 = EntityLayerSupertypeGroupDto.from(documentGroup2, EntityToDtoContext.completeTransformationContext)
        DocumentGroupDto dtoDG3 = EntityLayerSupertypeGroupDto.from(documentGroup3, EntityToDtoContext.completeTransformationContext)

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

        DocumentGroup gg = Mock()
        // pitty this does not work
        //        1 * gg.setMembers([documentGroup2] as Set)
        //        1 * gg.setMembers([documentGroup3] as Set)
        //        1 * gg.setMembers([documentGroup1] as Set)
        3 * gg.setMembers(_)

        EntityFactory factory = Mock()
        factory.createDocumentGroup() >> gg

        def transformationContext = new DtoToEntityContext(factory)
        [
            documentGroup1,
            documentGroup2,
            documentGroup3
        ].forEach(transformationContext.&addEntity)
        and: "The DTOs are tranformed back"
        DocumentGroup dG1 = DtoToEntityTransformer.transformDto2EntityLayerSupertype(transformationContext, dtoDG1)
        DocumentGroup dG2 = DtoToEntityTransformer.transformDto2EntityLayerSupertype(transformationContext, dtoDG2)
        DocumentGroup dG3 = DtoToEntityTransformer.transformDto2EntityLayerSupertype(transformationContext, dtoDG3)

        then: "all members are set"

    }

    def "Transform group that contains itself"() {
        given: "A group that contains itself"

        AssetGroup assetGroup = Mock()
        assetGroup.id >> (Key.newUuid())
        assetGroup.name >>"AssetGroupInstanceName"
        assetGroup.domains >> []
        assetGroup.links >> []
        assetGroup.customAspects >> []
        assetGroup.modelInterface >> Document.class

        assetGroup.members>> ([assetGroup] as Set)
        when: "the group is transformed"
        AssetGroupDto ag = EntityLayerSupertypeGroupDto.from(assetGroup, EntityToDtoContext.completeTransformationContext)

        then: "The group contains it self as it is not forbidden"
        ag.members == [
            ModelObjectReference.from(assetGroup)
        ] as Set
    }
}
