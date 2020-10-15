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
import org.veo.adapter.presenter.api.dto.full.FullEntityLayerSupertypeGroupDto
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext
import org.veo.core.entity.Asset
import org.veo.core.entity.Document
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.entity.groups.AssetGroup
import org.veo.core.entity.groups.DocumentGroup
import org.veo.core.entity.groups.ProcessGroup
import org.veo.core.entity.transform.ClassKey
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

    def "Transform simple group to DTO and back"() {
        given: "A unit with a group"
        Unit unit = createUnit()

        AssetGroup assetGroup = Mock()
        ReferenceAssembler assembler = Mock()

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
        assetGroup.getCreatedAt() >> Instant.now()
        assetGroup.getUpdatedAt() >> Instant.now()



        when: "the group is transformed into a DTO"
        def context = EntityToDtoContext.getCompleteTransformationContext(assembler)
        def dto = FullEntityLayerSupertypeGroupDto.from(assetGroup, context)

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
        asset.createdAt >> Instant.now()
        asset.updatedAt >> Instant.now()

        Asset asset1 = Mock(Asset)
        asset1.id >> a1id
        asset1.domains >> []
        asset1.links >> []
        asset1.customAspects >> []
        asset1.clientName >> "AssetName1"
        asset1.modelInterface >> Asset.class
        asset1.createdAt >> Instant.now()
        asset1.updatedAt >> Instant.now()

        Asset asset2 = Mock(Asset)
        asset2.id >> a2id
        asset2.links >> []
        asset2.customAspects >> []
        asset2.domains >> []
        asset2.clientName >> "AssetName2"
        asset2.modelInterface >> Asset.class
        asset2.createdAt >> Instant.now()
        asset2.updatedAt >> Instant.now()

        def process = Mock(Process)
        process.id >> pid
        process.domains >> []
        process.links >> []
        process.customAspects >> []
        process.name >> "Process"
        process.modelInterface >> Process.class
        process.displayName >> "Process"
        process.createdAt >> Instant.now()
        process.updatedAt >> Instant.now()


        AssetGroup assetGroup = Mock(AssetGroup)
        assetGroup.id >> agid
        assetGroup.name >> "AssetGroup"
        assetGroup.domains >> []
        assetGroup.links >> []
        assetGroup.customAspects >> []
        assetGroup.members>>([asset1, asset2] as Set)
        assetGroup.modelInterface >> Asset.class
        assetGroup.createdAt >> Instant.now()
        assetGroup.updatedAt >> Instant.now()

        ProcessGroup processGroup = Mock()
        processGroup.id >> pgid
        processGroup.name >> "ProcessGroupInstanceName"
        processGroup.domains >> []
        processGroup.links >> []
        processGroup.customAspects >> []
        processGroup.members >>([process] as Set)
        processGroup.modelInterface >> Process.class
        processGroup.createdAt >> Instant.now()
        processGroup.updatedAt >> Instant.now()

        EntityFactory factory = Mock()
        factory.createAsset() >> asset
        factory.createAssetGroup() >> assetGroup
        factory.createProcessGroup() >> processGroup

        ReferenceAssembler assembler = Mock()

        when: "the groups are transformed to DTOs"
        def e2DtoContext = EntityToDtoContext.getCompleteTransformationContext(assembler)
        def ag = FullEntityLayerSupertypeGroupDto.from(assetGroup, e2DtoContext)
        def pg = FullEntityLayerSupertypeGroupDto.from(processGroup, e2DtoContext)
        then: "the two assets and the group are also transformed with members"
        ag.members.size()==2
        ag.members*.displayName as Set == [asset1.name, asset2.name] as Set

        pg.members.size() == 1
        pg.members.first().displayName == process.name



        when: "the DTOs are transformed back into entities"
        def context = Mock(DtoToEntityContext).tap {
            it.context >> new HashMap<>()
            it.context.put(new ClassKey<>(Asset, asset.id),  asset);
            it.context.put(new ClassKey<>(Asset, asset1.id),  asset1);
            it.context.put(new ClassKey<>(Asset, asset2.id),  asset2);
            it.context.put(new ClassKey<>(Process, process.id),  process);
            it.factory >> factory
        }

        AssetGroup eag = ag.toEntity(context)
        ProcessGroup epg = pg.toEntity(context)

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
        documentGroup1.createdAt >> Instant.now()
        documentGroup1.updatedAt >> Instant.now()

        DocumentGroup documentGroup2 = Mock()
        documentGroup2.id >> Key.newUuid()
        documentGroup2.name>>"DocumentGroup1"
        documentGroup2.domains >> []
        documentGroup2.links >> []
        documentGroup2.customAspects >> []
        documentGroup2.modelInterface >> Document.class
        documentGroup2.createdAt >> Instant.now()
        documentGroup2.updatedAt >> Instant.now()

        DocumentGroup documentGroup3 = Mock()
        documentGroup3.id >> Key.newUuid()
        documentGroup3.name>>"DocumentGroup1"
        documentGroup3.domains >> []
        documentGroup3.links >> []
        documentGroup3.customAspects >> []
        documentGroup3.modelInterface >> Document.class
        documentGroup3.createdAt >> Instant.now()
        documentGroup3.updatedAt >> Instant.now()

        documentGroup1.members >> ([documentGroup2] as Set)
        documentGroup3.members >> ([documentGroup1] as Set)

        documentGroup2.members >> ([documentGroup3] as Set)

        ReferenceAssembler assembler = Mock()

        when: "the groups are transformed"
        def context = EntityToDtoContext.getCompleteTransformationContext(assembler)
        def dtoDG1 = FullEntityLayerSupertypeGroupDto.from(documentGroup1, context)
        def dtoDG2 = FullEntityLayerSupertypeGroupDto.from(documentGroup2, context)
        def dtoDG3 = FullEntityLayerSupertypeGroupDto.from(documentGroup3, context)

        then: "all members are set"
        def refAssembler = Mock(ReferenceAssembler)
        dtoDG1.members == [
            ModelObjectReference.from(documentGroup2, refAssembler)
        ] as Set
        dtoDG2.members == [
            ModelObjectReference.from(documentGroup3, refAssembler)
        ] as Set
        dtoDG3.members == [
            ModelObjectReference.from(documentGroup1, refAssembler)
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

        def transformationContext = Mock(DtoToEntityContext) {
            it.context >> new HashMap<>()
            it.factory >> factory
        }
        [
            documentGroup1,
            documentGroup2,
            documentGroup3
        ].forEach({
            transformationContext.context.put(new ClassKey<>(Document, it.id), it)}
        )


        and: "The DTOs are transformed back"
        DocumentGroup dG1 = dtoDG1.toEntity(transformationContext)
        DocumentGroup dG2 = dtoDG2.toEntity(transformationContext)
        DocumentGroup dG3 = dtoDG3.toEntity(transformationContext)

        then: "all members are set"
        // TODO VEO-288 This test is missing some conditions here.
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
        assetGroup.createdAt >> Instant.now()
        assetGroup.updatedAt >> Instant.now()

        def refAssembler = Mock(ReferenceAssembler)

        when: "the group is transformed"
        def context = EntityToDtoContext.getCompleteTransformationContext(refAssembler)
        def ag = FullEntityLayerSupertypeGroupDto.from(assetGroup, context)

        then: "The group contains it self as it is not forbidden"
        ag.members == [
            ModelObjectReference.from(assetGroup, refAssembler)
        ] as Set
    }
}
