/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.TemplateItemAspects
import org.veo.core.entity.Unit
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.ImpactRef
import org.veo.core.entity.risk.ImpactValues
import org.veo.core.entity.risk.PotentialProbability
import org.veo.core.entity.risk.ProbabilityRef
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

/**
 * This provides a complete client domain catalog example.
 */
class CatalogSpec extends VeoMvcSpec {

    static final String RISK_DEF_ID = "id"
    @Autowired
    ClientRepositoryImpl clientRepository
    @Autowired
    UnitRepositoryImpl unitRepository
    @Autowired
    ControlRepositoryImpl controlRepository
    @Autowired
    DomainRepositoryImpl domainRepository
    @Autowired
    TransactionTemplate txTemplate
    @Autowired
    EntityDataFactory entityFactory

    Domain domain
    Domain domain1
    CatalogItem item1
    CatalogItem item2
    CatalogItem item3
    CatalogItem item4
    CatalogItem item5
    CatalogItem item6
    CatalogItem item7
    CatalogItem zz1
    CatalogItem zz2
    CatalogItem otherItem
    CatalogItem processImpactExample
    CatalogItem controlImpactExample
    CatalogItem controlImpactExample1
    CatalogItem controlImpactExample2
    CatalogItem scenarioProbabilityExample
    CatalogItem scenarioProbabilityExample1
    CatalogItem scenarioProbabilityExample2
    CatalogItem itemComposite
    CatalogItem itemPart
    CatalogItem itemScope
    CatalogItem itemMember
    Client client
    Client secondClient
    Domain domain3
    Unit unit
    Unit unitSecondClient
    Control controlSecondClient

