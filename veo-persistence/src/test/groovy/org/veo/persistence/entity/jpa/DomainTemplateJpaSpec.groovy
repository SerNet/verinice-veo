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
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Catalog
import org.veo.core.entity.Domain
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.test.VeoSpec

class DomainTemplateJpaSpec extends AbstractJpaSpec {
    @Autowired
    DomainTemplateDataRepository repository
    @Autowired
    DomainDataRepository domainRepository
    @Autowired
    TransactionTemplate txTemplate

    EntityFactory factory

    DomainTemplate domain0
    Domain domain1

    def setup() {
        factory = new EntityDataFactory()
        domain1 = newDomain(newClient ())
    }

    def 'domainTemplate is inserted'() {
        given: "the domain template"

        domain0 = newDomainTemplate() {
            riskDefinitions = ["id1":
                createRiskDefinition("id1"),
                "id2":createRiskDefinition("id2")
            ] as Map
        }

        when: "saving"

        domain0 = txTemplate.execute {
            return repository.save(domain0)
        }
        DomainTemplate d = txTemplate.execute {
            return repository.findById(domain0.dbId).get()
        }

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.riskDefinitions == domain0.riskDefinitions
    }

    def 'domainTemplate with catalog is inserted'() {
        given: "the domain template"

        domain0 = newDomainTemplate()

        when: "saving"

        domain0 = repository.save(domain0)
        Catalog c = newCatalog(domain0) {
            name = "a"
        }

        domain0 = txTemplate.execute {
            return repository.save(domain0)
        }
        DomainTemplate d = txTemplate.execute {
            return repository.findById(domain0.dbId).get()
        }

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.catalogs.size() == 1
    }

    def 'domainTemplate with catalog and catalog items'() {
        given: "the domain template and a catalog"

        domain0 = newDomainTemplate()
        Catalog catalog = newCatalog(domain0) {
            name = "a"
            newCatalogItem(it, VeoSpec.&newControl)
            newCatalogItem(it, VeoSpec.&newControl)
            newCatalogItem(it, VeoSpec.&newControl)
        }

        when: "saving"

        domain0 = txTemplate.execute {
            return repository.save(domain0)
        }
        DomainTemplate d = txTemplate.execute {
            return repository.findById(domain0.dbId).get()
        }

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.catalogs.first().id != null
        d.catalogs.first().catalogItems.size() == 3
    }

    def 'domainTemplate with catalog and catalog items with subtype'() {
        given: "the domain template and a catalog"

        domain0 = newDomainTemplate()
        Catalog catalog = newCatalog(domain0) {
            name = "a"
            newCatalogItem(it, VeoSpec.&newControl)
            newCatalogItem(it, VeoSpec.&newControl)
            newCatalogItem(it, VeoSpec.&newControl)
        }
        newCatalogItem(catalog, {
            newProcess(it) {
                name = 'p1'
                setSubType(domain0, "Test", "NEW")
            }
        })
        domain0.elementTypeDefinitions.add(newElementTypeDefinition(domain0, "process"))

        when: "saving"

        domain0 = txTemplate.execute {
            return repository.save(domain0)
        }
        DomainTemplate d = txTemplate.execute {
            return repository.findById(domain0.dbId).get()
        }

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.catalogs.first().id != null
        d.catalogs.first().name == 'a'
        d.catalogs.first().catalogItems.size() == 4
        d.catalogs.first().catalogItems.find { it.element.name == 'p1' }.element.getSubType(d).get() == 'Test'
        d.elementTypeDefinitions.size() == 1
    }

    def 'domainTemplate with catalog and catalog items with subtype and link'() {
        given: "the domain template and a catalog"

        domain0 = newDomainTemplate()
        Catalog catalog = newCatalog(domain0) {
            name = "a"
            newCatalogItem(it, VeoSpec.&newControl)
            newCatalogItem(it, VeoSpec.&newControl)
            newCatalogItem(it, VeoSpec.&newControl)
        }
        def itemP1 = newCatalogItem(catalog, {
            newProcess(it) {
                name = 'p1'
                setSubType(domain0, "Test", "NEW")
            }
        })

        newCatalogItem(catalog, { c->
            newProcess(c) {
                name = 'p2'
                setSubType(domain0, "Test1", "NOT-NEW")
                addToLinks(newCustomLink(itemP1.element, "p2->p1"))
            }
        })

        domain0.elementTypeDefinitions.add(newElementTypeDefinition(domain0, "process"))

        when: "saving"

        domain0 = txTemplate.execute {
            return repository.save(domain0)
        }
        DomainTemplate d = txTemplate.execute {
            return repository.findById(domain0.dbId).get()
        }

        then: "saved and loaded"

        d.name == domain0.name
        d.authority == domain0.authority
        d.templateVersion == domain0.templateVersion
        d.revision == domain0.revision
        d.catalogs.first().id != null
        d.catalogs.first().name == 'a'
        d.catalogs.first().catalogItems.size() == 5
        with (d.catalogs.first().catalogItems.find { it.element.name == 'p1' }) {
            element.getSubType(d).get() == 'Test'
            element.getStatus(d).get() == 'NEW'
        }
        with (d.catalogs.first().catalogItems.find { it.element.name == 'p2' }) {
            element.getSubType(d).get() == 'Test1'
            element.getStatus(d).get() == 'NOT-NEW'
            element.links.size() == 1
            element.links[0].type == 'p2->p1'
            element.links[0].target.name == 'p1'
        }
        d.elementTypeDefinitions.size() == 1
    }

    def 'fetches latest template by name'() {
        given: "different iso template versions and one unrelated mogs template"
        repository.save(newDomainTemplate {
            name = "ISO"
            templateVersion = "10.0.2"
        })
        repository.save(newDomainTemplate {
            name = "ISO"
            templateVersion = "2.3.4"
        })
        repository.save(newDomainTemplate {
            name = "ISO"
            templateVersion = "10.1.0"
        })
        repository.save(newDomainTemplate {
            name = "ISO"
            templateVersion = "10.1.1"
        })
        repository.save(newDomainTemplate {
            name = "MOGS"
            templateVersion = "10.2.3"
        })

        when:
        def result = repository.findLatestTemplateIdByName("ISO")

        then:
        result.present
        repository.findById(result.get()).get().templateVersion == "10.1.1"
    }
}
