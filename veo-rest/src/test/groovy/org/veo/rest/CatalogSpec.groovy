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
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
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
    CatalogItem scenarioProbabilityExample
    CatalogItem itemComposite
    CatalogItem itemPart
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
                name = "ISO"
                authority = 'ta'
                templateVersion = '1.0'
                domainTemplate = domainTemplate
                riskDefinitions = [(RISK_DEF_ID): createRiskDefinition(RISK_DEF_ID)]
                applyElementTypeDefinition(newElementTypeDefinition("process", it) {
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
            }

            item1 = newCatalogItem(domain, {
                name = 'c1'
                elementType = "control"
                subType = "CTL_TOM"
                status = "NEW"
            })
            item2 = newCatalogItem(domain, {
                elementType = "control"
                subType = "CTL_TOM"
                status = "NEW"
                name = 'c2'
            })
            item3 = newCatalogItem(domain, {
                elementType = "control"
                subType = "CTL_TOM"
                status = "NEW"
                name = 'c3'
            })
            newTailoringReference(item3, TailoringReferenceType.COPY) {
                catalogItem = item2
            }
            newTailoringReference(item3, TailoringReferenceType.COPY_ALWAYS) {
                catalogItem = item1
            }

            item4 = newCatalogItem(domain, {
                elementType = "process"
                subType = "normalProcess"
                status = "NEW"
                name = 'p1'
            })

            newLinkTailoringReference(item4, TailoringReferenceType.LINK) {
                catalogItem = item1
                linkType = "link_to_item_1"
            }
            newLinkTailoringReference(item4, TailoringReferenceType.LINK) {
                catalogItem = item2
                linkType = "link_to_item_2"
            }

            item5 = newCatalogItem(domain, {
                elementType = "process"
                subType = "MY_SUBTYPE"
                status = "NEW"
                name = 'p2'
            })

            item6 = newCatalogItem(domain, {
                elementType = "process"
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

            newLinkTailoringReference(item6, TailoringReferenceType.LINK) {
                catalogItem = item1
                linkType = 'link_to_item_1'
            }

            item7 = newCatalogItem(domain, {
                elementType = "control"
                subType = "CTL_TOM"
                status = "NEW1"
                name = 'tom1'
                description = "a control with external tailorref"
            })

            newLinkTailoringReference(item7, TailoringReferenceType.LINK_EXTERNAL) {
                catalogItem = item4
                linkType = 'externallinktest'
            }

            zz1 = newCatalogItem(domain, {
                elementType = "control"
                subType = "CTL_TOM"
                status = "NEW"
                name = 'zz1'
                description = "a control linked in a circle"
            })
            zz2 = newCatalogItem(domain, {
                elementType = "control"
                subType = "CTL_TOM"
                status = "NEW"
                name = 'zz2'
            })

            newLinkTailoringReference(zz1, TailoringReferenceType.LINK) {
                catalogItem = zz2
                linkType = "link_to_zz2"
                attributes = [
                    "control_comment":"another comment",
                    "control_operatingStage":"Updated"
                ]
            }
            newLinkTailoringReference(zz2, TailoringReferenceType.LINK) {
                catalogItem = zz1
                linkType = "link_to_zz1"
                attributes = [
                    "control_comment":"comment of the link to zz1",
                    "control_another_attribute":"test"
                ]
            }

            def riskDefinitionRef = new RiskDefinitionRef(RISK_DEF_ID)

            processImpactExample = newCatalogItem(domain, {
                elementType = "process"
                subType = "PRO_DataProcessing"
                status = "NEW"
                name = 'zzzp-impact'
                description = "a process example entry"

                //TODO:veo-2285
                //                aspects = [
                //                    (org.veo.core.entity.CatalogItem.PROCESS_RISK_VALUES): [
                //                        (RISK_DEF_ID):  [
                //                            "C" : 2
                //                        ]
                //                    ]
                //                ]
            })

            controlImpactExample = newCatalogItem(domain, {
                elementType = "control"
                subType = "CTL_TOM"
                status = "NEW"
                name = 'zzzzc-impact'
                description = "a control example entry"
                //                aspects = [
                //                    (org.veo.core.entity.CatalogItem.CONTROL_RISK_VALUES):  [
                //                        (RISK_DEF_ID): [
                //                            "implementationStatus": 1
                //                        ]
                //                    ]
                //                ]
            })

            scenarioProbabilityExample = newCatalogItem(domain, {
                elementType = "scenario"
                subType = "SCN_Scenario"
                status = "NEW"
                name = 'zzzzszsimpact'
                description = "a scenario example entry"
                //                aspects = [
                //                    (org.veo.core.entity.CatalogItem.SCENARIO_RISK_VALUES): [
                //                        (RISK_DEF_ID): [
                //                            "potentialProbability": 3
                //                        ]]
                //                ]
            })

            itemComposite = newCatalogItem(domain, {
                elementType = "control"
                subType = "CTL_TOM"
                status = "NEW"
                name = 'zzzzzComposite'
            })

            itemPart = newCatalogItem(domain, {
                elementType = "control"
                subType = "CTL_TOM"
                status = "NEW"
                name = 'zzzzzPart'
            })

            newTailoringReference(itemComposite, TailoringReferenceType.PART) {
                catalogItem = itemPart
            }
            newTailoringReference(itemPart, TailoringReferenceType.COMPOSITE) {
                catalogItem = itemComposite
            }

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

            (item1, item2, item3, item4, item5, item6, item7, zz1, zz2, processImpactExample,
                    controlImpactExample, scenarioProbabilityExample, itemComposite, itemPart) = domain.catalogItems.sort{it.name}

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
                elementType = "control"
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
