/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.service

import org.veo.core.entity.CustomAspect
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Domain
import org.veo.core.entity.Element
import org.veo.core.entity.Key
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.ElementTypeDefinition
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.service.ElementMigrationService

import spock.lang.Specification

class ElementMigrationServiceSpec extends Specification{
    def elementMigrationService = new ElementMigrationService()
    def domain = Spy(Domain)

    def 'removes obsolete custom aspect'() {
        given:
        domain.getElementTypeDefinition("asset") >> Spy(ElementTypeDefinition) {
            customAspects >> [
                typeA: Spy(CustomAspectDefinition) {
                    attributeDefinitions >> [
                        attrA: new TextAttributeDefinition()
                    ]
                }
            ]
        }
        def element = Spy(Element) {
            id >> Key.newUuid()
            modelType >> "asset"
            customAspects >> ([
                Spy(CustomAspect) {
                    type >> "typeA"
                    attributes >> [
                        "attrA": "valueA"
                    ]
                },
                Spy(CustomAspect) {
                    type >> "typeB"
                    attributes >> [
                        "attrA": "valueA"
                    ]
                }
            ] as Set)
            links >> ([] as Set)
            getSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.customAspects*.type.sort() == ["typeA"]
    }

    def 'removes obsolete custom aspect attribute'() {
        given:
        domain.getElementTypeDefinition("asset") >> Spy(ElementTypeDefinition) {
            customAspects >> [
                typeA: Spy(CustomAspectDefinition) {
                    attributeDefinitions >> [
                        attrA: new TextAttributeDefinition()
                    ]
                }
            ]
        }
        def element = Spy(Element) {
            id >> Key.newUuid()
            modelType >> "asset"
            customAspects >> ([
                Spy(CustomAspect) {
                    type >> "typeA"
                    attributes >> [
                        attrA: "valueA",
                        attrB: "valueB"
                    ]
                }
            ] as Set)
            links >> ([] as Set)
            getSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.customAspects[0].attributes.keySet().sort() == ["attrA"]
    }

    def 'removes obsolete link'() {
        given:
        domain.getElementTypeDefinition("asset") >> Spy(ElementTypeDefinition) {
            links >> [
                typeA: Mock(LinkDefinition) {
                    attributeDefinitions >> [:]
                    targetType >> "person"
                    targetSubType >> "PER_Person"
                }
            ]
        }
        def targetPerson = Mock(Element) {
            modelType >> "person"
            getSubType(domain) >> Optional.of("PER_Person")
        }
        def element = Spy(Element) {
            id >> Key.newUuid()
            modelType >> "asset"
            customAspects >> ([] as Set)
            links >> ([
                Spy(CustomLink) {
                    type >> "typeA"
                    attributes >> [:]
                    target >> targetPerson
                },
                Spy(CustomLink) {
                    type >> "typeB"
                    attributes >> [:]
                    target >> targetPerson
                }
            ] as Set)
            getSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.links*.type.sort() == ["typeA"]
    }

    def 'removes obsolete link attribute'() {
        given:
        domain.getElementTypeDefinition("asset") >>  Spy(ElementTypeDefinition) {
            links >> [
                typeA: Spy(LinkDefinition) {
                    attributeDefinitions >> [
                        attrA: new TextAttributeDefinition()
                    ]
                    targetType >> "person"
                    targetSubType >> "PER_Person"
                }
            ]
        }
        def targetPerson = Mock(Element) {
            modelType >> "person"
            getSubType(domain) >> Optional.of("PER_Person")
        }
        def element = Spy(Element) {
            id >> Key.newUuid()
            modelType >> "asset"
            customAspects >> ([] as Set)
            links >> ([
                Spy(CustomLink) {
                    type >> "typeA"
                    attributes >> [
                        attrA: "valueA",
                        attrB: "valueB",
                    ]
                    target >> targetPerson
                }
            ] as Set)
            getSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.links[0].attributes.keySet().sort() == ["attrA"]
    }

    def 'removes invalid custom aspect attribute'() {
        given:
        domain.getElementTypeDefinition("asset") >>  Spy(ElementTypeDefinition) {
            customAspects >> [
                typeA: Spy(CustomAspectDefinition) {
                    attributeDefinitions >> [
                        firstName: new TextAttributeDefinition(),
                        lastName: new TextAttributeDefinition()
                    ]
                }
            ]
        }
        def targetPerson = Mock(Element) {
            modelType >> "person"
            getSubType(domain) >> Optional.of("PER_Person")
        }
        def element = Spy(Element) {
            id >> Key.newUuid()
            modelType >> "asset"
            customAspects >> [
                Spy(CustomLink) {
                    type >> "typeA"
                    attributes >> [
                        firstName: "Johnny",
                        lastName: 5,
                    ]
                    target >> targetPerson
                }
            ]
            links >> []
            getSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.customAspects[0].attributes.keySet() ==~ ["firstName"]
    }

    def 'removes links with wrong target type or sub type'() {
        given:
        domain.getElementTypeDefinition("asset") >>  Spy(ElementTypeDefinition) {
            links >> [
                veryNicePersonLink: Spy(LinkDefinition) {
                    attributeDefinitions >> [:]
                    targetType >> "person"
                    targetSubType >> "PER_VeryNice"
                },
                verySmartPeronLink: Spy(LinkDefinition) {
                    attributeDefinitions >> [:]
                    targetType >> "person"
                    targetSubType >> "PER_VerySmart"
                },
                serverLink: Spy(LinkDefinition) {
                    attributeDefinitions >> [:]
                    targetType >> "asset"
                    targetSubType >> "AST_Server"
                },
            ]
        }
        def veryNiceTargetPerson = Spy(Element) {
            modelType >> "person"
            getSubType(domain) >> Optional.of("PER_VeryNice")
        }
        def element = Spy(Element) {
            id >> Key.newUuid()
            modelType >> "asset"
            customAspects >> ([] as Set)
            links >> ([
                Spy(CustomLink) {
                    type >> "veryNicePersonLink"
                    attributes >> [:]
                    target >> veryNiceTargetPerson
                },
                Spy(CustomLink) {
                    type >> "verySmartPeronLink"
                    attributes >> [:]
                    target >> veryNiceTargetPerson
                },
                Spy(CustomLink) {
                    type >> "serverLink"
                    attributes >> [:]
                    target >> veryNiceTargetPerson
                }
            ] as Set)
            getSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.links*.type.sort() == ["veryNicePersonLink"]
    }

    def 'removes obsolete sub type'() {
        given:
        domain.getElementTypeDefinition("asset") >>  Mock(ElementTypeDefinition) {
            customAspects >> []
            links >> []
            subTypes >> [
                AST_Server: Mock(SubTypeDefinition) {
                    statuses >> ["NEW"]
                },
                AST_Device: Mock(SubTypeDefinition) {
                    statuses >> ["NEW"]
                },
            ]
        }
        def element = Mock(Element) {
            id >> Key.newUuid()
            modelType >> "asset"
            customAspects >> ([] as Set)
            links >> ([] as Set)
            getSubType(domain) >> Optional.of("AST_Building")
            getStatus(domain) >> Optional.of("NEW")
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        1 * element.associateWithDomain(domain, null, null)
    }

    def 'removes obsolete status'() {
        given:
        domain.getElementTypeDefinition("asset") >>  Mock(ElementTypeDefinition) {
            customAspects >> []
            links >> []
            subTypes >> [
                AST_Server: Mock(SubTypeDefinition) {
                    statuses >> ["NEW"]
                }
            ]
        }
        def element = Mock(Element) {
            id >> Key.newUuid()
            modelType >> "asset"
            customAspects >> ([] as Set)
            links >> ([] as Set)
            getSubType(domain) >> Optional.of("AST_Server")
            getStatus(domain) >> Optional.of("OLD")
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        1 * element.associateWithDomain(domain, "AST_Server", 'NEW')
    }
}
