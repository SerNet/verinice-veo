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

import static org.veo.core.entity.TailoringReferenceType.LINK

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Domain
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.profile.ProfileDefinition
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

class DomainTemplateJpaSpec extends AbstractJpaSpec {
    @Autowired
    ClientDataRepository clientRepository
    @Autowired
    DomainTemplateDataRepository repository
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
                "exampleOrganization": new ProfileDefinition("Beispieldaten", "So wird's gemacht ", "de_DE",
                Collections.singleton(data), Collections.emptySet())
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
        d.riskDefinitions == domain0.riskDefinitions
        d.jsonProfiles == domain0.jsonProfiles
    }

    def 'domainTemplate with items'() {
        given: "the domain template and a catalog"
        domain0 = newDomainTemplate() {
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
        d.catalogItems.size() == 3
    }

    def 'domainTemplate with catalog and catalog items with subtype'() {
        given: "the domain template and a catalog"
        domain0 = newDomainTemplate()
        newCatalogItem(domain0, {
            elementType = "control"
        })
        newCatalogItem(domain0, {
            elementType = "control"
        })
        newCatalogItem(domain0, {
            elementType = "control"
        })
        newCatalogItem(domain0, {
            elementType = "process"
            name = "p1"
            subType = "Test"
            status = "NEW"
        })

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
        d.catalogItems.size() == 4
        d.catalogItems.find { it.name == 'p1' }.subType == 'Test'
        d.elementTypeDefinitions.size() == 8
    }

    def 'domainTemplate with catalog items with subtype and link'() {
        given: "the domain template and a catalog"
        domain0 = newDomainTemplate()
        newCatalogItem(domain0, {
            elementType = "control"
        })
        newCatalogItem(domain0,{
            elementType = "control"
        })
        newCatalogItem(domain0,{
            elementType = "control"
        })
        def itemP1 = newCatalogItem(domain0, {
            name = 'p1'
            status = "NEW"
            subType = "Test"
            elementType = "control"
        })

        def ci = newCatalogItem(domain0, {
            name = 'p2'
            status = "NOT-NEW"
            subType = "Test1"
            elementType = "control"
        })

        newLinkTailoringReference(ci, LINK) {
            target = itemP1
            linkType = "p2->p1"
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
        with (d.catalogItems.find { it.name == 'p1' }) {
            subType == 'Test'
            status == 'NEW'
            elementType == "control"
        }
        with (d.catalogItems.find { it.name == 'p2' }) {
            subType == 'Test1'
            status == 'NOT-NEW'
            elementType == "control"

            tailoringReferences.size() == 1
            tailoringReferences[0].linkType == 'p2->p1'
            tailoringReferences[0].target.name == 'p1'
        }
        d.elementTypeDefinitions.size() == 8
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

        when:
        def currentVersion = repository.findCurrentTemplateVersion("ISO")

        then:
        currentVersion.get() == "10.1.1"
    }

    def "queries do not fetch domains"() {
        given: "a domain"
        def client = clientRepository.save(newClient())
        def domainId = newDomain(client) {
            name = "main"
            templateVersion = "0.1.0"
        }.idAsString
        clientRepository.save(client)

        expect: "template queries to ignore the domain"
        repository.findCurrentTemplateVersion("main").empty
        repository.findLatestTemplateIdByName("main").empty
        repository.findTemplateIdsByName("main").empty
        repository.findByIdWithProfilesAndRiskDefinitions(domainId).empty
    }
}
