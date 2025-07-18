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

import static java.util.UUID.randomUUID

import org.veo.core.entity.Asset
import org.veo.core.entity.AssetRisk
import org.veo.core.entity.CustomAspect
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Domain
import org.veo.core.entity.Element
import org.veo.core.entity.ElementType
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.ElementTypeDefinition
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.risk.CategorizedImpactValueProvider
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.ImpactValues
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.entity.riskdefinition.CategoryDefinition
import org.veo.core.entity.riskdefinition.RiskDefinition
import org.veo.core.entity.riskdefinition.RiskDefinitionChange
import org.veo.service.ElementMigrationService

import spock.lang.Specification

class ElementMigrationServiceSpec extends Specification{
    def elementMigrationService = new ElementMigrationService()
    def domain = Spy(Domain)

    def 'removes obsolete custom aspect'() {
        given:
        domain.getElementTypeDefinition(ElementType.ASSET) >> Spy(ElementTypeDefinition) {
            customAspects >> [
                typeA: Spy(CustomAspectDefinition) {
                    attributeDefinitions >> [
                        attrA: new TextAttributeDefinition()
                    ]
                }
            ]
        }
        def element = Spy(Element) {
            it.idAsString >> randomUUID()
            type >> ElementType.ASSET
            customAspects >> ([
                Spy(CustomAspect) {
                    type >> "typeA"
                    attributes >> [
                        "attrA": "valueA"
                    ]
                    it.domain >> domain
                },
                Spy(CustomAspect) {
                    type >> "typeB"
                    attributes >> [
                        "attrA": "valueA"
                    ]
                    it.domain >> domain
                }
            ] as Set)
            links >> ([] as Set)
            findSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.customAspects*.type.sort() == ["typeA"]
    }

    def 'removes obsolete custom aspect attribute'() {
        given:
        domain.getElementTypeDefinition(ElementType.ASSET) >> Spy(ElementTypeDefinition) {
            customAspects >> [
                typeA: Spy(CustomAspectDefinition) {
                    attributeDefinitions >> [
                        attrA: new TextAttributeDefinition()
                    ]
                }
            ]
        }
        def element = Spy(Element) {
            it.idAsString >> randomUUID()
            type >> ElementType.ASSET
            customAspects >> ([
                Spy(CustomAspect) {
                    type >> "typeA"
                    attributes >> [
                        attrA: "valueA",
                        attrB: "valueB"
                    ]
                    it.domain >> domain
                }
            ] as Set)
            links >> ([] as Set)
            findSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.customAspects[0].attributes.keySet().sort() == ["attrA"]
    }

    def 'removes obsolete link'() {
        given:
        domain.getElementTypeDefinition(ElementType.ASSET) >> Spy(ElementTypeDefinition) {
            links >> [
                typeA: Mock(LinkDefinition) {
                    attributeDefinitions >> [:]
                    targetType >> ElementType.PERSON
                    targetSubType >> "PER_Person"
                }
            ]
        }
        def targetPerson = Mock(Element) {
            type >> ElementType.PERSON
            findSubType(domain) >> Optional.of("PER_Person")
        }
        def element = Spy(Element) {
            it.idAsString >> randomUUID()
            type >> ElementType.ASSET
            customAspects >> ([] as Set)
            links >> ([
                Spy(CustomLink) {
                    type >> "typeA"
                    attributes >> [:]
                    target >> targetPerson
                    it.domain >> domain
                },
                Spy(CustomLink) {
                    type >> "typeB"
                    attributes >> [:]
                    target >> targetPerson
                    it.domain >> domain
                }
            ] as Set)
            findSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.links*.type.sort() == ["typeA"]
    }

    def 'removes obsolete link attribute'() {
        given:
        domain.getElementTypeDefinition(ElementType.ASSET) >>  Spy(ElementTypeDefinition) {
            links >> [
                typeA: Spy(LinkDefinition) {
                    attributeDefinitions >> [
                        attrA: new TextAttributeDefinition()
                    ]
                    targetType >> ElementType.PERSON
                    targetSubType >> "PER_Person"
                }
            ]
        }
        def targetPerson = Mock(Element) {
            type >> ElementType.PERSON
            findSubType(domain) >> Optional.of("PER_Person")
        }
        def element = Spy(Element) {
            it.idAsString >> randomUUID()
            type >> ElementType.ASSET
            customAspects >> ([] as Set)
            links >> ([
                Spy(CustomLink) {
                    type >> "typeA"
                    attributes >> [
                        attrA: "valueA",
                        attrB: "valueB",
                    ]
                    target >> targetPerson
                    it.domain >> domain
                }
            ] as Set)
            findSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.links[0].attributes.keySet().sort() == ["attrA"]
    }

    def 'removes invalid custom aspect attribute'() {
        given:
        domain.getElementTypeDefinition(ElementType.ASSET) >>  Spy(ElementTypeDefinition) {
            customAspects >> [
                typeA: Spy(CustomAspectDefinition) {
                    attributeDefinitions >> [
                        firstName: new TextAttributeDefinition(),
                        lastName: new TextAttributeDefinition()
                    ]
                }
            ]
        }
        def element = Spy(Element) {
            it.idAsString >> randomUUID()
            type >> ElementType.ASSET
            customAspects >> [
                Spy(CustomAspect) {
                    type >> "typeA"
                    attributes >> [
                        firstName: "Johnny",
                        lastName: 5,
                    ]
                    it.domain >> domain
                }
            ]
            links >> []
            findSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.customAspects[0].attributes.keySet() ==~ ["firstName"]
    }

    def 'removes links with wrong target type or sub type'() {
        given:
        domain.getElementTypeDefinition(ElementType.ASSET) >>  Spy(ElementTypeDefinition) {
            links >> [
                veryNicePersonLink: Spy(LinkDefinition) {
                    attributeDefinitions >> [:]
                    targetType >> ElementType.PERSON
                    targetSubType >> "PER_VeryNice"
                },
                verySmartPeronLink: Spy(LinkDefinition) {
                    attributeDefinitions >> [:]
                    targetType >> ElementType.PERSON
                    targetSubType >> "PER_VerySmart"
                },
                serverLink: Spy(LinkDefinition) {
                    attributeDefinitions >> [:]
                    targetType >> ElementType.ASSET
                    targetSubType >> "AST_Server"
                },
            ]
        }
        def veryNiceTargetPerson = Spy(Element) {
            type >> ElementType.PERSON
            findSubType(domain) >> Optional.of("PER_VeryNice")
        }
        def element = Spy(Element) {
            it.idAsString >> randomUUID()
            type >> ElementType.ASSET
            customAspects >> ([] as Set)
            links >> ([
                Spy(CustomLink) {
                    type >> "veryNicePersonLink"
                    attributes >> [:]
                    target >> veryNiceTargetPerson
                    it.domain >> domain
                },
                Spy(CustomLink) {
                    type >> "verySmartPeronLink"
                    attributes >> [:]
                    target >> veryNiceTargetPerson
                    it.domain >> domain
                },
                Spy(CustomLink) {
                    type >> "serverLink"
                    attributes >> [:]
                    target >> veryNiceTargetPerson
                    it.domain >> domain
                }
            ] as Set)
            findSubType(domain) >> Optional.empty()
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        element.links*.type.sort() == ["veryNicePersonLink"]
    }

    def 'removes element with obsolete sub type from domain'() {
        given:
        domain.getElementTypeDefinition(ElementType.ASSET) >>  Mock(ElementTypeDefinition) {
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
            id >> UUID.randomUUID()
            type >> ElementType.ASSET
            getCustomAspects(domain) >> ([] as Set)
            getLinks(domain) >> ([] as Set)
            findSubType(domain) >> Optional.of("AST_Building")
            getStatus(domain) >> "NEW"
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        1 * element.removeFromDomains(domain)
    }

    def 'removes obsolete status'() {
        given:
        domain.getElementTypeDefinition(ElementType.ASSET) >>  Mock(ElementTypeDefinition) {
            customAspects >> []
            links >> []
            subTypes >> [
                AST_Server: Mock(SubTypeDefinition) {
                    statuses >> ["NEW"]
                }
            ]
        }
        def element = Mock(Element) {
            it.idAsString >> randomUUID()
            type >> ElementType.ASSET
            getCustomAspects(domain) >> ([] as Set)
            getLinks(domain) >> ([] as Set)
            findSubType(domain) >> Optional.of("AST_Server")
            getStatus(domain) >> "OLD"
        }

        when:
        elementMigrationService.migrate(element, domain)

        then:
        1 * element.setStatus('NEW', domain)
    }

    def 'removes risk values from risks if risk values are not supported'() {
        given:
        def category = Mock(CategoryDefinition) {
            id >> 'C'
            riskValuesSupported >> false
        }

        def riskDefinition = Mock(RiskDefinition) {
            id >> 'rd'
            getCategories() >> [category]
            getCategory('C') >> Optional.of(category)
            getCategory(_) >> Optional.empty()
        }
        def riskDefinitionRef = RiskDefinitionRef.from(riskDefinition)
        riskDefinition.toRef() >> riskDefinitionRef

        domain.getElementTypeDefinition(ElementType.ASSET) >>  Mock(ElementTypeDefinition) {
            customAspects >> []
            links >> []
            subTypes >> [
                AST_Server: Mock(SubTypeDefinition) {
                    statuses >> ["NEW"]
                }
            ]
        }
        domain.getRiskDefinition(riskDefinition.id) >> Optional.of(riskDefinition)

        def risk = Mock(AssetRisk) {
            getImpactProvider(riskDefinitionRef, domain) >> Mock(CategorizedImpactValueProvider) {
                getAvailableCategories() >> [CategoryRef.from(category)]
            }
            getRiskDefinitions(domain) >> [riskDefinitionRef]
        }

        def element = Mock(Asset) {
            it.idAsString >> randomUUID()
            modelType >> "asset"
            getCustomAspects(domain) >> ([] as Set)
            getLinks(domain) >> ([] as Set)
            findSubType(domain) >> Optional.of("AST_Server")
            getStatus(domain) >> "NEW"
            getRiskDefinitions(domain) >> [riskDefinitionRef]
            getImpactValues(domain)>> [
                (riskDefinitionRef): Mock(ImpactValues) {
                    potentialImpacts() >> [:]
                    potentialImpactsCalculated() >> [:]
                    potentialImpactReasons() >> [:]
                    potentialImpactExplanations() >> [:]
                }
            ]
            risks >> [risk]
        }

        when:
        elementMigrationService.migrateRiskRelated(element, domain, riskDefinition, [
            new RiskDefinitionChange.RiskMatrixRemove(category)
        ] as Set)

        then:
        1 * risk.removeRiskCategory(riskDefinitionRef, CategoryRef.from(category), domain)
    }

    def 'removes risk values from risks if category is removed'() {
        given:
        def category = Mock(CategoryDefinition) {
            id >> 'C'
            riskValuesSupported >> true
        }

        def category2 = Mock(CategoryDefinition) {
            id >> 'X'
            riskValuesSupported >> true
        }

        def riskDefinition = Mock(RiskDefinition) {
            id >> 'rd'
            getCategories() >> [category]
            getCategory('C') >> Optional.of(category)
            getCategory(_) >> Optional.empty()
        }
        def riskDefinitionRef = RiskDefinitionRef.from(riskDefinition)
        riskDefinition.toRef() >> riskDefinitionRef

        domain.getElementTypeDefinition(ElementType.ASSET) >>  Mock(ElementTypeDefinition) {
            customAspects >> []
            links >> []
            subTypes >> [
                AST_Server: Mock(SubTypeDefinition) {
                    statuses >> ["NEW"]
                }
            ]
        }
        domain.getRiskDefinition(riskDefinition.id) >> Optional.of(riskDefinition)

        def risk = Mock(AssetRisk) {
            getImpactProvider(riskDefinitionRef, domain) >> Mock(CategorizedImpactValueProvider) {
                getAvailableCategories() >> [
                    CategoryRef.from(category),
                    CategoryRef.from(category2)
                ]
            }
            getRiskDefinitions(domain) >> [riskDefinitionRef]
        }

        def element = Mock(Asset) {
            it.idAsString >> randomUUID()
            modelType >> "asset"
            getCustomAspects(domain) >> ([] as Set)
            getLinks(domain) >> ([] as Set)
            findSubType(domain) >> Optional.of("AST_Server")
            getStatus(domain) >> "NEW"
            getRiskDefinitions(domain) >> [riskDefinitionRef]
            getImpactValues(domain)>> [
                (riskDefinitionRef): Mock(ImpactValues) {
                    potentialImpacts() >> [:]
                    potentialImpactsCalculated() >> [:]
                    potentialImpactReasons() >> [:]
                    potentialImpactExplanations() >> [:]
                }
            ]
            risks >> [risk]
        }

        when:
        elementMigrationService.migrateRiskRelated(element, domain, riskDefinition, [
            new RiskDefinitionChange.CategoryListRemove(category2)
        ] as Set)

        then:
        1 * risk.removeRiskCategory(riskDefinitionRef, CategoryRef.from(category2), domain)
        0 * risk.removeRiskCategory(riskDefinitionRef, CategoryRef.from(category), domain)
    }

    def 'adds risk category to risk aspect if it supports risks'() {
        given:
        def category = Mock(CategoryDefinition) {
            id >> 'C'
            riskValuesSupported >> true
        }

        def riskDefinition = Mock(RiskDefinition) {
            id >> 'rd'
            getCategories() >> [category]
            getCategory('C') >> Optional.of(category)
            getCategory(_) >> Optional.empty()
        }
        def riskDefinitionRef = RiskDefinitionRef.from(riskDefinition)
        riskDefinition.toRef() >> riskDefinitionRef

        domain.getElementTypeDefinition(ElementType.ASSET) >>  Mock(ElementTypeDefinition) {
            customAspects >> []
            links >> []
            subTypes >> [
                AST_Server: Mock(SubTypeDefinition) {
                    statuses >> ["NEW"]
                }
            ]
        }
        domain.getRiskDefinition(riskDefinition.id) >> Optional.of(riskDefinition)

        def risk = Mock(AssetRisk) {
            getRiskDefinitions(domain) >> [
                riskDefinitionRef
            ]
            getImpactProvider(riskDefinitionRef, domain) >> Mock(CategorizedImpactValueProvider) {
                getAvailableCategories() >> [CategoryRef.from(category)]
            }
        }

        def element = Mock(Asset) {
            it.idAsString >> randomUUID()
            modelType >> "asset"
            getCustomAspects(domain) >> ([] as Set)
            getLinks(domain) >> ([] as Set)
            findSubType(domain) >> Optional.of("AST_Server")
            getStatus(domain) >> "NEW"
            getRiskDefinitions(domain) >> [riskDefinitionRef]
            getImpactValues(domain)>> [
                (riskDefinitionRef): Mock(ImpactValues) {
                    potentialImpacts() >> [:]
                    potentialImpactsCalculated() >> [:]
                    potentialImpactReasons() >> [:]
                    potentialImpactExplanations() >> [:]
                }
            ]
            risks >> [risk]
        }

        when:
        elementMigrationService.migrateRiskRelated(element, domain, riskDefinition, [
            new RiskDefinitionChange.CategoryListAdd(category)
        ] as Set)

        then:
        1 * risk.addRiskCategory(riskDefinitionRef, CategoryRef.from(category), domain)
    }

    def 'does not add risk category to risk aspect if it does not support risks'() {
        given:
        def category = Mock(CategoryDefinition) {
            id >> 'C'
            riskValuesSupported >> false
        }

        def riskDefinition = Mock(RiskDefinition) {
            id >> 'rd'
            getCategories() >> [category]
            getCategory('C') >> Optional.of(category)
            getCategory(_) >> Optional.empty()
        }
        def riskDefinitionRef = RiskDefinitionRef.from(riskDefinition)
        riskDefinition.toRef() >> riskDefinitionRef

        domain.getElementTypeDefinition(ElementType.ASSET) >>  Mock(ElementTypeDefinition) {
            customAspects >> []
            links >> []
            subTypes >> [
                AST_Server: Mock(SubTypeDefinition) {
                    statuses >> ["NEW"]
                }
            ]
        }
        domain.getRiskDefinition(riskDefinition.id) >> Optional.of(riskDefinition)

        def risk = Mock(AssetRisk) {
            getRiskDefinitions(domain) >> [
                riskDefinitionRef
            ]
            getImpactProvider(riskDefinitionRef, domain) >> Mock(CategorizedImpactValueProvider) {
                getAvailableCategories() >> [CategoryRef.from(category)]
            }
        }

        def element = Mock(Asset) {
            it.idAsString >> randomUUID()
            modelType >> "asset"
            getCustomAspects(domain) >> ([] as Set)
            getLinks(domain) >> ([] as Set)
            findSubType(domain) >> Optional.of("AST_Server")
            getStatus(domain) >> "NEW"
            getRiskDefinitions(domain) >> [riskDefinitionRef]
            getImpactValues(domain)>> [
                (riskDefinitionRef): Mock(ImpactValues) {
                    potentialImpacts() >> [:]
                    potentialImpactsCalculated() >> [:]
                    potentialImpactReasons() >> [:]
                    potentialImpactExplanations() >> [:]
                }
            ]
            risks >> [risk]
        }

        when:
        elementMigrationService.migrateRiskRelated(element, domain, riskDefinition, [
            new RiskDefinitionChange.CategoryListAdd(category)
        ] as Set)

        then:
        0 * risk.addRiskCategory(_)
    }

    def 'does not add risk category if aspect for rd does not exist'() {
        given:
        def category = Mock(CategoryDefinition) {
            id >> 'C'
            riskValuesSupported >> true
        }

        def riskDefinition = Mock(RiskDefinition) {
            id >> 'rd'
            getCategories() >> [category]
        }
        def riskDefinitionRef = RiskDefinitionRef.from(riskDefinition)
        riskDefinition.toRef() >> riskDefinitionRef

        domain.getElementTypeDefinition("asset") >>  Mock(ElementTypeDefinition) {
            customAspects >> []
            links >> []
            subTypes >> [
                AST_Server: Mock(SubTypeDefinition) {
                    statuses >> ["NEW"]
                }
            ]
        }
        domain.getRiskDefinition(riskDefinition.id) >> Optional.of(riskDefinition)

        def risk = Mock(AssetRisk) {
            getRiskDefinitions(domain) >> []
            getImpactProvider(riskDefinitionRef, domain) >> Mock(CategorizedImpactValueProvider) {
                getAvailableCategories() >> [CategoryRef.from(category)]
            }
        }

        def element = Mock(Asset) {
            it.idAsString >> randomUUID()
            modelType >> "asset"
            getCustomAspects(domain) >> ([] as Set)
            getLinks(domain) >> ([] as Set)
            findSubType(domain) >> Optional.of("AST_Server")
            getStatus(domain) >> "NEW"
            getRiskDefinitions(domain) >> [riskDefinitionRef]
            getImpactValues(domain)>> [
                (riskDefinitionRef): Mock(ImpactValues) {
                    potentialImpacts() >> [:]
                    potentialImpactsCalculated() >> [:]
                    potentialImpactReasons() >> [:]
                    potentialImpactExplanations() >> [:]
                }
            ]
            risks >> [risk]
        }

        when:
        elementMigrationService.migrateRiskRelated(element, domain, riskDefinition,[
            new RiskDefinitionChange.CategoryListAdd(category)
        ] as Set)

        then:
        0 * risk.addRiskCategory(_)
    }
}
