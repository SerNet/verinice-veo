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
import org.veo.core.entity.ElementType
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.specification.EntitySpecifications
import org.veo.core.entity.specification.InvalidUnitException
import org.veo.test.VeoSpec

import spock.lang.Ignore

class CompositeElementSpec extends VeoSpec {
    Client client
    Unit unit

    def setup() {
        this.client = newClient()
        this.unit = newUnit(client)
    }

    def "A composite element can be created"() {
        given: "a timestamp"
        Instant beforeCreation = Instant.now()

        when: "a new composite object is created"
        Asset composite = newAsset(unit) {name = "New composite"}

        then: "the composite is initialized as expected"
        composite.name == "New composite"
        composite.version == 0L
        composite.createdAt?.isAfter(beforeCreation)
        composite.createdAt?.isBefore(Instant.now())
    }

    // This used to be guaranteed by the model, now it can't be verified without using external validation.
    //  It can't be tested without relying on external classes.
    @Ignore("TODO VEO-427 prevent creation of elements with no unit")
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
            newProcess(unit) { id = UUID.randomUUID() },
            newProcess(unit2) {
                id = UUID.randomUUID()
            }
        ] as Set

        when: "a composite is reinstantiated with the processes:"
        def composite = newProcess(unit)
        composite.parts = processes

        then: "an exception is thrown"
        def ex = thrown ClientBoundaryViolationException
        ex.message == "The client boundary would be violated by the attempted operation on process ${processes[1].idAsString} by client: $client.id"
    }

    def "A composite part from another client cannot be added"() {
        given: "a set of two processes from different clients"
        Client client2 = newClient()
        Unit unit2 = newUnit(client2)
        def p1 = newProcess(unit) { id = UUID.randomUUID() }
        def p2 = newProcess(unit2) { id = UUID.randomUUID() }

        when: "a new composite is created"
        def processComposite = newProcess(unit)

        and: "the two processes are added"
        processComposite.parts = [p1, p2] as Set

        then: "an exception is thrown"
        def ex = thrown ClientBoundaryViolationException
        ex.message == "The client boundary would be violated by the attempted operation on process ${p2.idAsString} by client: $client.id"
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
     * Any functionality defined by {@link Entity} type can be called for individual objects or for composites of objects. The composite may implement different behaviour than individual objects, i.e. involving its parts in the operation.
     */
    def "A composite can be used just like a single element of the same type"() {
        given: "a set of two processes"
        def domain = newDomain(client) {
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.PROCESS, it) {
                subTypes = [
                    ST: newSubTypeDefinition {
                        statuses = ["NEW"]
                    }
                ]
                customAspects = [
                    type: newCustomAspectDefinition {
                        attributeDefinitions = [
                            val: new TextAttributeDefinition()
                        ]
                    },
                    type2: newCustomAspectDefinition {
                        attributeDefinitions = [
                            val: new TextAttributeDefinition()
                        ]
                    }
                ]
                links = [
                    goodLink: new LinkDefinition()
                ]
            })
        }
        def p1 = newProcess(unit) {
            associateWithDomain(domain, "ST", "NEW")
            applyCustomAspect(newCustomAspect("type", domain) {
                attributes.val = "val1"
            })
        }
        def p2 = newProcess(unit) {
            associateWithDomain(domain, "ST", "NEW")
        }

        p1.applyLink(newCustomLink(p2, "goodLink", domain))

        when: "a composite is reinstantiated with the processes:"
        def processComposite = newProcess(unit)
        processComposite.parts = [p1, p2] as Set

        and: "another object should be compared"
        def p3 = newProcess(unit) {
            associateWithDomain(domain, "ST", "NEW")
            applyCustomAspect(newCustomAspect("type2", domain) {
                attributes.val = "val3"
            })
            applyLink(newCustomLink(p2, "goodLink", domain))
        }

        then: "the same method can be called on the composite (branch node) or element (leaf node)"
        p1.links.first().target == p2
        p3.links.first().target == p2
        p1.customAspects.first().attributes.val == "val1"
        p3.customAspects.first().attributes.val == "val3"
    }
}
