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
package org.veo.persistence.entity

import java.time.Instant

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.EntityLayerSupertype
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.entity.Versioned
import org.veo.core.entity.groups.AssetGroup
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.specification.SameClientSpecification
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.entity.jpa.groups.EntityGroupData
import org.veo.persistence.entity.jpa.groups.ScopeData
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.test.VeoSpec

import spock.lang.Ignore

public class EntityGroupSpec extends VeoSpec {
    EntityFactory entityFactory

    Client client

    Unit unit

    def setup() {
        entityFactory = new EntityDataFactory()
        this.client = entityFactory.createClient(Key.newUuid(), "client")
        this.unit = entityFactory.createUnit(Key.newUuid(), "unit", null)
        this.unit.setClient(client)
    }


    def "A group object can be created"() {
        given: "a timestamp"
        Instant beforeCreation = Instant.now()

        when: "a new group object is created"
        AssetGroup group = entityFactory.createAssetGroup(Key.newUuid(), "New group", this.unit)
        group.version("user", null)

        then: "the group is initialized as expected"
        group.getId() != null
        group.getName().equals("New group")
        group.getState().equals(Versioned.Lifecycle.CREATING)
        group.getVersion().equals(0L)
        group.getCreatedAt()?.isAfter(beforeCreation)
        group.getCreatedAt()?.isBefore(Instant.now())
        group.getValidUntil() == null
    }

    // This used to be guaranteed by the model, now it can't be verified without using external validation
    // of model objects. It can't be tested without relying on external classes.
    //    def "A group must have a unit" () {
    //        when: "a group is created with no unit"
    //        entityFactory.createAssetGroup(Key.newUuid(), "unit", null)
    //
    //        then: "an exception is thrown"
    //        // thrown InvalidUnitException;
    //
    //    }

    def "A group can contain assets"() {
        given: "a set of two assets"
        Set assets = Set.of(
                entityFactory.createAsset(Key.newUuid(), "asset 1", unit),
                entityFactory.createAsset(Key.newUuid(), "asset 2", unit),
                )

        when: "a group is reinstantiated with the assets:"
        def assetGroup = entityFactory.createAssetGroup(Key.newUuid(), "group1", unit)
        assetGroup.members = assets

        then: "the group contains the assets"
        assetGroup.members.size() == 2
    }


    def "A group can contain processes"() {
        given: "a set of two processes"
        def processes = [
            entityFactory.createProcess(Key.newUuid(), "process 1", unit),
            entityFactory.createProcess(Key.newUuid(), "process 2", unit)
        ] as Set

        when: "a group is reinstantiated with the processes:"
        def processGroup = entityFactory.createProcessGroup(Key.newUuid(), "processgroup", unit)
        processGroup.members = processes

        then: "the group contains the processes"
        processGroup.members.size() == 2
    }

    // Note: this could be prevented from working with @TypeChecked
    def "A group can only contain objects of the same type"() {
        given: "two objects of different types"
        def process = entityFactory.createProcess(Key.newUuid(), "process 1", unit)
        def asset = entityFactory.createAsset(Key.newUuid(), "asset 1", unit)
        Set newMembers = Set.of(process, asset)

        when: "a group is created with objects of two different types"
        def processGroup = entityFactory.createProcessGroup(Key.newUuid(), "processgroup", unit)
        processGroup.members = newMembers

        then: "an exception should be thrown - but is not because Java is not a fully statically typed language"
        processGroup.members.size() == 2
        // Alternatively we would have to implement our own runtime checking of group member types,
        // i.e.: "this.groupMembers.iterator().next().getClass().isInstance(newMember)"
    }

    def "A group can contain subgroups of identical types"() {
        given: "two groups of identical types"
        Process processGroup1 = entityFactory.createProcessGroup(Key.newUuid(), "processgroup 1", unit)
        Process processGroup2 = entityFactory.createProcessGroup(Key.newUuid(), "processgroup 2", unit)
        // These are required to create groups of objects with different types.

        when: "a group is created with those subgroups"
        def topProcessGroup = entityFactory.createProcessGroup(Key.newUuid(), "topprocessgroup", unit)
        topProcessGroup.members = [
            processGroup1,
            processGroup2] as Set<Process>

        then: "the group contains both subgroups"
        topProcessGroup.members.size() == 2
    }

    def "A group can contain subgroups of different types"() {
        given: "two groups of identical types"
        EntityGroupData<Asset> subgroup1 = newAssetGroup(unit) {
            name = "Subgroup 1"
        }

        EntityGroupData<Process> subgroup2 = newProcessGroup(unit) {
            name = "Subgroup 2"
        }

        when: "a group is created with those subgroups"
        EntityGroupData<EntityGroupData<? extends EntityLayerSupertype>> group = new ScopeData()


        group.addGroupMember(subgroup1)
        group.addGroupMember(subgroup2)

        then: "the group contains both subgroups"
        group.members.size() == 2
    }

    def "A group can contain a subgroup that contains itself"() {
        given: "a subgroup of identical types"
        def subGroup = entityFactory.createProcessGroup(Key.newUuid(), "subgroup", unit)

        when: "a new group is created with this subgroup"
        def topGroup = entityFactory.createProcessGroup(Key.newUuid(), "topgroup", unit)
        topGroup.members.add(subGroup)

        and: "the group itself is added to the subgroup"
        subGroup.members.add(topGroup)

        then: "the group contains the subgroup which contains itself"
        topGroup.members.size() == 1
        subGroup.members.size() == 1
        topGroup.members.iterator().next().equals(subGroup)
        subGroup.members.iterator().next().equals(topGroup)

    }

