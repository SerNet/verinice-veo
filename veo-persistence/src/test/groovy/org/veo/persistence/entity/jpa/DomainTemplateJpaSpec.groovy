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
import org.veo.core.entity.profile.ProfileDefinition
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

        Map data = [name: "Meeting-Metadaten",
            id: "40cd6764-830c-4af2-b88d-4bf02c9018e5",
            description: "z.B. Datum, Uhrzeit und Dauer der Kommunikation, Name des Meetings, Teilnehmer-IP-Adresse",
            links:[],
            customAspects:[],
            type:"asset",
            parts: [],
            domains:[
                "7de19fda-73dc-42b8-ab85-56bc04d27460" : [
                    subType: "AST_Datatype",
                    status: "FOR_REVIEW",
                    decisionResults :[],
                ]
            ]
        ]

        domain0 = newDomainTemplate() {
            riskDefinitions = ["id1":
                createRiskDefinition("id1"),
                "id2":createRiskDefinition("id2")
            ]
            profiles = [
                "demoUnit": ProfileDefinition.of(Collections.singleton(data), Collections.emptySet())
            ]
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
        d.profiles == domain0.profiles
    }

    def 'domainTemplate with catalog is inserted'() {
        when: "saving a domain template with a catalog"
        domain0 = txTemplate.execute {
            repository.save(newDomainTemplate {
                newCatalog(it)
            })
        }
        and: "retrieving it"
        def d = txTemplate.execute {
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

        domain0 = newDomainTemplate() {
            newCatalog(it) {
                name = "a"
                newCatalogItem(it, VeoSpec.&newControl)
                newCatalogItem(it, VeoSpec.&newControl)
                newCatalogItem(it, VeoSpec.&newControl)
            }
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
                associateWithDomain(domain0, "Test", "NEW")
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
                associateWithDomain(domain0, "Test", "NEW")
            }
        })

        newCatalogItem(catalog, { c->
            newProcess(c) {
                name = 'p2'
                associateWithDomain(domain0, "Test1", "NOT-NEW")
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

    def 'fetches templates by name'() {
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
        def result = repository.findTemplateIdsByName("ISO")

        then:
        result.size() == 4
        repository.findById(result.last()).get().templateVersion == "10.1.1"

        when:
        def currentVersion = repository.findCurrentTemplateVersion("ISO")

        then:
        currentVersion.get() == "10.1.1"
    }

    // TODO VEO-1072 becomes obsolete once we've abolished non-sem-vers from the DB
    def 'sem-ver beats pseudo-sem-ver'() {
        given: "a valid sem-ver and a two-number version"
        repository.save(newDomainTemplate {
            name = "ISO"
            templateVersion = "1.0"
        })
        repository.save(newDomainTemplate {
            name = "ISO"
            templateVersion = "1.1.0"
        })

        when:
        def currentVersion = repository.findCurrentTemplateVersion("ISO")

        then:
        currentVersion.get() == "1.1.0"
    }
}
