/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
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

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.repository.DomainTemplateRepository
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainTemplateRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolationException

@Transactional
class ClientRepositorySpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl repository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private DomainTemplateRepositoryImpl domainTemplateRepository

    def "create a simple client and a domain together"() {
        given: "a domain and a client"
        Client client = repository.save(newClient {
            name = "Test Client"
        })

        Domain domain = newDomain(client) {
            name = "27001"
            description = "ISO/IEC"
            abbreviation = "ISO"
        }
        client.addToDomains(domain)
        repository.save(client)

        when: "the client is retrieved from the database"
        Optional<Client> newClient = repository.findById(client.id)
        boolean isPresent = newClient.isPresent()

        Client c = newClient.get()

        then : "client and domain are valid"
        isPresent
        c.name == "Test Client"
        c.domains.size()==1
        c.domains.first().name == "27001"
        c.domains.first().owner == c

        when: "The client is persisted"
        repository.save(c)

        and: "The client is retrieved"
        newClient = repository.findById(client.id)
        isPresent = newClient.isPresent()
        c = newClient.get()

        then : "the client and domain are valid"
        isPresent
        c.name == "Test Client"
        c.domains.size()==1
        c.domains.first().name == "27001"
        c.domains.first().description == "ISO/IEC"
        c.domains.first().abbreviation == "ISO"
        c.domains.first().owner == c
    }

    def "create a simple client with unit"() {
        given: "a domain and a client"
        Key clientId = Key.newUuid()
        Client client = repository.save(newClient {
            id = clientId
        })
        Domain domain = newDomain(client) {
            name = "27001"
            description = "ISO/IEC"
            abbreviation = "ISO"
        }
        client.addToDomains(domain)
        repository.save(client)

        when: "loaded from db"
        Optional<Client> newClient = repository.findById(clientId)

        Client c = newClient.get()
        unitRepository.save(newUnit(c) {
            name = "new Unit"
        })

        newClient = repository.findById(clientId)
        def units = unitRepository.findByClient(c)

        then:
        newClient.isPresent()

        when:
        c = newClient.get()

        then: "test"
        c.domains.first().description == "ISO/IEC"
        c.domains.first().abbreviation == "ISO"
        units.size() == 1
    }

    def "create simple client, unit and some objects"() {
        given: "a domain and a client"
        Key clientId = Key.newUuid()
        Client client = repository.save(newClient{
            id = clientId
        })
        Domain domain = newDomain(client) {
            name = "27001"
            description = "ISO/IEC"
            abbreviation = "ISO"
        }
        client.addToDomains(domain)
        client = repository.save(client)

        Unit unit = newUnit(client) {
            name = "u1"
            domains = [domain] as Set
        }

        newPerson(unit) {
            associateWithDomain(domain, "NormalPerson", "NEW")
        }

        newAsset(unit) {
            associateWithDomain(domain, "NormalAsset", "NEW")
        }

        newProcess(unit) {
            associateWithDomain(domain, "NormalProcess", "NEW")
        }

        newDocument(unit) {
            associateWithDomain(domain, "NormalDocument", "NEW")
        }

        when:"save and load the client"
        client = repository.save(client)
        unit.client = client
        unitRepository.save(unit)

        Client newClient = repository.findById(clientId).get()
        def units = unitRepository.findByClient(newClient)

        then:"all data is present"
        newClient.name == client.name
        newClient.domains == client.domains
        //        newClient.units == client.units
        newClient.domains.first() == client.domains.first()
        newClient.domains.first().name == client.domains.first().name
        newClient.domains.first().abbreviation == client.domains.first().abbreviation
        newClient.domains.first().description == client.domains.first().description
        units.first().name == unit.name
        units.first().description == unit.description
        //        newClient.units.first().abbreviation == client.units.first().abbreviation
    }

    def "create a simple client and a domain together with catalog items"() {
        given: "a domain and a client"
        Domain domain = newDomain(newClient()) {domain ->
            name = "27001"
            description = "ISO/IEC"
            abbreviation = "ISO"
            applyElementTypeDefinition(newElementTypeDefinition(Control.SINGULAR_TERM, domain) {
                subTypes = [
                    ctl : newSubTypeDefinition {
                        statuses = ["NEW"]
                    }
                ]
            })
        }

        newCatalogItem(domain, {
            elementType = "control"
            subType = "ctl"
            status = "NEW"
        }
        )
        newCatalogItem(domain, {
            elementType = "control"
            subType = "ctl"
            status = "NEW"
        })
        newCatalogItem(domain, {
            elementType = "control"
            subType = "ctl"
            status = "NEW"
        })

        Client client = newClient {
            name = "Test Client"
            domains = [domain] as Set
        }

        repository.save(client)

        when: "the client is retrieved from the database"
        Optional<Client> newClient = repository.findById(client.id)
        boolean isPresent = newClient.isPresent()

        Client c = newClient.get()

        then : "client and domain are valid"
        isPresent
        c.name == "Test Client"
        c.domains.size()==1
        c.domains.first().name == "27001"
        c.domains.first().owner == c
        c.domains.first().catalogItems.size() == 3

        when: "The client is persisted"
        repository.save(c)

        and: "The client is retrieved"
        newClient = repository.findById(client.id)
        isPresent = newClient.isPresent()
        c = newClient.get()

        then : "the client and domain are valid"
        isPresent
        c.name == "Test Client"
        c.domains.size()==1
        c.domains.first().name == "27001"
        c.domains.first().description == "ISO/IEC"
        c.domains.first().abbreviation == "ISO"
        c.domains.first().owner == c
        c.domains.first().catalogItems.size() == 3
    }

    def "a client cannot be saved with an invalid catalog item"() {
        given: "a client with a catalog containing an item"
        Client client = newClient()
        def domain = newDomain(client)
        newCatalogItem(domain) {
            name = 'c3'
            subType = "ctl"
            status = "NEW"
        }

        when: "the client is saved"
        repository.save(client)

        then: "the validation cascades down to the invalid item"
        ConstraintViolationException ex = thrown(ConstraintViolationException)
        ex.getConstraintViolations().first().propertyPath ==~ /domains\[].catalogItems\[].elementType/
        ex.getConstraintViolations().first().getMessageTemplate() ==~ /.*NotNull.message.*/
    }

    def "finds clients where domain template not applied"() {
        given:
        def domainTemplateA = domainTemplateRepository.save(newDomainTemplate {})
        def domainTemplateB = domainTemplateRepository.save(newDomainTemplate {})
        clientDataRepository.saveAll ([
            newClient {
                name = "c0"
                domains = []
            },
            newClient {
                name = "c1"
                domains = [
                    newDomain(it),
                    newDomain(it) {
                        domainTemplate = domainTemplateB
                    },
                ]
            },
            newClient {
                name = "c2"
                domains = [
                    newDomain(it) {
                        domainTemplate = domainTemplateB
                    },
                    newDomain(it) {
                        domainTemplate = domainTemplateA
                    },
                ]
            },
            newClient {
                name = "c3"
                domains = [
                    newDomain(it) {
                        domainTemplate = domainTemplateA
                    },
                    newDomain(it),
                    newDomain(it),
                ]
            },
        ])

        when:
        def results = clientDataRepository.findAllWhereDomainTemplateNotApplied(domainTemplateA.idAsString)

        then:
        results*.name ==~ ["c0", "c1"]
    }
}
