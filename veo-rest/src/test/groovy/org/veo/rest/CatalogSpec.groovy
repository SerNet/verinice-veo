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
import org.veo.core.entity.Catalog
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.ControlRiskValues
import org.veo.core.entity.risk.ImpactRef
import org.veo.core.entity.risk.ImplementationStatusRef
import org.veo.core.entity.risk.PotentialProbabilityImpl
import org.veo.core.entity.risk.ProbabilityRef
import org.veo.core.entity.risk.ProcessImpactValues
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.persistence.access.CatalogRepositoryImpl
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
    CatalogRepositoryImpl catalogRepository
    @Autowired
    TransactionTemplate txTemplate
    @Autowired
    EntityDataFactory entityFactory

    Domain domain
    Domain domain1
    Catalog catalog
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
    Client client
    Client secondClient
    Domain domain3
    Catalog catalog1
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
            }
            catalog = newCatalog(domain) {
                name= 'a'
            }

            item1 = newCatalogItem(catalog, {
                newControl(it) {
                    name = 'c1'
                    associateWithDomain(domain, "CTL_TOM", "NEW")
                }
            })
            item2 = newCatalogItem(catalog, {
                newControl(it) {
                    name = 'c2'
                    associateWithDomain(domain, "CTL_TOM", "NEW")
                }
            })
            item3 = newCatalogItem(catalog, {
                newControl(it) {
                    name = 'c3'
                    associateWithDomain(domain, "CTL_TOM", "NEW")
                }
            })
            newTailoringReference(item3, TailoringReferenceType.COPY) {
                catalogItem = item2
            }
            newTailoringReference(item3, TailoringReferenceType.COPY_ALWAYS) {
                catalogItem = item1
            }

            item4 = newCatalogItem(catalog, {
                newProcess(it) {
                    name = 'p1'
                    description = "a process example entry"
                    associateWithDomain(domain, "normalProcess", "NEW")
                }
            })

            newLinkTailoringReference(item4, TailoringReferenceType.LINK) {
                catalogItem = item1
                linkType = "link_to_item_1"
            }
            newLinkTailoringReference(item4, TailoringReferenceType.LINK) {
                catalogItem = item2
                linkType = "link_to_item_2"
            }

            item5 = newCatalogItem(catalog, {
                newProcess(it) {
                    name = 'p2'
                    description = "a process with subtype"
                    associateWithDomain(domain, "MY_SUBTYPE", "NEW")
                }
            })

            item6 = newCatalogItem(catalog, {
                newProcess(it) {
                    abbreviation = "caf"
                    name = 'p3-all-features'
                    description = "a process with subtype"
                    associateWithDomain(domain, "MY_SUBTYPE", "START")
                    customAspects = [
                        newCustomAspect("process_resilience", domain) {
                            attributes = [
                                "process_resilience_impact":"process_resilience_impact_low"
                            ]
                        },
                        newCustomAspect("process_processingDetails", domain) {
                            attributes = [
                                "process_processingDetails_comment":"my comment",
                                "process_processingDetails_operatingStage":"process_processingDetails_operatingStage_operation"
                            ]
                        }
                    ]
                }
            })

            newLinkTailoringReference(item6, TailoringReferenceType.LINK) {
                catalogItem = item1
                linkType = 'link_to_item_1'
            }

            item7 = newCatalogItem(catalog, {
                newControl(it) {
                    name = 'tom1'
                    description = "a control with external tailorref"
                    associateWithDomain(domain, "CTL_TOM", "NEW1")
                    customAspects = [
                        newCustomAspect("process_resilience", domain) {
                            attributes = [
                                "process_resilience_impact":"process_resilience_impact_low"
                            ]
                        },
                        newCustomAspect("process_processingDetails", domain) {
                            attributes = [
                                "process_processingDetails_comment":"another comment",
                                "process_processingDetails_operatingStage":"process_processingDetails_operatingStage_operation"
                            ]
                        }
                    ]
                }
            })
            newLinkTailoringReference(item7, TailoringReferenceType.LINK_EXTERNAL) {
                catalogItem = item4
                linkType = 'externallinktest'
            }

            zz1 = newCatalogItem(catalog, {
                newControl(it) {
                    name = 'zz1'
                    description = "a control linked in a circle"
                    associateWithDomain(domain, "CTL_TOM", "NEW")
                }
            })
            zz2 = newCatalogItem(catalog, {
                newControl(it) {
                    name = 'zz2'
                    description = "a control linked in a circle"
                    associateWithDomain(domain, "CTL_TOM", "NEW")
                }
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

            processImpactExample = newCatalogItem(catalog, {
                newProcess(it) {
                    name = 'zzzp-impact'
                    description = "a process example entry"
                    associateWithDomain(domain, "PRO_DataProcessing", "NEW")
                    setImpactValues(domain, [
                        (riskDefinitionRef) : new ProcessImpactValues().tap{
                            potentialImpacts = [
                                (new CategoryRef("C")): new ImpactRef(2)
                            ]
                        }
                    ] as Map )
                }
            })

            controlImpactExample = newCatalogItem(catalog, {
                newControl(it) {
                    name = 'zzzzc-impact'
                    description = "a control example entry"
                    associateWithDomain(domain, "CTL_TOM", "NEW")
                    setRiskValues(domain, [
                        (riskDefinitionRef): new ControlRiskValues().tap {
                            implementationStatus = new ImplementationStatusRef(1)
                        }
                    ] as Map)
                }
            })

            scenarioProbabilityExample = newCatalogItem(catalog, {
                newScenario(it) {
                    name = 'zzzzszsimpact'
                    description = "a scenario example entry"
                    associateWithDomain(domain, "SCN_Scenario", "NEW")
                    setPotentialProbability(domain, [
                        (riskDefinitionRef): new PotentialProbabilityImpl().tap {
                            potentialProbability = new ProbabilityRef(3)
                        }
                    ] as Map)
                }
            })

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
            catalog = domain.catalogs.first()

            (item1, item2, item3, item4, item5, item6, item7, zz1, zz2, processImpactExample, controlImpactExample, scenarioProbabilityExample) = catalog.catalogItems.sort{it.element.name}

            secondClient = newClient() {
                it.name = "the other"
            }
            domain3 = newDomain(secondClient) {
                abbreviation = "D1"
                name = "Domain 1"
                authority = 'ta'
                templateVersion = '1.0'
            }

            catalog1 = newCatalog(domain3) {
                name = 'b'
                newCatalogItem(it, {
                    newControl(it) {
                        name = 'c15'
                        associateWithDomain(domain3, "CTL_TOM", "NEW")
                    }
                })
            }
            secondClient = clientRepository.save(secondClient)
            domain3 = secondClient.domains[0]

            unitSecondClient = newUnit(secondClient) {
                it.name = "Other client unit"
            }

            unitSecondClient = unitDataRepository.save(unitSecondClient)
            controlSecondClient = controlRepository.save(newControl(unitSecondClient))

            catalog1 = domain3.catalogs.first()
            otherItem = catalog1.catalogItems.first()
        }
    }
}
