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
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.entity.Versioned
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.specification.SameClientSpecification
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.test.VeoSpec

import spock.lang.Ignore

class CompositeEntitySpec extends VeoSpec {
    EntityFactory entityFactory

    Client client

    Unit unit

    def setup() {
        entityFactory = new EntityDataFactory()
        this.client = entityFactory.createClient(Key.newUuid(), "client")
        this.unit = entityFactory.createUnit(Key.newUuid(), "unit", null)
        this.unit.client = client
    }


    def "A composite entity can be created"() {
        given: "a timestamp"
        Instant beforeCreation = Instant.now()

        when: "a new composite object is created"
        Asset composite = entityFactory.createAsset(Key.newUuid(), "New composite", this.unit)
        composite.version("user", null)

        then: "the composite is initialized as expected"
        composite.id != null
        composite.name == "New composite"
        composite.state == Versioned.Lifecycle.CREATING
        composite.version == 0L
        composite.createdAt?.isAfter(beforeCreation)
        composite.createdAt?.isBefore(Instant.now())
        composite.validUntil == null
    }

    // This used to be guaranteed by the model, now it can't be verified without using external validation
    // of model objects. It can't be tested without relying on external classes.
    @Ignore("TODO VEO-427 prevent creation of entities with no unit")
    def "A composite must have a unit" () {
        when: "a composite is created with no unit"
        entityFactory.createAssetComposite(Key.newUuid(), "unit", null)

        then: "an exception is thrown"
        thrown InvalidUnitException;
    }

    def "A composite can contain assets"() {
        given: "a set of two assets"
        Set assets = Set.of(
                entityFactory.createAsset(Key.newUuid(), "asset 1", unit),
                entityFactory.createAsset(Key.newUuid(), "asset 2", unit),
                )

        when: "a composite is reinstantiated with the assets:"
        def assetComposite = entityFactory.createAsset(Key.newUuid(), "composite1", unit)
        assetComposite.parts = assets

        then: "the composite contains the assets"
        assetComposite.parts.size() == 2
    }


    def "A composite can contain processes"() {
        given: "a set of two processes"
        def processes = [
            entityFactory.createProcess(Key.newUuid(), "process 1", unit),
            entityFactory.createProcess(Key.newUuid(), "process 2", unit)
        ] as Set

        when: "a composite is reinstantiated with the processes:"
        def processComposite = entityFactory.createProcess(Key.newUuid(), "processComposite", unit)
        processComposite.parts = processes

        then: "the composite contains the processes"
        processComposite.parts.size() == 2
    }

    // Note: this could be prevented from working with @TypeChecked
    def "A composite can only contain objects of the same type"() {
        given: "two objects of different types"
        def process = entityFactory.createProcess(Key.newUuid(), "process 1", unit)
        def asset = entityFactory.createAsset(Key.newUuid(), "asset 1", unit)
        Set newMembers = Set.of(process, asset)

        when: "a composite is created with objects of two different types"
        def processComposite = entityFactory.createProcess(Key.newUuid(), "processComposite", unit)
        processComposite.parts = newMembers

        then: "an exception should be thrown - but is not because Java is not a fully statically typed language"
        processComposite.parts.size() == 2
        // Alternatively we would have to implement our own runtime checking of composite member types,
        // i.e.: "this.compositeMembers.iterator().next().getClass().isInstance(newMember)"
    }

    def "A composite can contain sub-composites of identical types"() {
        given: "two composites of identical types"
        Process processComposite1 = entityFactory.createProcess(Key.newUuid(), "processcomposite 1", unit)
        processComposite1.addPart(entityFactory.createProcess(Key.newUuid(), "process1", unit))
        Process processComposite2 = entityFactory.createProcess(Key.newUuid(), "processcomposite 2", unit)
        processComposite2.addPart(entityFactory.createProcess(Key.newUuid(), "process2", unit))

        when: "a composite is created with those subcomposites"
        def topProcessComposite = entityFactory.createProcess(Key.newUuid(), "topprocesscomposite", unit)
        topProcessComposite.parts = [
            processComposite1,
            processComposite2] as Set<Process>

        then: "the composite contains both subcomposites"
        topProcessComposite.parts.size() == 2
    }

    def "A composite can contain a subcomposite that contains itself"() {
        given: "a subcomposite of identical types"
        def subComposite = entityFactory.createProcess(Key.newUuid(), "subcomposite", unit)

        when: "a new composite is created with this subcomposite"
        def topComposite = entityFactory.createProcess(Key.newUuid(), "topcomposite", unit)
        topComposite.addPart(subComposite)

        and: "the composite itself is added to the subcomposite"
        subComposite.addPart(topComposite)

        then: "the composite contains the subcomposite which contains itself"
        topComposite.parts.size() == 1
        subComposite.parts.size() == 1
        topComposite.parts.iterator().next() == subComposite
        subComposite.parts.iterator().next() == topComposite

    }

    def "A composite could contain itself"() {
        given: "a new empty composite"
        def composite = entityFactory.createProcess(Key.newUuid(), "subcomposite", unit)

        when: "the composite is added to itself as a subcomposite"
        composite.addPart(composite)

        then: "the composite contains itself"
        composite.parts.size() == 1
        composite.parts.first() == composite
    }

