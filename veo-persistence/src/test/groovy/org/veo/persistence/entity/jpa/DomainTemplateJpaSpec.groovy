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

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Catalog
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Domain
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.jpa.CatalogDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

class DomainTemplateJpaSpec extends AbstractJpaSpec {
    @Autowired
    DomainTemplateDataRepository repository;
    @Autowired
    CatalogDataRepository catalogRepository;
    EntityFactory factory;

    DomainTemplate domain0
    Domain domain1

    def setup() {
        factory = new EntityDataFactory()
        //domain0 = newDomainTemplate {}
        domain1 = newDomain {}
    }

    def 'domainTemplate is inserted'() {
        given: "the domain template"

        domain0 = newDomainTemplate {}

        when: "saving"

        domain0 = repository.save(domain0)
        DomainTemplate d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
    }

    def 'domainTemplate with catalog is inserted'() {
        given: "the domain template"

        domain0 = newDomainTemplate {}

        when: "saving"

        domain0 = repository.save(domain0)
        Catalog c = newCatalog {
            name = "a"
        }

        domain0.addToCatalogs(c)
        domain0 = repository.save(domain0)
        DomainTemplate d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.catalogs.size() == 1
    }

    def 'domainTemplate with catalog and catalog items'() {
        given: "the domain template and a catalog"

        domain0 = newDomainTemplate {}
        Catalog catalog = newCatalog {
            name = "a"
        }
        domain0.addToCatalogs(catalog)
        CatalogItem item1 = newCatalogItem(catalog) {}
        CatalogItem item2 = newCatalogItem(catalog) {}
        CatalogItem item3 = newCatalogItem(catalog) {}

        catalog.catalogItems = [item1, item2, item3] as Set

        when: "saving"

        domain0 = repository.save(domain0)

        DomainTemplate d = repository.findById(domain0.dbId).get()

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.catalogs.first().id == catalog.id
        d.catalogs.first().catalogItems.size() == 3
    }

}
