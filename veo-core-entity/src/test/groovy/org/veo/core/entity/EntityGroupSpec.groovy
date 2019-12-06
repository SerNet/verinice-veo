/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
package org.veo.core.entity

import java.time.Instant

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.TypeChecked

import org.veo.core.entity.EntityLayerSupertype.Lifecycle
import org.veo.core.entity.Key
import org.veo.core.entity.asset.Asset
import org.veo.core.entity.group.EntityGroup
import org.veo.core.entity.process.Process
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.specification.EntitySpecification
import org.veo.core.entity.specification.InvalidUnitException
import org.veo.core.entity.specification.SameClientSpecification
import spock.lang.Specification

public class EntityGroupSpec extends Specification {

    Unit unit;

    def setup() {
        this.unit = Unit.newUnitBelongingToClient(Client.newClient("New Client"), "New Unit");
    }


    def "A group object can be created" () {
        given: "a timestamp"
        Instant beforeCreation = Instant.now();

        when: "a new group object is created"
        EntityGroup<Asset> group = EntityGroup.newGroup(unit, "New group");

        then: "the group is initialized as expected"
        group.getId() != null;
        group.getName().equals("New group");
        group.getState().equals(Lifecycle.CREATING);
        group.getVersion().equals(0L);
        group.getValidFrom()?.isAfter(beforeCreation);
        group.getValidFrom()?.isBefore(Instant.now());
        group.getValidUntil() == null;
    }

    def "A group must have a unit" () {
        when: "a group is created with no unit"
        EntityGroup group = EntityGroup.existingGroup(Key.newUuid(), null, "no group");

        then: "an exception is thrown"
        thrown InvalidUnitException;
    }

    def "A group can contain assets" () {
        given: "a set of two assets"
        Set assets = Set.of(
                Asset.newAsset(unit, "New asset 1"),
                Asset.newAsset(unit, "New asset 2")
                );

        when: "a group is reinstantiated with the assets:"
        EntityGroup<Asset> assetGroup = EntityGroup.existingGroup(Key.newUuid(), unit, "Asset-Group");
        assetGroup.setGroupMembers(assets);

        then: "the group contains the assets"
        assetGroup.getGroupMembers().size() == 2;
    }


    def "A group can contain processes" () {
        given: "a set of two processes"
        Set processes = Set.of(
                Process.newProcess(unit, "New process 1"),
                Process.newProcess(unit, "New process 2")
                );

        when: "a group is reinstantiated with the processes:"
        EntityGroup<Process> processGroup = EntityGroup.existingGroup(Key.newUuid(), unit, "Process-Group");
        processGroup.setGroupMembers(processes);

        then: "the group contains the processes"
        processGroup.getGroupMembers().size() == 2;
    }

    // Note: type checking would prevent this simple case at compile time:
    // @TypeChecked
    def "A group can only contain objects of the same type" () {
        given: "two objects of different types"
        Process process = Process.newProcess(unit, "Process");
        Asset asset = Asset.newAsset(unit, "Asset");

        when: "a group is created with objects of two different types"
        EntityGroup<Process> group = EntityGroup.newGroup(unit, "Group");
        group.addGroupMember(process);
        group.addGroupMember(asset);

        then: "an exception should be thrown - but is not because Java does not have type checking at runtime"
        group.getGroupMembers().size() == 2;
        // This is why we should enforce parameter usage for generic types with a PMD rule
        // Currently we don't do that.
        // Alternatively we would have to implement our own runtime checking of group member types,
        // i.e.: "this.groupMembers.iterator().next().getClass().isInstance(newMember)"

    }

    def "A group can contain subgroups of identical types" () {
        given: "two groups of identical types"
        EntityGroup<Asset> subgroup1 = EntityGroup.newGroup(unit, "Subgroup 1");
        EntityGroup<Asset> subgroup2 = EntityGroup.newGroup(unit, "Subgroup 2");

        when: "a group is created with those subgroups"
        EntityGroup<EntityGroup<Asset>> group = EntityGroup.newGroup(unit, "Group");
        group.addGroupMember(subgroup1);
        group.addGroupMember(subgroup2);

        then: "the group contains both subgroups"
        group.getGroupMembers().size() == 2;
    }

    def "A group can contain subgroups of different types" () {
        given: "two groups of identical types"
        EntityGroup<Asset> subgroup1 = EntityGroup.newGroup(unit, "Subgroup 1");
        EntityGroup<Process> subgroup2 = EntityGroup.newGroup(unit, "Subgroup 2");

        when: "a group is created with those subgroups"
        EntityGroup<EntityGroup<? extends EntityLayerSupertype>> group = EntityGroup.newGroup(unit, "Group");
        group.addGroupMember(subgroup1);
        group.addGroupMember(subgroup2);

        then: "the group contains both subgroups"
        group.getGroupMembers().size() == 2;
    }

    def "A group can contain a subgroup that contains itself" () {
        given: "a subgroup of identical types"
        EntityGroup<EntityGroup<? extends EntityLayerSupertype>> subgroup = EntityGroup.newGroup(unit, "Subgroup");

        when: "a new group is created with this subgroup"
        EntityGroup<EntityGroup<? extends EntityLayerSupertype>> group = EntityGroup.newGroup(unit, "Group");
        group.addGroupMember(subgroup);

        and: "the group itself is added to the subgroup"
        subgroup.addGroupMember(group);

        then: "the group contains the subgroup which contains itself"
        group.getGroupMembers().size() == 1;
        subgroup.getGroupMembers().size() ==1;
        group.getGroupMembers().iterator().next().equals(subgroup);
        subgroup.getGroupMembers().iterator().next().equals(group);

    }

