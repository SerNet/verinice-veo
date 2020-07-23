/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import groovy.json.JsonSlurper

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.*
import org.veo.core.entity.custom.SimpleProperties
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.rest.configuration.WebMvcSecurityConfiguration

/**
 * Integration test for the unit personler. Uses mocked spring MVC environment.
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
class PersonControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private PersonRepositoryImpl personRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Domain domain
    private Domain domain1
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)

    def setup() {
        txTemplate.execute {
            domain = newDomain {
                description = "ISO/IEC"
                abbreviation = "ISO"
            }
            domain1 = newDomain {
                description = "ISO/IEC2"
                abbreviation = "ISO"
            }

            def client = newClient{
                id = clientId
                domains = [domain, domain1] as Set
            }
            unit = newUnit client, {
                name = "Test unit"
            }
            client.units << unit
            clientRepository.save(client)
        }
    }

    @WithUserDetails("user@domain.example")
    def "create a person"() {
        given: "a request body"

        Map request = [
            name: 'New Person',
            owner: [
                displayName: 'test2',
                href: '/units/' + unit.id.uuidValue()
            ]
        ]

        when: "a request is made to the server"

        def results = post('/persons', request)

        then: "the person is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new person is returned"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Person created successfully.'
    }


    @WithUserDetails("user@domain.example")
    def "retrieve a person"() {
        given: "a saved person"

        def person = newPerson unit, {
            name = 'Test person-1'
        }

        person = txTemplate.execute {
            personRepository.save(person)
        }

        when: "a request is made to the server"
        def results = get("/persons/${person.id.uuidValue()}")

        then: "the person is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test person-1'
        result.owner.href == "/units/"+unit.id.uuidValue()
    }


    @WithUserDetails("user@domain.example")
    def "retrieve all persons for a unit"() {
        given: "two saved persons"

        def person = newPerson unit, {
            name = 'Test person-1'
        }

        def person2 = newPerson unit, {
            name = 'Test person-2'
        }

        (person, person2) = txTemplate.execute {
            [person, person2].collect(personRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/persons?parent=${unit.id.uuidValue()}")

        then: "the persons are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2

        result.sort{it.name}.first().name == 'Test person-1'
        result.sort{it.name}.first().owner.href == "/units/"+unit.id.uuidValue()
        result.sort{it.name}[1].name == 'Test person-2'
        result.sort{it.name}[1].owner.href == "/units/"+unit.id.uuidValue()
    }


    @WithUserDetails("user@domain.example")
    def "put a person"() {
        given: "a saved person"

        Key<UUID> id = Key.newUuid()
        def person = newPerson unit, {
            it.id = id
        }
        person.setDomains([domain1] as Set)

        person = txTemplate.execute {
            personRepository.save(person)
        }

        Map request = [
            id: id.uuidValue(),
            name: 'New person-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                href: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ],  domains: [
                [
                    href: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ]
        ]


        when: "a request is made to the server"
        def results = put("/persons/${person.id.uuidValue()}", request)

        then: "the person is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New person-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.href == "/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put a person with custom properties"() {
        given: "a saved person"

        CustomProperties cp = new SimpleProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Person'] as Set)
        cp.setId(Key.newUuid())
        Key<UUID> id = Key.newUuid()
        def person = newPerson unit, {
            it.id = id
        }
        person.setCustomAspects([cp] as Set)
        person.setDomains([domain1] as Set)

        person = txTemplate.execute {
            personRepository.save(person)
        }

        Map request = [
            id: id.uuidValue(),
            name: 'New person-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                href: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ], domains: [
                [
                    href: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ], customAspects:
            [
                'my.aspect-test' :
                [
                    id: '00000000-0000-0000-0000-000000000000',
                    type : 'my.aspect-test1',
                    applicableTo: [
                        "Person"
                    ],
                    domains: [],
                    attributes:  [
                        test1:'value1',
                        test2:'value2'
                    ]
                ]
            ]
        ]

        when: "a request is made to the server"
        def results = put("/persons/${person.id.uuidValue()}", request)

        then: "the person is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New person-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.href == "/units/"+unit.id.uuidValue()
        def entity = txTemplate.execute { personRepository.findById(id).get() }
        entity.name == 'New person-2'
        entity.abbreviation == 'u-2'
        entity.customAspects.first().type == 'my.aspect-test1'
        entity.customAspects.first().applicableTo == ['Person'] as Set
        entity.customAspects.first().stringProperties.test1 == 'value1'
        entity.customAspects.first().stringProperties.test2 == 'value2'
    }

    @WithUserDetails("user@domain.example")
    def "delete a person"() {

        given: "an existing person"
        Key<UUID> id = Key.newUuid()
        def person = newPerson unit, {
            it.id = id
        }
        person.setDomains([domain1] as Set)

        person = txTemplate.execute {
            personRepository.save(person)
        }

        when: "a delete request is sent to the server"

        def results = delete("/persons/${person.id.uuidValue()}")

        then: "the person is deleted"
        results.andExpect(status().isOk())
        !personRepository.exists(id)
    }
}