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
package org.veo.persistence.entity.jpa

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Catalog
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.jpa.CatalogDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.test.VeoSpec

class DomainJpaSpec extends AbstractJpaSpec {
    @Autowired
    DomainTemplateDataRepository domainTemplateRepository
    @Autowired
    DomainDataRepository repository
    @Autowired
    CatalogDataRepository catalogRepository
    @Autowired
    ClientDataRepository clientRepository
    @Autowired
    UnitDataRepository unitRepository
    @Autowired
    ControlDataRepository controlRepository
    EntityFactory factory

    Domain domain0
    DomainTemplate domainTemplate
    Client client

    def setup() {
        factory = new EntityDataFactory()
        client = newClient()
        client = clientRepository.save(client)
    }

    def 'domain is inserted'() {
        given: "the domain"
        domain0 = newDomain(client) {
            abbreviation = 'd-1'
            name = 'domain'
            description = 'a description'
            authority = 'ISO'
            templateVersion = '1.0'
            riskDefinitions = ["id":
                createRiskDefinition("id1")
            ]as Map
        }

        when: "saving"
        domain0 = repository.save(domain0)
        Domain d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"
        d.abbreviation == domain0.abbreviation
        d.name == domain0.name
        d.description == domain0.description
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.riskDefinitions == domain0.riskDefinitions
    }

    def 'domain with catalog is inserted'() {
        given: "the domain template"
        domain0 = newDomain(client) {
            name = 'domain'
            authority = 'ISO'
            templateVersion = '1.0'
        }

        when: "saving"
        domain0 = repository.save(domain0)
        Catalog c = newCatalog(domain0) {
            abbreviation = 'c-1'
            name = 'a catalog'
            description = 'catalog 1desc'
        }

        domain0 = repository.save(domain0)
        Domain d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"
        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.catalogs.size() == 1
        d.catalogs.first().name == c.name
        d.catalogs.first().abbreviation == c.abbreviation
        d.catalogs.first().description == c.description
    }

    def 'domain with catalog and catalog items'() {
        given: "the domain template and a catalog"
        domain0 = newDomain(client) {}
        Catalog catalog = newCatalog(domain0) {
            abbreviation = 'c-1'
            name = 'catalog'
            description = 'a catalog'

            newCatalogItem(it, {
                elementType = "control"
            })
            newCatalogItem(it, {
                elementType = "control"
            })
            newCatalogItem(it, {
                elementType = "control"
            })
        }

        when: "saving"
        domain0 = repository.save(domain0)
        Domain d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"
        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.catalogs.size() == 1
        d.catalogs.first().id != null
        d.catalogs.first().domainTemplate == domain0
        d.catalogs.first().name == catalog.name
        d.catalogs.first().abbreviation == catalog.abbreviation
        d.catalogs.first().description == catalog.description
        d.catalogs.first().catalogItems.size() == 3
    }

    def 'domain with catalog and catalog items and a domain template'() {
        given: "the domain template and a catalog"
        domainTemplate = newDomainTemplate()
        domainTemplateRepository.save(domainTemplate)

        domain0 = newDomain(client)
        domain0.domainTemplate = domainTemplate

        Catalog catalog = newCatalog(domain0) {
            name = 'a'
            newCatalogItem(it, {
                elementType = "control"
            })
            newCatalogItem(it, {
                elementType = "control"
            })
            newCatalogItem(it, {
                elementType = "control"
            })
        }

        domain0.addToCatalogs(catalog)

        when: "saving"
        domain0 = repository.save(domain0)

        Domain d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"
        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.catalogs.size() == 1
        d.catalogs.first().id != null
        d.catalogs.first().catalogItems.size() == 3
        d.domainTemplate == domainTemplate
    }

    def 'domain with catalog and catalog items and controls'() {
        given: "the domain template and a catalog"
        domain0 = newDomain(client)
        newCatalog(domain0) {
            name = 'a'

            newCatalogItem(it, {
                elementType = "control"
                name = 'c1'
            })
            def item2 = newCatalogItem(it, {
                elementType = "control"
                name = 'c2'
            })
            def item3 = newCatalogItem(it, {
                elementType = "control"
                name = 'c3'
            })
            newTailoringReference(item3, TailoringReferenceType.COPY) {
                catalogItem = item2
            }
        }

        when: "saving"
        domain0 = repository.save(domain0)

        Domain d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"
        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.catalogs.size() == 1
        d.catalogs.first().catalogItems.size() == 3
        with(d.catalogs.first().catalogItems.sort {it.name}) {
            size() == 3
            it[0].name == 'c1'
            it[1].name == 'c2'
            it[2].name == 'c3'
            it[2].tailoringReferences.size() == 1
            it[2].tailoringReferences[0].referenceType == TailoringReferenceType.COPY
        }
    }

