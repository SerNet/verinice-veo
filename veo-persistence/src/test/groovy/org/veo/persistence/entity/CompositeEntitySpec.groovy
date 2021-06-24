/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.persistence.entity

import java.time.Instant

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.entity.Versioned
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.specification.EntitySpecifications
import org.veo.core.entity.specification.InvalidUnitException
import org.veo.core.entity.specification.SameClientSpecification
import org.veo.test.VeoSpec

import spock.lang.Ignore

class CompositeEntitySpec extends VeoSpec {
    Client client
    Unit unit

    def setup() {
        this.client = newClient()
        this.unit = newUnit(client)
    }


    def "A composite entity can be created"() {
        given: "a timestamp"
        Instant beforeCreation = Instant.now()

        when: "a new composite object is created"
        Asset composite = newAsset(unit) {name = "New composite"}

        then: "the composite is initialized as expected"
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
        newAsset(unit)

        then: "an exception is thrown"
        thrown InvalidUnitException
    }

    def "A composite can contain assets"() {
        given: "a set of two assets"
        Set assets = Set.of(
                newAsset(unit),
                newAsset(unit),
                )

        when: "a composite is reinstantiated with the assets:"
        def assetComposite = newAsset(unit) {
            parts = assets
        }

        then: "the composite contains the assets"
        assetComposite.parts.size() == 2
    }


    def "A composite can contain processes"() {
        given: "a set of two processes"
        def processes = [
            newProcess(unit),
            newProcess(unit)
        ] as Set

        when: "a composite is reinstantiated with the processes:"
        def processComposite = newProcess(unit) {
            parts = processes
        }

        then: "the composite contains the processes"
        processComposite.parts.size() == 2
    }

    // Note: this could be prevented from working with @TypeChecked
    def "A composite can only contain objects of the same type"() {
        given: "two objects of different types"
        def process = newProcess(unit)
        def asset = newAsset(unit)
        Set newMembers = Set.of(process, asset)

        when: "a composite is created with objects of two different types"
        def processComposite = newProcess(unit) {
            parts = newMembers
        }

        then: "an exception should be thrown - but is not because Java is not a fully statically typed language"
        processComposite.parts.size() == 2
        // Alternatively we would have to implement our own runtime checking of composite member types,
        // i.e.: "this.compositeMembers.iterator().next().getClass().isInstance(newMember)"
    }

    def "A composite can contain sub-composites of identical types"() {
        given: "two composites of identical types"
        Process processComposite1 = newProcess(unit) {
            addPart(newProcess(unit))
        }
        Process processComposite2 = newProcess(unit) {
            addPart(newProcess(unit))
        }

        when: "a composite is created with those subcomposites"
        def topProcessComposite = newProcess(unit) {
            parts = [
                processComposite1,
                processComposite2
            ] as Set<Process>
        }

        then: "the composite contains both subcomposites"
        topProcessComposite.parts.size() == 2
    }

    def "A composite can contain a subcomposite that contains itself"() {
        given: "a subcomposite of identical types"
        def subComposite = newProcess(unit)

        when: "a new composite is created with this subcomposite"
        def topComposite = newProcess(unit)
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
        def composite = newProcess(unit)

        when: "the composite is added to itself as a subcomposite"
        composite.addPart(composite)

        then: "the composite contains itself"
        composite.parts.size() == 1
        composite.parts.first() == composite
    }

    def "A composite cannot be created with elements from different clients"() {
        given: "a set of two processes from different clients"
        Client client2 = newClient()
        Unit unit2 = newUnit(client2)

        def processes = [
            newProcess(unit),
            newProcess(unit2)
        ] as Set

        when: "a composite is reinstantiated with the processes:"
        def composite = newProcess(unit)
        composite.parts = processes

        then: "an exception is thrown"
        thrown ClientBoundaryViolationException
    }

    def "A composite part from another client cannot be added"() {
        given: "a set of two processes from different clients"
        Client client2 = newClient()
        Unit unit2 = newUnit(client2)
        def p1 = newProcess(unit)
        def p2 = newProcess(unit2)

        when: "a new composite is created"
        def processComposite = newProcess(unit)

        and: "the two processes are added"
        processComposite.parts = [p1, p2] as Set

        then: "an exception is thrown"
        thrown ClientBoundaryViolationException
    }

    def "A composite can return its parts"() {
        given: "a set of two processes"
        def p1 = newProcess(unit)
        def p2 = newProcess(unit)

        when: "a composite is reinstantiated with the processes:"
        def processComposite = newProcess(unit)
        processComposite.parts = [p1, p2] as Set

        then: "the composite can be queried for its parts"
        processComposite.parts.size() == 2
        processComposite.parts.contains(p1)
        processComposite.parts.contains(p2)
    }

    def "A composite can return parts that fulfill a specification"() {
        given: "a set of two processes"
        def p1 = newProcess(unit)
        def p2 = newProcess(unit)

        and: "a specification"
        def spec1 = EntitySpecifications.hasSameClient(client)
        def spec2 = EntitySpecifications.hasSameClient(newClient())

        when: "a composite is reinstantiated with the processes"
        def processComposite = newProcess(unit)
        processComposite.parts = [p1, p2] as Set

        then: "the composite can be queried using the specification"
        processComposite.findPartsFulfilling(spec1).size() == 2
        processComposite.findPartsFulfilling(spec2).size() == 0

        spec1.selectSatisfyingElementsFrom(processComposite.parts).size() == 2
    }

    /*
     * Any interface implemented by the layer-supertype can be called for individual
     * objects or for composites of objects. The composite may implement different behaviour
     * than individual objects, i.e. involving its parts in the operation.
     */
    def "A composite can be used just like a single element of the same type"() {
        given: "a set of two processes"
        def p1 = newProcess(unit)
        def p2 = newProcess(unit)

        def link = newCustomLink(p2, p1)
        p1.addToLinks(link)

        def customAspect = newCustomProperties("type")
        p1.addToCustomAspects(customAspect)

        when: "a composite is reinstantiated with the processes:"
        def processComposite = newProcess(unit)
        processComposite.parts = [p1, p2] as Set

        and: "another object should be compared"
        def p3 = newProcess(unit)

        def customAspect2 = newCustomProperties("type2")
        p3.addToCustomAspects(customAspect2)

        def link2 = newCustomLink(p2, p3)
        p3.addToLinks(link2)

        then: "the same method can be called on the composite (branch node) or element (leaf node)"
        p1.links.first() == link
        p3.links.first() == link2
        p1.customAspects.first() == customAspect
        p3.customAspects.first() == customAspect2
    }

}