    def "A group could contain itself"() {
        given: "a new empty group"
        def group = entityFactory.createProcessGroup(Key.newUuid(), "subgroup", unit)

        when: "the group is added to itself as a subgroup"
        group.members.add(group)

        then: "the group contains itself"
        group.members.size() == 1
        group.members.first() == group
    }

    @Ignore("TODO VEO-384 restore guard clause when adding members from other clients to a group")
    def "A group cannot be created with elements from different clients"() {
        given: "a set of two processes from different clients"
        Client client2 = entityFactory.createClient(Key.newUuid(), "client 2")
        Unit unit2 = entityFactory.createUnit(Key.newUuid(), "unit 2", null)

        def processes = [
            entityFactory.createProcess(Key.newUuid(), "process 1", unit),
            entityFactory.createProcess(Key.newUuid(), "process 2", unit2)
        ] as Set

        when: "a group is reinstantiated with the processes:"
        def group = entityFactory.createProcessGroup(Key.newUuid(), "group", null)
        group.members = processes

        then: "an exception is thrown"
        thrown ClientBoundaryViolationException
    }

    @Ignore("TODO VEO-384 restore guard clause when adding members from other clients to a group")
    def "A group member from another client cannot be added"() {
        given: "a set of two processes from different clients"
        Client client2 = entityFactory.createClient(Key.newUuid(), "client 2")
        Unit unit2 = entityFactory.createUnit(Key.newUuid(), "unit 2", null)
        def p1 = entityFactory.createProcess(Key.newUuid(), "process 1", unit)
        def p2 = entityFactory.createProcess(Key.newUuid(), "process 2", unit2)

        when: "a new group is created"
        def processGroup = entityFactory.createProcessGroup(Key.newUuid(), "processgroup", unit)

        and: "the two processes are added"
        processGroup.members = [p1, p2] as Set

        then: "an exception is thrown"
        thrown ClientBoundaryViolationException
    }

    def "A group can return its members"() {
        given: "a set of two processes"
        def p1 = entityFactory.createProcess(Key.newUuid(), "process 1", unit)
        def p2 = entityFactory.createProcess(Key.newUuid(), "process 2", unit)

        when: "a group is reinstantiated with the processes:"
        def processGroup = entityFactory.createProcessGroup(Key.newUuid(), "processgroup", unit)
        processGroup.members = [p1, p2] as Set

        then: "the group can be queried for its members"
        processGroup.members.size() == 2
        processGroup.members.contains(p1)
        processGroup.members.contains(p2)
    }

    def "A group can return members that fulfill a specification"() {
        given: "a set of two processes"
        def p1 = entityFactory.createProcess(Key.newUuid(), "process 1", unit)
        def p2 = entityFactory.createProcess(Key.newUuid(), "process 2", unit)

        and: "a specification"
        def spec1 = new SameClientSpecification(client)
        def spec2 = new SameClientSpecification(entityFactory.createClient(Key.newUuid(), "client2"))

        when: "a group is reinstantiated with the processes"
        def processGroup = entityFactory.createProcessGroup(Key.newUuid(), "processgroup", unit)
        processGroup.members = [p1, p2] as Set

        then: "the group can be queried using the specification"
        // TODO VEO-385 restore filtering by specifications to groups
        //processGroup.findMembersFulfilling(spec1).size() == 2;
        //processGroup.findMembersFulfilling(spec2).size() == 0;

        // instead we have to do this:
        processGroup.members.stream()
                .filter({ spec1.isSatisfiedBy(it.getOwner().getClient()) })
                .toSet()
                .size() == 2
        processGroup.members.stream()
                .filter({ spec2.isSatisfiedBy(it.getOwner().getClient()) })
                .toSet()
                .size() == 0


        // Not working because of broken method "isSatisfiedBy(T entity)" in SameClientSpecification:
        //        spec1.selectSatisfyingElementsFrom(
        //                processGroup.members.stream().map{ p -> p.owner.client } as Set
        //            ).size() == 2
    }

    /*
     * Any interface implemented by the layer-supertype can be called for individual
     * objects or for groups of objects. The group may implement different behaviour
     * than individual objects, i.e. involving its members in the operation.
     */

    def "A group can be used just like a single element of the same type"() {
        given: "a set of two processes"
        def p1 = entityFactory.createProcess(Key.newUuid(), "process 1", unit)
        def p2 = entityFactory.createProcess(Key.newUuid(), "process 2", unit)

        def link = entityFactory.createCustomLink("aLink", p1, p2)
        p1.addToLinks(link)

        def customAspect = entityFactory.createCustomProperties()
        customAspect.type = "type"
        customAspect.applicableTo = [Process.getTypeName()] as Set
        p1.addToCustomAspects(customAspect)

        when: "a group is reinstantiated with the processes:"
        def processGroup = entityFactory.createProcessGroup(Key.newUuid(), "processgroup", unit)
        processGroup.members = [p1, p2] as Set

        and: "another object should be compared"
        def p3 = entityFactory.createProcess(Key.newUuid(), "process 3", unit)

        def customAspect2 = entityFactory.createCustomProperties()
        customAspect2.type = "type2"
        customAspect2.applicableTo = [Process.getTypeName()] as Set
        p3.addToCustomAspects(customAspect2)

        def link2 = entityFactory.createCustomLink("aLink2", p3, p2)
        p3.addToLinks(link2)

        then: "the same method can be called on the group (branch node) or element (leaf node)"
        p1.getLinks().first() == link
        p3.getLinks().first() == link2
        p1.getCustomAspects().first() == customAspect
        p3.getCustomAspects().first() == customAspect2
    }

}