    def 'domain with catalog with linked elements'() {
        given: "the domain template and a catalog"
        newDomain(client) {domain->
            applyElementTypeDefinition(newElementTypeDefinition(Control.SINGULAR_TERM, domain) {
                subTypes = [
                    ctl : newSubTypeDefinition {
                        statuses = ["NEW"]
                    }
                ]
            })
        }
        Unit unit = newUnit(client)
        unit = unitRepository.save(unit)
        client = clientRepository.save(client)
        domain0 = client.domains.first()

        Catalog catalog = newCatalog(domain0) {
            name = 'a'
        }

        CatalogItem item1 = newCatalogItem(catalog, {
            elementType = "control"
            name = 'c1'
            abbreviation = 'c1'
            description = 'control number one'
            subType = "ctl"
            status = "NEW"
        })
        CatalogItem item2 = newCatalogItem(catalog, {
            elementType = "control"
            name = 'c2'
            subType = "ctl"
            status = "NEW"
        })
        CatalogItem item3 = newCatalogItem(catalog, {
            elementType = "control"
            name = 'c3'
            subType = "ctl"
            status = "NEW"
        })
        newTailoringReference(item3, TailoringReferenceType.COPY) {
            catalogItem = item2
        }
        CatalogItem item4 = newCatalogItem(catalog, {
            elementType = "asset"
            name = 'd1'
            subType = "asset"
            status = "NEW"
        })
        CatalogItem item5 = newCatalogItem(catalog, {
            elementType = "process"
            name = 'p1'
            subType = "Test"
            status = "NEW"
        })
        newTailoringReference(item5, TailoringReferenceType.COPY) {
            catalogItem = item2
        }
        newLinkTailoringReference(item5, TailoringReferenceType.LINK) {
            catalogItem = item3
            linkType = 'linktest'
        }

        CatalogItem item6 = newCatalogItem(catalog, {
            elementType = "control"
            name = 'c-p'
            subType = "ctl"
            status = "NEW"
        })
        newLinkTailoringReference(item6, TailoringReferenceType.LINK_EXTERNAL) {
            catalogItem = item2
            linkType = 'externallinktest'
        }

        catalog.catalogItems = [
            item1,
            item2,
            item3,
            item4,
            item5,
            item6
        ] as Set

        catalog = catalogRepository.save(catalog)

        when: "saving"
        domain0 = repository.save(domain0)

        Domain d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"
        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.catalogs.size() == 1

        when:
        def loadedCatalog = d.catalogs.first()

        then:
        loadedCatalog.id == catalog.id
        loadedCatalog.catalogItems.size() == 6
        with(loadedCatalog.catalogItems.sort {
            it.name
        }) {
            it[0].name == 'c-p'
            it[0].tailoringReferences.size() == 1
            it[0].tailoringReferences[0].referenceType == TailoringReferenceType.LINK_EXTERNAL
            it[0].tailoringReferences[0].linkType == 'externallinktest'
            it[1].name == 'c1'
            it[1].abbreviation == item1.abbreviation
            it[1].description == item1.description
            it[2].name == 'c2'
            it[3].name == 'c3'
            it[3].tailoringReferences.size() == 1
            it[3].tailoringReferences[0].referenceType == TailoringReferenceType.COPY
            it[4].name == 'd1'
            //            it[4].parts.first().name == item4.parts.first().name
            //TODO: VEO-2269 handle parts as tailrinrref
            it[5].name == 'p1'
            it[5].tailoringReferences.size() == 2
        }

        when: "create elements linked to catalogItems"
        def firstItemFromCatalog = loadedCatalog.catalogItems.sort {
            it.name
        }.first()
        Control controlEntity = newControl(unit) {
            name = 'c1'
        }
        controlEntity.description = "a linked entity"
        controlEntity.appliedCatalogItems = [firstItemFromCatalog] as Set
        controlEntity = controlRepository.save(controlEntity)
        clientRepository.save(client)

        Control c = controlRepository.findById(controlEntity.dbId).get()

        then: "the link remains"
        c.appliedCatalogItems.size() == 1
        c.appliedCatalogItems.first() == firstItemFromCatalog
    }
}