    @Ignore("TODO VEO-384 restore guard clause when adding parts from other clients to a composite")
    def "A composite cannot be created with elements from different clients"() {
        given: "a set of two processes from different clients"
        Client client2 = entityFactory.createClient(Key.newUuid(), "client 2")
        Unit unit2 = entityFactory.createUnit(Key.newUuid(), "unit 2", null)
        unit2.client = client2

        def processes = [
            entityFactory.createProcess(Key.newUuid(), "process 1", unit),
            entityFactory.createProcess(Key.newUuid(), "process 2", unit2)
        ] as Set

        when: "a composite is reinstantiated with the processes:"
        def composite = entityFactory.createProcess(Key.newUuid(), "composite", null)
        composite.parts = processes

        then: "an exception is thrown"
        thrown ClientBoundaryViolationException
    }

    @Ignore("TODO VEO-384 restore guard clause when adding parts from other clients to a composite")
    def "A composite part from another client cannot be added"() {
        given: "a set of two processes from different clients"
        Client client2 = entityFactory.createClient(Key.newUuid(), "client 2")
        Unit unit2 = entityFactory.createUnit(Key.newUuid(), "unit 2", null)
        def p1 = entityFactory.createProcess(Key.newUuid(), "process 1", unit)
        def p2 = entityFactory.createProcess(Key.newUuid(), "process 2", unit2)

        when: "a new composite is created"
        def processComposite = entityFactory.createProcess(Key.newUuid(), "processcomposite", unit)

        and: "the two processes are added"
        processComposite.parts = [p1, p2] as Set

        then: "an exception is thrown"
        thrown ClientBoundaryViolationException
    }

    def "A composite can return its parts"() {
        given: "a set of two processes"
        def p1 = entityFactory.createProcess(Key.newUuid(), "process 1", unit)
        def p2 = entityFactory.createProcess(Key.newUuid(), "process 2", unit)

        when: "a composite is reinstantiated with the processes:"
        def processComposite = entityFactory.createProcess(Key.newUuid(), "processcomposite", unit)
        processComposite.parts = [p1, p2] as Set

        then: "the composite can be queried for its parts"
        processComposite.parts.size() == 2
        processComposite.parts.contains(p1)
        processComposite.parts.contains(p2)
    }

    def "A composite can return parts that fulfill a specification"() {
        given: "a set of two processes"
        def p1 = entityFactory.createProcess(Key.newUuid(), "process 1", unit)
        def p2 = entityFactory.createProcess(Key.newUuid(), "process 2", unit)

        and: "a specification"
        def spec1 = new SameClientSpecification(client)
        def spec2 = new SameClientSpecification(entityFactory.createClient(Key.newUuid(), "client2"))

        when: "a composite is reinstantiated with the processes"
        def processComposite = entityFactory.createProcess(Key.newUuid(), "processcomposite", unit)
        processComposite.parts = [p1, p2] as Set

        then: "the composite can be queried using the specification"
        // TODO VEO-385 restore filtering by specifications to composites
        //processComposite.findMembersFulfilling(spec1).size() == 2;
        //processComposite.findMembersFulfilling(spec2).size() == 0;

        // instead we have to do this:
        processComposite.parts.stream()
                .filter({ spec1.isSatisfiedBy(it.owner.client) })
                .toSet()
                .size() == 2
        processComposite.parts.stream()
                .filter({ spec2.isSatisfiedBy(it.owner.client) })
                .toSet()
                .size() == 0

        // Not working because of broken method "isSatisfiedBy(T entity)" in SameClientSpecification:
        //        spec1.selectSatisfyingElementsFrom(
        //                processComposite.parts.stream().map{ p -> p.owner.client } as Set
        //            ).size() == 2
    }

    /*
     * Any interface implemented by the layer-supertype can be called for individual
     * objects or for composites of objects. The composite may implement different behaviour
     * than individual objects, i.e. involving its parts in the operation.
     */
    def "A composite can be used just like a single element of the same type"() {
        given: "a set of two processes"
        def p1 = entityFactory.createProcess(Key.newUuid(), "process 1", unit)
        def p2 = entityFactory.createProcess(Key.newUuid(), "process 2", unit)

        def link = entityFactory.createCustomLink("aLink", p1, p2)
        p1.addToLinks(link)

        def customAspect = entityFactory.createCustomProperties()
        customAspect.type = "type"
        customAspect.applicableTo = [Process.typeName] as Set
        p1.addToCustomAspects(customAspect)

        when: "a composite is reinstantiated with the processes:"
        def processComposite = entityFactory.createProcess(Key.newUuid(), "processcomposite", unit)
        processComposite.parts = [p1, p2] as Set

        and: "another object should be compared"
        def p3 = entityFactory.createProcess(Key.newUuid(), "process 3", unit)

        def customAspect2 = entityFactory.createCustomProperties()
        customAspect2.type = "type2"
        customAspect2.applicableTo = [Process.typeName] as Set
        p3.addToCustomAspects(customAspect2)

        def link2 = entityFactory.createCustomLink("aLink2", p3, p2)
        p3.addToLinks(link2)

        then: "the same method can be called on the composite (branch node) or element (leaf node)"
        p1.links.first() == link
        p3.links.first() == link2
        p1.customAspects.first() == customAspect
        p3.customAspects.first() == customAspect2
    }

}