    def "A group could contain itself" () {
        given: "a new empty group"
        EntityGroup<EntityGroup<? extends EntityLayerSupertype>> group = EntityGroup.newGroup(unit, "Group");

        when: "the group is added to itself as a subgroup"
        group.addGroupMember(group);

        then: "the group contains itself"
        group.getGroupMembers().size() == 1;
        group.getGroupMembers().iterator().next().equals(group);
    }

    def "A group cannot be created with elements from different clients" () {
        given: "a set of two processes from different clients"
        Unit unit2 = Unit.newUnitBelongingToClient(Client.newClient("New Client 2"), "New Unit 2");

        Set processes = Set.of(
                Process.newProcess(unit, "New process 1"),
                Process.newProcess(unit2, "New process 2")
                );

        when: "a group is reinstantiated with the processes:"
        EntityGroup<Process> processGroup = EntityGroup.existingGroup(Key.newUuid(), unit, "Process-Group");
        processGroup.setGroupMembers(processes);

        then: "an exception is thrown"
        thrown ClientBoundaryViolationException;
    }

    def "Checking the client of a group will check clients of all members" () {
        given: "a processes entity"
        def p1 = Process.newProcess(unit, "New process 1");

        and: "a special entity that records method calls"
        def mockEntity = Mock(EntityLayerSupertype);
        mockEntity.getUnit() >> this.unit;

        when: "a group is reinstantiated with one process"
        def processGroup1 = EntityGroup.existingGroup(Key.newUuid(), unit, "Process-Group");
        processGroup1.addGroupMember(p1);

        and: "a subgroup is reinstantiated with the special entity"
        def subgroup = EntityGroup.existingGroup(Key.newUuid(), unit, "Sub-Group");
        subgroup.addGroupMember(mockEntity);
        processGroup1.addGroupMember(subgroup);

        and: "a client check is performed on the top group"
        processGroup1.checkSameClient(p1);

        then: "the check is performed on the subgroup as well"
        1 * mockEntity.checkSameClient(_);
    }

    def "A group member from another client cannot be added" () {
        given: "a set of two processes from different clients"
        Unit unit2 = Unit.newUnitBelongingToClient(Client.newClient("New Client 2"), "New Unit 2");
        Process p1 = Process.newProcess(unit, "New process 1");
        Process p2 = Process.newProcess(unit2, "New process 2");

        when: "a new group is created"
        EntityGroup<Process> processGroup = EntityGroup.existingGroup(Key.newUuid(), unit, "Process-Group");

        and: "the two processes are added"
        processGroup.addGroupMember(p1);
        processGroup.addGroupMember(p2);

        then: "an exception is thrown"
        thrown ClientBoundaryViolationException;
    }

    def "A group can return its members" () {
        given: "a set of two processes"
        Process p1 = Process.newProcess(unit, "New process 1");
        Process p2 = Process.newProcess(unit, "New process 2");
        Set processes = Set.of( p1, p2);

        when: "a group is reinstantiated with the processes:"
        EntityGroup<Process> processGroup = EntityGroup.existingGroup(Key.newUuid(), unit, "Process-Group");
        processGroup.setGroupMembers(processes);

        then: "the group can be queried for its members"
        processGroup.getGroupMembers().size() == 2;
        processGroup.getGroupMembers().contains(p1);
        processGroup.getGroupMembers().contains(p2);
    }

    def "A group can return members that fulfill a specification" () {
        given: "a set of two processes"
        Process p1 = Process.newProcess(unit, "New process 1");
        Process p2 = Process.newProcess(unit, "New process 2");
        Set processes = Set.of( p1, p2);

        and: "a specification"
        SameClientSpecification spec1 = new SameClientSpecification(unit.getClient());
        SameClientSpecification spec2 = new SameClientSpecification(Client.newClient("Another client"));

        when: "a group is reinstantiated with the processes"
        EntityGroup<Process> processGroup = EntityGroup.existingGroup(Key.newUuid(), unit, "Process-Group");
        processGroup.setGroupMembers(processes);

        then: "the group can be queried using the specification"
        processGroup.findMembersFulfilling(spec1).size() == 2;
        processGroup.findMembersFulfilling(spec2).size() == 0;
    }

    /*
     * Any interface implemented by the layer-supertype can be called for individual
     * objects or for groups of objects. The group may implement different behaviour
     * than individual objects, i.e. involving its members in the operation.
     */
    def "A group can be used just like a single element of the same type" () {
        given: "a set of two processes"
        Process p1 = Process.newProcess(unit, "New process 1");
        Process p2 = Process.newProcess(unit, "New process 2");
        Set processes = Set.of(p1, p2);

        when: "a group is reinstantiated with the processes:"
        EntityGroup<Process> processGroup = EntityGroup.existingGroup(Key.newUuid(), unit, "Process-Group");
        processGroup.setGroupMembers(processes);

        and: "another object should be compared"
        Process p3 = Process.newProcess(unit, "New process 3");

        then: "the same method can be called on the group (branch node) or element (leaf node)"
        ((EntityLayerSupertype)processGroup).checkSameClient(p3);
        ((EntityLayerSupertype)p1).checkSameClient(p3);
    }

}