    def setup() {
        txTemplate.execute {
            client = createTestClient()

            domain = newDomain (client) {
                description = "ISO/IEC"
                abbreviation = "ISO"
                name = "IT-Grundschutz"
                authority = 'ta'
                templateVersion = '1.0'
                domainTemplate = domainTemplate
                riskDefinitions = [(RISK_DEF_ID): createRiskDefinition(RISK_DEF_ID)]
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.CONTROL, it) {
                    subTypes = [
                        CTL_TOM: newSubTypeDefinition {
                            statuses = ["NEW", "NEW1"]
                        }
                    ]
                    links = [
                        link_to_zz1: newLinkDefinition(ElementType.CONTROL, "CTL_TOM") {
                            attributeDefinitions = [
                                control_comment: new TextAttributeDefinition(),
                                control_another_attribute: new TextAttributeDefinition(),
                            ]
                        },
                        link_to_zz2: newLinkDefinition(ElementType.CONTROL, "CTL_TOM") {
                            attributeDefinitions = [
                                control_comment: new TextAttributeDefinition(),
                                control_operatingStage: new TextAttributeDefinition(),
                            ]
                        },
                        control_relevantAppliedThreat: newLinkDefinition(ElementType.SCENARIO, "SCN_Scenario") {
                            attributeDefinitions = [
                                control_comment: new TextAttributeDefinition(),
                                control_operatingStage: new TextAttributeDefinition(),
                            ]
                        }
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.PROCESS, it) {
                    subTypes = [
                        normalProcess: newSubTypeDefinition {},
                        MY_SUBTYPE: newSubTypeDefinition {
                            statuses = ["NEW", "START"]
                        },
                    ]
                    links = [
                        link_to_item_1: newLinkDefinition(ElementType.CONTROL, "CTL_TOM"),
                        link_to_item_2: newLinkDefinition(ElementType.CONTROL, "CTL_TOM"),
                        externallinktest: newLinkDefinition(ElementType.CONTROL, "CTL_TOM"),
                    ]
                    customAspects = [
                        process_resilience: newCustomAspectDefinition {
                            attributeDefinitions = [
                                process_resilience_impact: new EnumAttributeDefinition(List.of("process_resilience_impact_low", "process_resilience_impact_high"))
                            ]
                        },
                        process_processingDetails: new CustomAspectDefinition().tap{
                            attributeDefinitions = [
                                process_processingDetails_comment: new TextAttributeDefinition(),
                                process_processingDetails_operatingStage: new EnumAttributeDefinition([
                                    "process_processingDetails_operatingStage_planning",
                                    "process_processingDetails_operatingStage_operation",
                                ])
                            ]
                        }
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.SCENARIO, it) {
                    subTypes = [
                        SCN_Scenario: newSubTypeDefinition {},
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.SCOPE, it) {
                    subTypes = [
                        kaleidoscope: newSubTypeDefinition {},
                    ]
                })
            }

            item1 = newCatalogItem(domain, {
                name = 'c1'
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW"
            })
            item2 = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW"
                name = 'c2'
            })
            item3 = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW"
                name = 'c3'
            })
            newTailoringReference(item3, item2, TailoringReferenceType.COPY)
            newTailoringReference(item3, item1, TailoringReferenceType.COPY_ALWAYS)

            item4 = newCatalogItem(domain, {
                elementType = ElementType.PROCESS
                subType = "normalProcess"
                status = "NEW"
                name = 'p1'
            })

            newLinkTailoringReference(item4, item1, TailoringReferenceType.LINK) {
                linkType = "link_to_item_1"
            }
            newLinkTailoringReference(item4, item2, TailoringReferenceType.LINK) {
                linkType = "link_to_item_2"
            }

            item5 = newCatalogItem(domain, {
                elementType = ElementType.PROCESS
                subType = "MY_SUBTYPE"
                status = "NEW"
                name = 'p2'
            })

            item6 = newCatalogItem(domain, {
                elementType = ElementType.PROCESS
                subType = "MY_SUBTYPE"
                status = "START"
                abbreviation = "caf"
                name = 'p3-all-features'
                description = "a process with subtype"
                customAspects = [
                    "process_resilience":  [
                        "process_resilience_impact": "process_resilience_impact_low"
                    ],
                    "process_processingDetails":
                    [
                        "process_processingDetails_comment": "my comment",
                        "process_processingDetails_operatingStage": "process_processingDetails_operatingStage_operation"
                    ]
                ]
            })

            newLinkTailoringReference(item6, item1, TailoringReferenceType.LINK) {
                linkType = 'link_to_item_1'
            }

            item7 = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW1"
                name = 'tom1'
                description = "a control with external tailorref"
            })

            newLinkTailoringReference(item7, item4, TailoringReferenceType.LINK_EXTERNAL) {
                linkType = 'externallinktest'
            }

            zz1 = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW"
                name = 'zz1'
                description = "a control linked in a circle"
            })
            zz2 = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW"
                name = 'zz2'
            })

            newLinkTailoringReference(zz1, zz2, TailoringReferenceType.LINK) {
                linkType = "link_to_zz2"
                attributes = [
                    "control_comment":"another comment",
                    "control_operatingStage":"Updated"
                ]
            }
            newLinkTailoringReference(zz2, zz1, TailoringReferenceType.LINK) {
                linkType = "link_to_zz1"
                attributes = [
                    "control_comment":"comment of the link to zz1",
                    "control_another_attribute":"test"
                ]
            }

            processImpactExample = newCatalogItem(domain, {
                elementType = ElementType.PROCESS
                subType = "normalProcess"
                status = "NEW"
                name = 'processImpactExample'
                description = "a process example entry"

                aspects = new TemplateItemAspects(
                        [
                            (RiskDefinitionRef.from(domain.riskDefinitions.get(RISK_DEF_ID))):
                            (new ImpactValues([
                                ( CategoryRef.from(domain.riskDefinitions.get(RISK_DEF_ID)
                                .getCategory("C").get())):
                                (
                                ImpactRef.from(domain.riskDefinitions.get(RISK_DEF_ID)
                                .getCategory("C").get().getLevel(2).get())
                                )
                            ])
                            )]
                        ,null, null)
            })

            controlImpactExample = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW"
                name = 'controlImpactExample'
                description = "a control example entry"
            })

            controlImpactExample1 = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW"
                name = 'controlImpactExample1'
                description = "a second control example entry"
            })

            controlImpactExample2 = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW"
                name = 'controlImpactExample2'
                description = "a second control example entry"
            })

            scenarioProbabilityExample = newCatalogItem(domain, {
                elementType = ElementType.SCENARIO
                subType = "SCN_Scenario"
                status = "NEW"
                name = 'scenarioProbabilityExample'
                description = "a scenario example entry"
                aspects = new TemplateItemAspects([:],[
                    (RiskDefinitionRef.from(domain.riskDefinitions.get(RISK_DEF_ID))):
                    (new PotentialProbability(
                    ProbabilityRef.from(domain.riskDefinitions.get(RISK_DEF_ID).getProbability().getLevel(3).get())))
                ], null)
            })

            scenarioProbabilityExample1 = newCatalogItem(domain, {
                elementType = ElementType.SCENARIO
                subType = "SCN_Scenario"
                status = "NEW"
                name = 'scenarioProbabilityExample1'
                description = "a scenario example entry"
            })

            scenarioProbabilityExample2 = newCatalogItem(domain, {
                elementType = ElementType.SCENARIO
                subType = "SCN_Scenario"
                status = "NEW"
                name = 'scenarioProbabilityExample2'
                description = "a scenario example entry"
            })

            newLinkTailoringReference(controlImpactExample, scenarioProbabilityExample, TailoringReferenceType.LINK) {
                linkType = "control_relevantAppliedThreat"
            }
            newLinkTailoringReference(scenarioProbabilityExample,controlImpactExample, TailoringReferenceType.LINK_EXTERNAL) {
                linkType = "control_relevantAppliedThreat"
            }

            newLinkTailoringReference(controlImpactExample2, scenarioProbabilityExample1, TailoringReferenceType.LINK) {
                linkType = "control_relevantAppliedThreat"
            }
            newLinkTailoringReference(scenarioProbabilityExample1,controlImpactExample2, TailoringReferenceType.LINK_EXTERNAL) {
                linkType = "control_relevantAppliedThreat"
            }

            newLinkTailoringReference(controlImpactExample2, scenarioProbabilityExample2, TailoringReferenceType.LINK) {
                linkType = "control_relevantAppliedThreat"
            }
            newLinkTailoringReference(scenarioProbabilityExample2,controlImpactExample2, TailoringReferenceType.LINK_EXTERNAL) {
                linkType = "control_relevantAppliedThreat"
            }

            newLinkTailoringReference(controlImpactExample1, scenarioProbabilityExample2, TailoringReferenceType.LINK) {
                linkType = "control_relevantAppliedThreat"
            }
            newLinkTailoringReference(scenarioProbabilityExample2,controlImpactExample1, TailoringReferenceType.LINK_EXTERNAL) {
                linkType = "control_relevantAppliedThreat"
            }

            itemComposite = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW"
                name = 'itemComposite'
            })

            itemPart = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW"
                name = 'itemPart'
            })

            newTailoringReference(itemComposite, itemPart, TailoringReferenceType.PART)
            newTailoringReference(itemPart, itemComposite, TailoringReferenceType.COMPOSITE)

            itemScope = newCatalogItem(domain) {
                elementType = ElementType.SCOPE
                subType = "kaleidoscope"
                status = "NEW"
                name = "itemScope"

                def riskRefRef = new RiskDefinitionRef(RISK_DEF_ID)
                aspects = new TemplateItemAspects(
                        [
                            (riskRefRef): new ImpactValues([
                                (new CategoryRef("C")): (new ImpactRef(2))
                            ]),
                        ], null, riskRefRef)
            }
            itemMember = newCatalogItem(domain) {
                elementType = ElementType.PROCESS
                subType = "normalProcess"
                status = "NEW"
                name = "itemMember"
            }

            newTailoringReference(itemMember, itemScope, TailoringReferenceType.SCOPE)
            newTailoringReference(itemScope, itemMember, TailoringReferenceType.MEMBER)

            domain1 = newDomain (client) {
                description = "ISO/IEC2"
                abbreviation = "ISO"
                name = "ISO"
                authority = 'ta'
                templateVersion = '1.0'
            }

            client = clientRepository.save(client)

            unit = newUnit(client) {
                it.name = "Test unit"
            }
            unit = unitDataRepository.save(unit)

            domain = client.domains.toList().get(0)
            domain1 = client.domains.toList().get(1)

            item1 = domain.catalogItems.find{it.name == "c1"}
            item2 = domain.catalogItems.find{it.name == "c2"}
            item3 = domain.catalogItems.find{it.name == "c3"}
            item4 = domain.catalogItems.find{it.name == "p1"}
            item5 = domain.catalogItems.find{it.name == "p2"}
            item6 = domain.catalogItems.find{it.name == "p3-all-features"}
            item7 = domain.catalogItems.find{it.name == "tom1"}
            zz1 = domain.catalogItems.find{it.name == "zz1"}
            zz2 = domain.catalogItems.find{it.name == "zz2"}
            itemMember = domain.catalogItems.find{it.name == "c1"}
            itemScope = domain.catalogItems.find{it.name == "c1"}
            processImpactExample = domain.catalogItems.find{it.name == "processImpactExample"}
            controlImpactExample = domain.catalogItems.find{it.name == "controlImpactExample"}
            controlImpactExample1 = domain.catalogItems.find{it.name == "controlImpactExample1"}
            controlImpactExample2 = domain.catalogItems.find{it.name == "controlImpactExample2"}
            scenarioProbabilityExample = domain.catalogItems.find{it.name == "scenarioProbabilityExample"}
            scenarioProbabilityExample1 = domain.catalogItems.find{it.name == "scenarioProbabilityExample1"}
            scenarioProbabilityExample2 = domain.catalogItems.find{it.name == "scenarioProbabilityExample2"}
            itemComposite = domain.catalogItems.find{it.name == "itemComposite"}
            itemPart = domain.catalogItems.find{it.name == "itemPart"}
            itemScope = domain.catalogItems.find{it.name == "itemScope"}
            itemMember = domain.catalogItems.find{it.name == "itemMember"}

            secondClient = newClient() {
                it.name = "the other"
            }
            domain3 = newDomain(secondClient) {
                abbreviation = "D1"
                name = "Domain 1"
                authority = 'ta'
                templateVersion = '1.0'
            }

            newCatalogItem(domain3, {
                elementType = ElementType.CONTROL
                subType = "CTL_TOM"
                status = "NEW"
                name = 'c15'
            })
            secondClient = clientRepository.save(secondClient)
            domain3 = secondClient.domains[0]

            unitSecondClient = newUnit(secondClient) {
                it.name = "Other client unit"
            }

            unitSecondClient = unitDataRepository.save(unitSecondClient)
            controlSecondClient = controlRepository.save(newControl(unitSecondClient))

            otherItem = domain3.catalogItems.first()
        }
    }
}
