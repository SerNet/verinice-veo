/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import org.veo.core.entity.Element
import org.veo.core.entity.EntityType
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.persistence.entity.jpa.CustomAspectData
import org.veo.persistence.entity.jpa.CustomLinkData
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.persistence.entity.jpa.transformer.IdentifiableDataFactory

import spock.lang.Specification

class ElementCustomAspectSpec extends Specification {
    private identifiableFactory = new IdentifiableDataFactory()
    private factory = new EntityDataFactory()

    // TODO VEO-1763 test CA synchronization with identically defined CAs
    def "custom aspects are handled correctly on #entityType.pluralTerm"() {
        given: "two domains with some CA definitions"
        def domainA = factory.createDomain("", "", "").tap {
            getElementTypeDefinition(entityType.singularTerm).customAspects = [
                someType: new CustomAspectDefinition().tap {
                    attributeDefinitions = [
                        attr: new TextAttributeDefinition()
                    ]
                }
            ]
        }
        def domainB = factory.createDomain("", "", "").tap {
            getElementTypeDefinition(entityType.singularTerm).customAspects = [
                someType: new CustomAspectDefinition().tap {
                    attributeDefinitions = [
                        attr: new IntegerAttributeDefinition()
                    ]
                },
                someOtherType: new CustomAspectDefinition().tap {
                    attributeDefinitions = [
                        attr: new IntegerAttributeDefinition()
                    ]
                }
            ]
        }

        and: "an element associated with both domains"
        def element = identifiableFactory.create(entityType.type, null) as Element
        element.associateWithDomain(domainA, "STA", "NEW")
        element.associateWithDomain(domainB, "STB", "NEW")

        when: "applying a custom aspect in both domains independently"
        element.applyCustomAspect(new CustomAspectData().tap {
            domain = domainA
            type = "someType"
            attributes = [
                "attr": "valA"
            ]
        })
        element.applyCustomAspect(new CustomAspectData().tap {
            domain = domainB
            type = "someType"
            attributes = [
                "attr": 11
            ]
        })

        then: "they can be retrieved"
        with(element.getCustomAspects(domainA)) {
            size() == 1
            first().domain == domainA
            first().type == "someType"
            first().attributes.attr == "valA"
        }
        with(element.getCustomAspects(domainB)) {
            size() == 1
            first().domain == domainB
            first().type == "someType"
            first().attributes.attr == 11
        }

        when: "updating one custom aspect in domain A"
        element.applyCustomAspect(new CustomAspectData().tap {
            domain = domainA
            type = "someType"
            attributes = [
                "attr": "newValA"
            ]
        })

        then: "the change is observable in the target domain"
        with(element.getCustomAspects(domainA)) {
            size() == 1
            first().attributes.attr == "newValA"
        }

        and: "absent in the other domain"
        with(element.getCustomAspects(domainB)) {
            size() == 1
            first().attributes.attr == 11
        }

        when: "adding a custom aspect in domain B"
        element.applyCustomAspect(new CustomAspectData().tap {
            domain = domainB
            type = "someOtherType"
            attributes = [
                "attr": 12
            ]
        })

        then: "the new CA exists in the target domain"
        with(element.getCustomAspects(domainB)) { cas ->
            cas.size() == 2
            cas.find { it.type == "someType" }.attributes.attr == 11
            cas.find { it.type == "someOtherType" }.attributes.attr == 12
        }

        and: "not in the other domain"
        with(element.getCustomAspects(domainA)) {
            size() == 1
            first().attributes.attr == "newValA"
        }

        where:
        entityType << EntityType.ELEMENT_TYPES
    }

    def "cannot use custom links as custom aspects on #entityType.pluralTerm"() {
        given:
        def element = identifiableFactory.create(entityType.type, null) as Element

        when:
        element.applyCustomAspect(new CustomLinkData())

        then:
        def applyEx = thrown(IllegalArgumentException)
        applyEx.message == "Cannot apply custom aspect - got custom link"

        when:
        element.removeCustomAspect(new CustomLinkData())

        then:
        def removeEx = thrown(IllegalArgumentException)
        removeEx.message == "Cannot remove custom aspect - got custom link"

        where:
        entityType << EntityType.ELEMENT_TYPES
    }
}
