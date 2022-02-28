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

import java.nio.charset.StandardCharsets

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Catalog
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.ModelConsistencyException
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainTemplateRepositoryImpl

import groovy.json.JsonSlurper

/**
 * Integration test for the domain controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class DomainControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private DomainTemplateRepositoryImpl domainTemplateRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Domain testDomain
    private Domain completeDomain
    private Domain secondDomain
    private Catalog catalog
    private Domain domainSecondClient

    def setup() {
        txTemplate.execute {
            def rd = createRiskDefinition("id1")

            def client = createTestClient()
            newDomain(client) {
                name = "Domain 1"
                revision = "0"
                newCatalog(it) {
                    name = 'a'
                }
            }
            newDomain(client) {
                name = "Domain 2"
            }
            newDomain(client) { d->
                name = "Domain-complete"
                newCatalog(d) {c->
                    name = 'a'
                    newCatalogItem(c,{
                        newControl(it) {
                            name = 'c1'
                        }
                    })
                    newCatalogItem(c,{
                        newControl(it) {
                            name = 'c2'
                        }
                    })
                    newCatalogItem(c,{
                        newControl(it) {
                            name = 'c3'
                        }
                    })
                }
                riskDefinitions = ["id":rd] as Map
            }

            client = clientRepository.save(client)

            testDomain = client.domains.find{it.name == "Domain 1"}
            completeDomain = client.domains.find{it.name == "Domain-complete"}
            secondDomain = client.domains.find{it.name == "Domain 2"}
            catalog = testDomain.catalogs.first()

            def secondClient = clientRepository.save(newClient() {
                newDomain(it)
            })
            domainSecondClient = secondClient.domains.first()
        }
    }


    @WithUserDetails("user@domain.example")
    def "retrieve a Domain"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def results = get("/domains/${testDomain.id.uuidValue()}")

        then: "the eTag is set"
        getETag(results) != null
        and:
        def result = parseJson(results)
        result._self == "http://localhost/domains/${testDomain.id.uuidValue()}"
        result.name == testDomain.name
        result.catalogs.size() == 1
        when:
        def firstCatalog = result.catalogs.first()
        then:
        firstCatalog.displayName == 'a'
        firstCatalog.targetUri == "http://localhost/catalogs/${catalog.dbId}"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a Domain wrong client"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def results = get("/domains/${domainSecondClient.id.uuidValue()}", false)

        then: "the data is rejected"
        ClientBoundaryViolationException ex = thrown()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all domains for a client"() {
        when: "a request is made to the server"
        def result = parseJson(get("/domains?"))

        then: "the domains are returned"
        result.size == 3
        result*.name.sort().first() == 'Domain 1'
    }

    @WithUserDetails("content-creator")
    def "update the element type schema in a domain with an object schema"() {
        given:
        def schemaJson = DomainControllerMockMvcITSpec.getResourceAsStream('/os_scope.json').withCloseable {
            new JsonSlurper().parse(it)
        }
        when: "a request is made to the server"
        def result = post("/domains/${testDomain.id.uuidValue()}/elementtypedefinitions/scope/updatefromobjectschema", schemaJson, 204)

        then: "the domains are returned"
        result.andReturn().response.getContentAsString(StandardCharsets.UTF_8) == ''
        when: 'reloading the updated domain from the database'
        def updatedDomain = txTemplate.execute {
            def client = clientRepository.findById(testDomain.owningClient.get().id).get()
            def d = client.domains.find{it.id == testDomain.id}
            //initialize lazy associations
            d.elementTypeDefinitions.each {
                it.subTypes.each {
                    it.value.statuses
                }
            }
            d
        }
        then: 'the entity schemas are updated'

        with(updatedDomain.getElementTypeDefinition('scope').get()) {
            with(it.subTypes) {
                it.keySet() == [
                    'SCP_Scope',
                    'SCP_Processor',
                    'SCP_Controller',
                    'SCP_JointController',
                    'SCP_ResponsibleBody'
                ] as Set
            }
            with(it.translations) {
                it.size() == 2
                with (it.en) {
                    it.scope_management == 'Head of the responsible body'
                }
            }
        }
    }

    @WithUserDetails("user@domain.example")
    def "cannot update element type schema as regular user"() {

        given:
        def schemaJson = DomainControllerMockMvcITSpec.getResourceAsStream('/os_scope.json').withCloseable {
            new JsonSlurper().parse(it)
        }
        when: "a request is made to the server"
        def status = postUnauthorized("/domains/${testDomain.id.uuidValue()}/elementtypedefinitions/scope/updatefromobjectschema", schemaJson)
        then: "it is forbidden"
        status.andReturn().response.status == 403
    }


    @WithUserDetails("user@domain.example")
    def "export a Domain"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def results = get("/domains/${completeDomain.id.uuidValue()}/export")
        def result = parseJson(results)

        then: "the domain is exported"
        result.name == completeDomain.name
        result.catalogs.size() == 1
        result.elementTypeDefinitions != null
        result.riskDefinitions !=null
        when:
        def firstCatalog = result.catalogs.first()
        then:
        with(firstCatalog) {
            name == 'a'
            catalogItems.size() == 3
            domainTemplate !=null
        }
    }

    @WithUserDetails("content-creator")
    def "create a DomainTemplate"() {
        given: "a saved domain"
        when: "a request is made to the server"
        def count1 = txTemplate.execute {
            domainTemplateDataRepository.count()
        }

        def results = post("/domains/${testDomain.id.uuidValue()}/createdomaintemplate/update-1",[:])
        def result = parseJson(results)
        then: "a result is returned"
        result != null

        when: "loading the domaintemplates from the database"
        def count2 = txTemplate.execute {
            domainTemplateDataRepository.count()
        }
        def dt = txTemplate.execute {
            domainTemplateRepository.getAll().find{ it.name == "Domain 1" }
        }
        then: "one domaintemplate more"
        count2 == count1 +1
        dt.revision == "update-1"

        when: "create the next template"
        results = post("/domains/${testDomain.id.uuidValue()}/createdomaintemplate/update-2",[:])
        count2 = txTemplate.execute {
            domainTemplateDataRepository.count()
        }
        then: "one domaintemplate more"
        count2 == count1 +2
    }

    @WithUserDetails("content-creator")
    def "create a DomainTemplate without revison number"() {
        given: "a saved domain"
        when: "a request is made to the server"
        def count1 = txTemplate.execute {
            domainTemplateDataRepository.count()
        }

        def results = post("/domains/${secondDomain.id.uuidValue()}/createdomaintemplate/update-3",[:])
        def result = parseJson(results)
        then: "a result is returned"
        result != null

        when: "loading the domaintemplates from the database"
        def count2 = txTemplate.execute {
            domainTemplateDataRepository.count()
        }
        then: "one domaintemplate more"
        count2 == count1 +1

        when: "create the next template"
        results = post("/domains/${secondDomain.id.uuidValue()}/createdomaintemplate/update-3",[:],false)
        then: "the data is rejected"
        ModelConsistencyException ex = thrown()
    }

    @WithUserDetails("user@domain.example")
    def "create a DomainTemplate forbidden for user"() {
        given: "a saved domain"
        when: "a request is made to the server"
        def status = postUnauthorized("/domains/${testDomain.id.uuidValue()}/createdomaintemplate/latest", [:])
        then: "it is forbidden"
        status.andReturn().response.status == 403
    }
}