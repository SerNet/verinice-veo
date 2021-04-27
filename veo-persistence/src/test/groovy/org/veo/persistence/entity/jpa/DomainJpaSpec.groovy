/*******************************************************************************
 * Copyright (c) 2021 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.entity.jpa

import java.time.Instant

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Asset
import org.veo.core.entity.Catalog
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.Process
import org.veo.core.entity.TailoringReference
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.entity.Versioned
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.jpa.CatalogDataRepository
import org.veo.persistence.access.jpa.CatalogItemDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

class DomainJpaSpec extends AbstractJpaSpec {
    @Autowired
    DomainTemplateDataRepository domainTemplateRepository;
    @Autowired
    DomainDataRepository repository;
    @Autowired
    CatalogDataRepository catalogRepository;
    @Autowired
    CatalogItemDataRepository catalogItemRepository;
    @Autowired
    ClientDataRepository clientRepository
    @Autowired
    UnitDataRepository unitRepository
    @Autowired
    ControlDataRepository controlRepository
    EntityFactory factory;

    Domain domain0
    DomainTemplate domainTemplate
    Client client

    def setup() {
        factory = new EntityDataFactory()
        client = newClient();
        client = clientRepository.save(client);
    }

    def 'domain is inserted'() {
        given: "the domain"

        domain0 = newDomain(client) {
            abbreviation = 'd-1'
            name = 'domain'
            description = 'a description'
            authority = 'ISO'
            templateVersion = '1.0'
            revision = 'latest'
        }


        when: "saving"

        domain0 = repository.save(domain0)
        DomainTemplate d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"

        d.abbreviation == domain0.abbreviation
        d.name == domain0.name
        d.description == domain0.description
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
    }

    def 'domain with catalog is inserted'() {
        given: "the domain template"

        domain0 = newDomain(client) {
            name = 'domain'
            authority = 'ISO'
            templateVersion = '1.0'
            revision = 'latest'
        }

        when: "saving"

        domain0 = repository.save(domain0)
        Catalog c = newCatalog {
            abbreviation = 'c-1'
            name = 'a catalog'
            description = 'catalog 1desc'
        }

        domain0.addToCatalogs(c)
        domain0 = repository.save(domain0)
        Domain d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.catalogs.size() == 1
        d.catalogs.first().name == c.name
        d.catalogs.first().abbreviation == c.abbreviation
        d.catalogs.first().description == c.description
    }

    def 'domain with catalog and catalog items'() {
        given: "the domain template and a catalog"

        domain0 = newDomain(client) {}
        Catalog catalog = newCatalog {
            abbreviation = 'c-1'
            name = 'catalog'
            description = 'a catalog'
            domainTemplate = domain0
        }
        domain0.addToCatalogs(catalog)
        CatalogItem item1 = newCatalogItem(catalog) {}
        CatalogItem item2 = newCatalogItem(catalog) {}
        CatalogItem item3 = newCatalogItem(catalog) {}

        catalog.catalogItems = [item1, item2, item3]

        when: "saving"

        domain0 = repository.save(domain0)
        Domain d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.catalogs.size() == 1
        d.catalogs.first().id == catalog.id
        d.catalogs.first().domainTemplate == domain0
        d.catalogs.first().name == catalog.name
        d.catalogs.first().abbreviation == catalog.abbreviation
        d.catalogs.first().description == catalog.description
        d.catalogs.first().catalogItems.size() == 3
    }

    def 'domain with catalog and catalog items and a domain template'() {
        given: "the domain template and a catalog"

        domainTemplate = newDomainTemplate {}
        domainTemplateRepository.save(domainTemplate)


        domain0 = newDomain(client) {}
        domain0.domainTemplate = domainTemplate

        Catalog catalog = newCatalog {
            name = 'a'
            domainTemplate = domain0
        }
        domain0.addToCatalogs(catalog)
        CatalogItem item1 = newCatalogItem(catalog) {}
        CatalogItem item2 = newCatalogItem(catalog) {}
        CatalogItem item3 = newCatalogItem(catalog) {}

        catalog.catalogItems = [item1, item2, item3]

        when: "saving"

        domain0 = repository.save(domain0)

        Domain d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.catalogs.size() == 1
        d.catalogs.first().id == catalog.id
        d.catalogs.first().catalogItems.size() == 3
        d.domainTemplate == domainTemplate
    }

    def 'domain with catalog and catalog items and controls'() {
        given: "the domain template and a catalog"

        domain0 = newDomain(client) {}
        domain0 = repository.save(domain0)
        Catalog catalog = newCatalog {
            name = 'a'
            domainTemplate = domain0
        }
        domain0.addToCatalogs(catalog)
        catalog = catalogRepository.save(catalog)

        CatalogItem item1 = newCatalogItem(catalog) {}
        CatalogItem item2 = newCatalogItem(catalog) {}
        CatalogItem item3 = newCatalogItem(catalog) {}

        clientRepository.save(client)

        Control control1= newControl(item1) {
            name = 'c1'
        }
        item1.element = control1
        item1 = catalogItemRepository.save(item1)
        control1 = item1.element

        Control control2= newControl(item2) {
            name = 'c2'
        }
        item2.element = control2
        item2 = catalogItemRepository.save(item2)
        control2 = item2.element

        Control control3= newControl(item3) {
            name = 'c3'
        }
        item3.element = control3

        TailoringReference tr1 = newTailoringReference() {
            catalogItem = item2
            referenceType = TailoringReferenceType.COPY
        }

        item3.tailoringReferences = [tr1]
        item3 = catalogItemRepository.save(item3)
        control3 = item3.element

        catalog.catalogItems = [item1, item2, item3]

        when: "saving"

        domain0 = repository.save(domain0)

        Domain d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.catalogs.size() == 1
        d.catalogs.first().id == catalog.id
        d.catalogs.first().catalogItems.size() == 3
        with(d.catalogs.first().catalogItems.sort {it.element.name}) {
            size == 3
            it[0].element.name == 'c1'
            it[1].element.name == 'c2'
            it[2].element.name == 'c3'
            it[2].tailoringReferences.size() == 1
            it[2].tailoringReferences[0].referenceType == TailoringReferenceType.COPY
            it[2].tailoringReferences[0].catalogItem.id == item2.id
        }
    }


    def 'domain with catalog with linked entities'() {
        given: "the domain template and a catalog"

        domain0 = newDomain(client) {}
        domain0 = repository.save(domain0)
        Catalog catalog = newCatalog {
            name = 'a'
            domainTemplate = domain0
        }
        domain0.addToCatalogs(catalog)
        catalog = catalogRepository.save(catalog)

        CatalogItem item1 = newCatalogItem(catalog) {}
        CatalogItem item2 = newCatalogItem(catalog) {}
        CatalogItem item3 = newCatalogItem(catalog) {}
        CatalogItem item4 = newCatalogItem(catalog) {}
        CatalogItem item5 = newCatalogItem(catalog) {}

        Unit unit = newUnit(client)
        unit = unitRepository.save(unit)
        client = clientRepository.save(client)

        Control control1= newControl(item1) {
            name = 'c1'
            abbreviation = 'c1'
            description = 'control number one'
        }
        item1.element = control1
        item1 = catalogItemRepository.save(item1)

        Control control2= newControl(item2) {
            name = 'c2'
        }
        item2.element = control2
        item2 = catalogItemRepository.save(item2)

        Control control3=newControl(item3) {
            name = 'c3'
        }
        item3.element = control3
        TailoringReference tr1 = newTailoringReference() {
            catalogItem = item2
            referenceType = TailoringReferenceType.COPY
        }
        item3.tailoringReferences = [tr1]
        item3 = catalogItemRepository.save(item3)

        Asset asset1 = newAsset(item4) {
            name = 'd1'
        }
        Asset asset2 = newAsset(item4) {
            name = 'sub-asset-1'
        }
        asset1.parts = [asset2]
        item4.element = asset1

        Process process1 = newProcess(item5) {
            name = 'p1'
        }
        item5.element = process1
        process1.setSubType(domain0, "Test")
        item5.tailoringReferences = [
            newTailoringReference() {
                catalogItem = item2
                referenceType = TailoringReferenceType.COPY
            },
            newTailoringReference() {
                catalogItem = item3
                referenceType = TailoringReferenceType.LINK
            }
        ]
        catalog.catalogItems = [
            item1,
            item2,
            item3,
            item4,
            item5] as Set

        when: "saving"

        domain0 = repository.save(domain0)

        Domain d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.catalogs.size() == 1
        d.catalogs.first().id == catalog.id
        d.catalogs.first().catalogItems.size() == 5
        with(d.catalogs.first().catalogItems.sort {it.element.name}) {
            it[0].element.name == 'c1'
            it[0].element.abbreviation == control1.abbreviation
            it[0].element.description == control1.description
            it[1].element.name == 'c2'
            it[2].element.name == 'c3'
            it[2].tailoringReferences.size() == 1
            it[2].tailoringReferences[0].referenceType == TailoringReferenceType.COPY
            it[2].tailoringReferences[0].catalogItem.id == item2.id
            it[3].element.name == 'd1'
            it[3].element.parts.first().name == asset2.name
            it[4].element.name == 'p1'
            it[4].element.subTypeAspects.size() == 1
            it[4].tailoringReferences.size() == 2
        }

        when: "create entities linked to catalogItems"

        Control controlEntity = newControl(unit) {
            name = 'c1'
        }
        controlEntity.description = "a linked entity"
        controlEntity.appliedCatalogItems = [item1] as Set
        controlEntity = controlRepository.save(controlEntity)
        clientRepository.save(client)

        Control c = controlRepository.findById(controlEntity.dbId).get()
        then: "the link remains"
        c.appliedCatalogItems.size()==1
        c.appliedCatalogItems.first()==item1
    }
}
