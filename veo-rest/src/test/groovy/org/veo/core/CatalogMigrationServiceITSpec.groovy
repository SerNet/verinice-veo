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
package org.veo.core

import static org.veo.core.entity.EntityType.DOCUMENT

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Asset
import org.veo.core.entity.Document
import org.veo.core.entity.Domain
import org.veo.core.entity.Person
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.repository.CatalogItemRepository
import org.veo.core.repository.DomainRepository
import org.veo.service.TemplateItemMigrationService

class CatalogMigrationServiceITSpec extends VeoSpringSpec{
    @Autowired
    CatalogItemRepository catalogItemRepository

    @Autowired
    DomainRepository domainRepository

    @Autowired
    TemplateItemMigrationService catalogItemMigrationService

    Domain domain

    def setup() {
        def client = createTestClient()
        domain = domainRepository.save(newDomain(client) {domain ->
            applyElementTypeDefinition(newElementTypeDefinition("document", domain) {
                subTypes = [
                    Manual: newSubTypeDefinition {
                        statuses = ["NEW"]
                    }
                ]
                customAspects = [
                    file: new CustomAspectDefinition().tap {
                        attributeDefinitions = [
                            extension: new EnumAttributeDefinition(["pdf", "md", "txt"])
                        ]
                    }
                ]
                links = [
                    author: new LinkDefinition().tap {
                        attributeDefinitions = [
                            copyrightYear: new IntegerAttributeDefinition(),
                            placeOfAuthoring: new TextAttributeDefinition(),
                        ]
                        targetType = "person"
                        targetSubType = "author"
                    }
                ]
            })
            applyElementTypeDefinition(newElementTypeDefinition(Person.SINGULAR_TERM, domain) {
                subTypes = [
                    author : newSubTypeDefinition {
                        statuses = ["NEW"]
                    }
                ]
            })
            applyElementTypeDefinition(newElementTypeDefinition(Asset.SINGULAR_TERM, domain) {
                subTypes = [
                    asset : newSubTypeDefinition {
                        statuses = ["NEW"]
                    }
                ]
            })
        })
    }

    def "invalid custom aspect attribute is removed"() {
        given:
        newCatalogItem(domain) {
            elementType = Document.SINGULAR_TERM
            subType = "Manual"
            status = "NEW"
            name = "router manual"
            customAspects = [
                "file": [
                    "extension": "pdf",
                    "size": 5000
                ]
            ]
        }
        domainRepository.save(domain)

        when:
        executeInTransaction {
            catalogItemMigrationService.migrate(DOCUMENT, domain)
        }

        and:
        def catalogItems = executeInTransaction {
            domainRepository.getById(domain.id).catalogItems.tap{
                it.customAspects*.aspectDescription
            }
        }

        then:
        with(catalogItems.find{it.name == "router manual"}) {
            def attributes = customAspects.file
            attributes.extension == "pdf"
            attributes.size == null
        }
    }

    def "link reference with invalid target is removed"() {
        given:
        def manualAuthor = newCatalogItem(domain, {
            elementType = Person.SINGULAR_TERM
            name = "manual author"
            subType = "author"
            status = "NEW"
        })
        def randomAsset = newCatalogItem(domain, {
            elementType = Asset.SINGULAR_TERM
            name = "random asset"
            subType = "asset"
            status = "NEW"
        })
        newCatalogItem(domain, {
            elementType = Document.SINGULAR_TERM
            name = "router manual"
            subType = "Manual"
            status = "NEW"
            tailoringReferences = [
                newLinkTailoringReference(it, manualAuthor, TailoringReferenceType.LINK) {
                    it.linkType = "author"
                },
                newLinkTailoringReference(it, randomAsset, TailoringReferenceType.LINK) {
                    it.linkType = "author"
                },
            ]
        })
        domainRepository.save(domain)

        when:
        executeInTransaction {
            catalogItemMigrationService.migrate(DOCUMENT, domain)
        }

        and:
        def domain = executeInTransaction {
            domainRepository.getById(domain.id).tap{
                it.catalogItems*.tailoringReferences*.target
            }
        }

        then:
        with(domain.catalogItems.find{it.name == "router manual"}) {
            it.tailoringReferences*.target*.name == ["manual author"]
        }
    }

    def "invalid attribute on internal and external link references is removed"() {
        given: "a catalog with two links that have some invalid attributes"
        def routerManual = newCatalogItem(domain, {
            name = "router manual"
            elementType = Document.SINGULAR_TERM
            subType = "Manual"
            status = "NEW"
        })
        def manualAuthor = newCatalogItem(domain, {
            name = "manual author"
            elementType = Person.SINGULAR_TERM
            subType = "author"
            status = "NEW"
            tailoringReferences = [
                newLinkTailoringReference(it, routerManual, TailoringReferenceType.LINK_EXTERNAL) {
                    it.linkType = "author"
                    it.attributes.copyrightYear = 2019
                    it.attributes.placeOfAuthoring = 22
                }
            ]
        })
        newCatalogItem(domain,{
            name = "thermometer manual"
            elementType = Document.SINGULAR_TERM
            subType = "Manual"
            status = "NEW"
            tailoringReferences = [
                newLinkTailoringReference(it, manualAuthor, TailoringReferenceType.LINK) {
                    it.linkType = "author"
                    it.attributes.copyrightYear = "2019"
                    it.attributes.placeOfAuthoring = "She wrote it in her armchair"
                },
            ]
        })
        domainRepository.save(domain)

        when: "migrating"
        executeInTransaction {
            catalogItemMigrationService.migrate(DOCUMENT, domain)
        }

        and: "fetching the catalog"
        def domain = executeInTransaction {
            domainRepository.findById(domain.id).get().tap{
                it.catalogItems*.tailoringReferences*.target
            }
        }

        then: "only the valid attributes remain"
        with(domain.catalogItems.find{it.name == "manual author"}) {
            with(it.tailoringReferences.find{it.target.name == "router manual"}) {
                attributes.copyrightYear == 2019
                attributes.placeOfAuthoring == null
            }
        }
        with(domain.catalogItems.find{it.name == "thermometer manual"}) {
            with(it.tailoringReferences.find{it.target.name == "manual author"}) {
                attributes.copyrightYear == null
                attributes.placeOfAuthoring == "She wrote it in her armchair"
            }
        }
    }
}
