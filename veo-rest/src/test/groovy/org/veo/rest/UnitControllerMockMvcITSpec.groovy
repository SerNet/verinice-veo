/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
package org.veo.rest

import static org.hamcrest.Matchers.*
import static org.springframework.boot.jdbc.EmbeddedDatabaseConnection.H2
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.support.TransactionTemplate

import groovy.json.JsonSlurper

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.rest.configuration.WebMvcSecurityConfiguration

/**
 * Integration test for the unit controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = [WebMvcSecurityConfiguration]
)
@EnableAsync
@ComponentScan("org.veo.rest")
@AutoConfigureTestDatabase(connection = H2)
@TestPropertySource(locations="classpath:application-test.properties")
class UnitControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private UnitRepositoryImpl urepository

    @Autowired
    private ClientRepositoryImpl repository

    @Autowired
    private TransactionTemplate txTemplate

    private Client client

    private Domain domain

    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)

    def setup() {
        domain = newDomain {
            name = "27001"
            description = "ISO/IEC"
            abbreviation = "ISO"
        }

        client = txTemplate.execute {
            repository.save(newClient {
                id = clientId
                name = "Demo Client"
                domains = [domain] as Set
            })
        }
    }

    @WithUserDetails("user@domain.example")
    def "create a unit for a new client"() {

        given: "a request body"

        Map request = [
            name: 'New unit'
        ]

        when: "a request is made to the server"

        def results = post('/units', request)

        then: "the unit is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new unit is returned"
        results.andExpect(jsonPath('$.success').value("true"))
        results.andExpect(jsonPath('$.resourceId', is(not(emptyOrNullString()))))
        results.andExpect(jsonPath('$.message').value('Unit created successfully.'))
    }

    @WithUserDetails("user@domain.example")
    def "get a unit"() {
        def id = Key.newUuid()
        txTemplate.execute {
            client = repository.findById(clientId).get()
        }
        Unit unit = newUnit client, {
            it.id = id
            name = "Test unit"
            setAbbreviation("u-1")
            setDescription("description")
            setDomains([client.domains.first()] as Set)
        }
        txTemplate.execute {
            unit = urepository.save(unit)
        }

        when: "a request is made to the server"

        def results = get("/units/${id.uuidValue()}")

        then: "the unit is returned with HTTP status code 200"

        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == "Test unit"
        result.abbreviation == "u-1"
        result.domains.first().displayName == "ISO 27001"
        def references = result.references
        references.size() == 2
        references*.displayName == [
            'Demo Client',
            'ISO 27001'
        ]
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all units"() {
        def id = Key.newUuid()
        txTemplate.execute {
            client = repository.findById(clientId).get()
        }
        Unit unit = newUnit client, {
            it.id = id
            name = "Test unit foo"
            setAbbreviation("u-1")
            setDescription("description")
            setDomains([client.domains.first()] as Set)
        }
        txTemplate.execute {
            unit = urepository.save(unit)
            client.addToUnits(unit)
            repository.save(client)
        }

        when: "a request is made to the server"

        def results = get("/units")

        then: "the units are returned"

        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.size() == 1
        result.first().name == "Test unit foo"
    }

    def createTestClientUnit(Key id) {
        Unit unit
        Client c
        txTemplate.execute {
            c = repository.findById(clientId, null).get()
            // we do not use Client.createUnit() here because we want to specify the unit's ID:
            unit = newUnit c, {
                it.id = id
                name = "Test unit-5"
                setAbbreviation("u-1")
                setDescription("description")
            }
            c.addToUnits(unit)
            c = repository.save(c)
        }
        return c
    }

    @WithUserDetails("user@domain.example")
    def "update a unit"() {
        given: "a unit"

        def id = Key.newUuid()
        Client c = createTestClientUnit(id)

        when: "the unit is updated by changing the name and adding a domain"

        Map request = [
            id: id.uuidValue(),
            name: 'New unit-2',
            abbreviation: 'u-2',
            description: 'desc',
            domains: [
                [
                    href: '/domains/'+c.domains.first().id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ]
        ]

        def results = put("/units/${id.uuidValue()}", request)

        then: "the unit is updated and a status code returned"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == "New unit-2"
        result.abbreviation == "u-2"
    }

    @WithUserDetails("user@domain.example")
    def "Update a unit multiple times"() {

        given: "a unit"
        def id = Key.newUuid()
        Client c = createTestClientUnit(id)

        when: "the unit is updated first by changing the name and adding a domain"
        Map request1 = [
            id: id.uuidValue(),
            name: 'New unit-2',
            abbreviation: 'u-2',
            description: 'desc',
            domains: [
                [
                    href: '/domains/'+c.domains.first().id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ]
        ]

        def results1 = put("/units/${id.uuidValue()}", request1)

        and: "the unit is updated secondly by changing the name and removing a domain"
        Map request2 = [
            id: id.uuidValue(),
            name: 'New unit-3',
            abbreviation: 'u-3',
            description: 'desc',
            domains: []]

        def results2 = put("/units/${id.uuidValue()}", request2)

        then: "the unit was correctly modified the first time"
        results1.andExpect(status().isOk())
        def result1 = new JsonSlurper().parseText(results1.andReturn().response.contentAsString)
        result1.name == "New unit-2"
        result1.abbreviation == "u-2"
        result1.domains.size() == 1

        then: "the unit was correctly modified the second time"
        results2.andExpect(status().isOk())
        def result2 = new JsonSlurper().parseText(results2.andReturn().response.contentAsString)
        result2.name == "New unit-3"
        result2.abbreviation == "u-3"
        result2.domains.size() == 0
    }

    @WithUserDetails("user@domain.example")
    def "create sub unit for a unit"() {

        //        def id = Key.newUuid();
        //        Client c = null
        //        Unit unit = null
        //        txTemplate.execute {
        //            c = repository.findById(clientId).get()
        //            unit = newUnit c, {
        //                    it.id = id
        //                    name = "Test unit-2"
        //                    setAbbreviation("u-3")
        //                    setDescription("description")
        //            }
        //            unit = urepository.save(unit)
        //        }

        given: "a request body"

        Map request1 = [
            name: 'parent-unit-1'
        ]

        def results1 = post('/units', request1)

        def parent = new JsonSlurper().parseText(results1.andReturn().getResponse().contentAsString)

        Map request = [
            name: 'sub-unit-1',
            parent: [
                href: '/units/'+parent.resourceId,
                displayName: 'test ddd'
            ]
        ]

        when: "a request is made to the server"

        def results = post('/units', request)

        then: "the unit is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new unit is returned"
        results.andExpect(jsonPath('$.success').value("true"))
        results.andExpect(jsonPath('$.resourceId', is(not(emptyOrNullString()))))
        results.andExpect(jsonPath('$.message').value('Unit created successfully.'))

        when: "get the sub unit"
        String temp = results.andReturn().getResponse().contentAsString
        def su = new JsonSlurper().parseText(temp)

        results = get("/units/${su.resourceId}")

        then: "sub unit has parent"

        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == "sub-unit-1"
        result.parent.href == "/units/"+parent.resourceId

        and: "sub unit refrences contain parent"
        def references = result.references
        references.size() == 2
        references*.displayName == [
            'Demo Client',
            'parent-unit-1'
        ]

        when: "get the parent"

        results = get("/units/${parent.resourceId}")

        result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then: " parent contains the child"

        results.andExpect(status().isOk())
        result.name == "parent-unit-1"


        when: "load the client"

        Client tclient = null
        txTemplate.execute {
            tclient = repository.findById(clientId).get()
        }

        then: "the data is persistent"
        tclient.units.find { it.id.uuidValue() == parent.resourceId }.units.first().name == "sub-unit-1"

    }

    @WithUserDetails("user@domain.example")
    def "delete a unit"() {
        given: "a unit"

        def id = Key.newUuid()
        Client c = createTestClientUnit(id)

        when: "the client is loaded"
        Client tclient = txTemplate.execute {
            repository.findById(clientId, null).get()
        }

        then: "the unit is present"
        tclient.getUnit(id).present

        when: "the unit is deleted"
        def results = delete("/units/${id.uuidValue()}")

        then: "the unit is removed and a status code returned"
        results.andExpect(status().isNoContent())


        when: "the client is loaded again"
        tclient = null
        txTemplate.execute {
            tclient = repository.findById(clientId, null).get()
        }

        then: "the unit was removed from the client"
        tclient.getUnit(id).empty

        when: "the unit is loaded again"
        def unit = txTemplate.execute {
            urepository.findById(id, null)
        }

        then: "the unit is no longer present"
        unit.empty
    }
}
