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

import org.veo.core.entity.Asset
import org.veo.core.entity.Element
import org.veo.core.entity.EntityType
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.persistence.entity.jpa.CustomLinkData
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.persistence.entity.jpa.transformer.IdentifiableDataFactory

import spock.lang.Specification

class ElementLinkSpec extends Specification {
    private identifiableFactory = new IdentifiableDataFactory()
    private factory = new EntityDataFactory()

    def "links are handled correctly on #entityType.pluralTerm"() {
        given: "two domains with some link definitions"
        def domainA = factory.createDomain("", "", "").tap {
            getElementTypeDefinition(entityType.singularTerm).links = [
                someType: new LinkDefinition().tap {
                    targetType = "asset"
                    attributeDefinitions = [
                        attr: new TextAttributeDefinition()
                    ]
                },
                someOtherType: new LinkDefinition().tap {
                    targetType = "asset"
                    attributeDefinitions = [
                        attr: new TextAttributeDefinition()
                    ]
                }
            ]
        }
        def domainB = factory.createDomain("", "", "").tap {
            getElementTypeDefinition(entityType.singularTerm).links = [
                someType: new LinkDefinition().tap {
                    targetType = "asset"
                    attributeDefinitions = [
                        attr: new IntegerAttributeDefinition()
                    ]
                }
            ]
        }

        and: "an element associated with both domains"
        def element = identifiableFactory.create(entityType.type, null) as Element
        element.associateWithDomain(domainA, "STA", "NEW")
        element.associateWithDomain(domainA, "STB", "NEW")

        and: "two target elements"
        def targetElementA = identifiableFactory.create(Asset.class, null)
        def targetElementB = identifiableFactory.create(Asset.class, null)

        when: "applying a link in both domains independently"
        element.applyLink(new CustomLinkData().tap {
            domain = domainA
            type = "someType"
            target = targetElementA
            attributes = [
                "attr": "valA"
            ]
        })
        element.applyLink(new CustomLinkData().tap {
            domain = domainB
            type = "someType"
            target = targetElementB
            attributes = [
                "attr": 11
            ]
        })

        then: "both links can be retrieved"
        with(element.getLinks(domainA)) {
            size() == 1
            first().domain == domainA
            first().type == "someType"
            first().target == targetElementA
            first().attributes.attr == "valA"
        }
        with(element.getLinks(domainB)) {
            size() == 1
            first().domain == domainB
            first().type == "someType"
            first().target == targetElementB
            first().attributes.attr == 11
        }

        when: "updating one link in domain A"
        element.applyLink(new CustomLinkData().tap {
            domain = domainA
            type = "someType"
            target = targetElementA
            attributes = [
                "attr": "newValA"
            ]
        })

        then: "the change is observable in the target domain"
        with(element.getLinks(domainA)) {
            size() == 1
            first().attributes.attr == "newValA"
        }

        and: "absent in the other domain"
        with(element.getLinks(domainB)) {
            size() == 1
            first().attributes.attr == 11
        }

        when: "adding a link with another target in domain B"
        element.applyLink(new CustomLinkData().tap {
            domain = domainB
            type = "someType"
            target = targetElementA
            attributes = [
                "attr": 10
            ]
        })

        then: "the new link exists in the target domain"
        with(element.getLinks(domainB)) { links ->
            links.size() == 2
            links.find { it.target == targetElementA }.type == "someType"
            links.find { it.target == targetElementA }.attributes.attr == 10
            links.find { it.target == targetElementB }.type == "someType"
            links.find { it.target == targetElementB }.attributes.attr == 11
        }

        and: "not in the other domain"
        with(element.getLinks(domainA)) {
            size() == 1
            first().attributes.attr == "newValA"
        }

        when: "adding a link with another type in domain A"
        element.applyLink(new CustomLinkData().tap {
            domain = domainA
            type = "someOtherType"
            target = targetElementA
            attributes = [
                "attr": "someOtherValA"
            ]
        })

        then: "the new link exists in the target domain"
        with(element.getLinks(domainA)) { links ->
            links.size() == 2
            links.find { it.type == "someType" }.target == targetElementA
            links.find { it.type == "someType" }.attributes.attr == "newValA"
            links.find { it.type == "someOtherType" }.target == targetElementA
            links.find { it.type == "someOtherType" }.attributes.attr == "someOtherValA"
        }

        and: "not in the other domain"
        with(element.getLinks(domainB)) {links ->
            links.size() == 2
            links.find { it.target == targetElementA }.type == "someType"
            links.find { it.target == targetElementA }.attributes.attr == 10
            links.find { it.target == targetElementB }.type == "someType"
            links.find { it.target == targetElementB }.attributes.attr == 11
        }

        where:
        entityType << EntityType.ELEMENT_TYPES
    }
}
