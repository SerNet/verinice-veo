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
import org.veo.core.entity.ElementType
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.persistence.entity.jpa.CustomAspectData
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.persistence.entity.jpa.transformer.IdentifiableDataFactory

import spock.lang.Specification

class ElementCustomAspectSpec extends Specification {
    private identifiableFactory = new IdentifiableDataFactory()
    private factory = new EntityDataFactory()

    def "custom aspects are handled correctly on #elementType.pluralTerm"() {
        given: "two domains with some CA definitions"
        def domainA = factory.createDomain("", "", "").tap {
            getElementTypeDefinition(elementType).customAspects = [
                someType: new CustomAspectDefinition().tap {
                    attributeDefinitions = [
                        attr: new TextAttributeDefinition()
                    ]
                },
                identicalType: new CustomAspectDefinition().tap{
                    attributeDefinitions = [
                        attr: new BooleanAttributeDefinition()
                    ]
                }
            ]
        }
        def domainB = factory.createDomain("", "", "").tap {
            getElementTypeDefinition(elementType).customAspects = [
                someType: new CustomAspectDefinition().tap {
                    attributeDefinitions = [
                        attr: new IntegerAttributeDefinition()
                    ]
                },
                someOtherType: new CustomAspectDefinition().tap {
                    attributeDefinitions = [
                        attr: new IntegerAttributeDefinition()
                    ]
                },
                identicalType: new CustomAspectDefinition().tap{
                    attributeDefinitions = [
                        attr: new BooleanAttributeDefinition()
                    ]
                }
            ]
        }

        and: "an element associated with both domains"
        def element = identifiableFactory.create(elementType.type) as Element
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

        when: "adding an identically defined custom aspect in domain A"
        element.applyCustomAspect(new CustomAspectData().tap{
            domain = domainA
            type = "identicalType"
            attributes = [
                attr: true
            ]
        })

        then: "the new CA exists in both domains"
        with(element.getCustomAspects(domainA)) { cas ->
            cas.size() == 2
            cas.find { it.type == "identicalType" }.attributes.attr == true
        }
        with(element.getCustomAspects(domainB)) { cas ->
            cas.size() == 3
            cas.find { it.type == "identicalType" }.attributes.attr == true
        }

        when: "updating the identically defined custom aspect in domain B"
        element.applyCustomAspect(new CustomAspectData().tap{
            domain = domainB
            type = "identicalType"
            attributes = [
                attr: false
            ]
        })

        then: "the new CA has been updated in both domain"
        with(element.getCustomAspects(domainA)) { cas ->
            cas.find { it.type == "identicalType" }.attributes.attr == false
        }
        with(element.getCustomAspects(domainB)) { cas ->
            cas.find { it.type == "identicalType" }.attributes.attr == false
        }

        where:
        elementType << ElementType.values()
    }
}
