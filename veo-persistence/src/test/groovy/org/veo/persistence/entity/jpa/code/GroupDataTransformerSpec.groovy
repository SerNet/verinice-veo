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
package org.veo.persistence.entity.jpa.code

import org.veo.core.entity.*
import org.veo.core.entity.groups.AssetGroup
import org.veo.core.entity.groups.DocumentGroup
import org.veo.core.entity.groups.ProcessGroup
import org.veo.core.entity.impl.AssetImpl
import org.veo.core.entity.impl.ProcessImpl
import org.veo.core.entity.impl.UnitImpl
import org.veo.persistence.entity.jpa.UnitData
import org.veo.persistence.entity.jpa.groups.AssetGroupData
import org.veo.persistence.entity.jpa.groups.DocumentGroupData
import org.veo.persistence.entity.jpa.groups.ProcessGroupData
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext
import spock.lang.Specification

class GroupDataTransformerSpec extends Specification {
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

        def subUnit = new UnitImpl(Key.uuidFrom(subUnitId), subUnitName, null)

        unit.setUnits([subUnit] as Set)
        subUnit.setParent(unit)
        return unit
    }



    def "Transform group with members to data object and back"() {
        given:  "Some groups with members"
        Unit unit = createUnit()

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
        AssetGroupData ag = AssetGroupData.from(assetGroup)
        ProcessGroupData pg = ProcessGroupData.from(processGroup)

        then: "the two assets and the group are also transformed with members"

        ag.getMembers().size()==2
        ag.getMembers()*.name as Set == [asset1.name, asset2.name ] as Set

        pg.getMembers().size() == 1
        pg.getMembers().first().name == process.name

        when: "the DTOs are transformed back into entities"
        AssetGroup eag = ag.toAssetGroup()
        ProcessGroup epg = pg.toProcessGroup()

        then: "the two assets and the group are also transformed with members"


        eag.getMembers().size()==2
        eag.getMembers()*.name as Set == [asset1.name, asset2.name ] as Set

        epg.getMembers().size() == 1
        epg.getMembers().first().name == process.name
    }

    def "Transform circular structure to DTO and back"() {
        given: "some groups"
        Unit unit = createUnit()

        DocumentGroup documentGroup1 = new DocumentGroup()
        documentGroup1.setId(Key.newUuid())
        documentGroup1.setName("DocumentGroup1")

        DocumentGroup documentGroup2 = new DocumentGroup()
        documentGroup2.setId(Key.newUuid())
        documentGroup2.setName("DocumentGroup2")

        DocumentGroup documentGroup3 = new DocumentGroup()
        documentGroup3.setId(Key.newUuid())
        documentGroup3.setName("DocumentGroup3")

        when: "the groups buils a circle"
        documentGroup1.setMembers([documentGroup2] as Set)
        documentGroup2.setMembers([documentGroup3] as Set)
        documentGroup3.setMembers([documentGroup1] as Set)

        and: "the groups are transformed"
        DocumentGroupData dtoDG1 = DocumentGroupData.from(documentGroup1, DataEntityToTargetContext.completeTransformationContext)
        DocumentGroupData dtoDG2 = DocumentGroupData.from(documentGroup2, DataEntityToTargetContext.completeTransformationContext)
        DocumentGroupData dtoDG3 = DocumentGroupData.from(documentGroup3, DataEntityToTargetContext.completeTransformationContext)

        then: "all members are set"
        dtoDG1.members == [dtoDG2] as Set
        dtoDG2.members == [dtoDG3] as Set
        dtoDG3.members == [dtoDG1] as Set

        when: "The data objects are transformed back"
        DocumentGroup dG1 = dtoDG1.toDocumentGroup()
        DocumentGroup dG2 = dtoDG2.toDocumentGroup()
        DocumentGroup dG3 = dtoDG3.toDocumentGroup()

        then: "all members are set"
        dG1.members == [dG2] as Set
        dG2.members == [dG3] as Set
        dG3.members == [dG1] as Set
    }

    def "Transform group that contains itself"() {
        given: "A group that contains itself"
        Unit unit = createUnit()

        AssetGroup assetGroup = new AssetGroup()
        assetGroup.setId(Key.newUuid())
        assetGroup.setName("AssetGroupInstanceName")

        assetGroup.setMembers([assetGroup] as Set)
        when: "the group is transformed"
        def unitData = UnitData.from(unit)
        AssetGroupData ag = AssetGroupData.from(assetGroup)

        then: "The group contains it self as it is not forbidden"
        ag.members == [ag ] as Set
    }
}
