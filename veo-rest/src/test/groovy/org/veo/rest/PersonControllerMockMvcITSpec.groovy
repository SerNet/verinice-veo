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

import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.rest.configuration.WebMvcSecurityConfiguration

import groovy.json.JsonSlurper

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
class PersonControllerMockMvcITSpec extends VeoRestMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private PersonRepositoryImpl personRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    TransactionTemplate txTemplate
    @Autowired
    private EntityDataFactory entityFactory

    private Unit unit
    private Domain domain
    private Domain domain1
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
    String salt = "salt-for-etag"

    def setup() {
        txTemplate.execute {

            domain = entityFactory.createDomain()
            domain.description = "ISO/IEC"
            domain.abbreviation = "ISO"
            domain.name = "ISO"
            domain.id = Key.newUuid()

            domain1 = entityFactory.createDomain()
            domain1.description = "ISO/IEC2"
            domain1.abbreviation = "ISO"
            domain1.name = "ISO"
            domain1.id = Key.newUuid()

            def client= entityFactory.createClient()
            client.id = clientId
            client.domains = [domain, domain1] as Set

            unit = entityFactory.createUnit()
            unit.name = "Test unit"
            unit.id = Key.newUuid()

            unit.client = client
            Client c = clientRepository.save(client)
            unitRepository.save(unit)
        }
        ETag.setSalt(salt)
    }

    @WithUserDetails("user@domain.example")
    def "create a person"() {
        given: "a request body"

        Map request = [
            name: 'New Person',
            owner: [
                displayName: 'test2',
                targetUri: '/units/' + unit.id.uuidValue()
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

        def person = entityFactory.createPerson()
        person.id = Key.newUuid()
        person.name = 'Test person-1'
        person.owner = unit

        person = txTemplate.execute {
            personRepository.save(person)
        }

        when: "a request is made to the server"
        def results = get("/persons/${person.id.uuidValue()}")
        String expectedETag = DigestUtils.sha256Hex(person.id.uuidValue() + "_" + salt + "_" + Long.toString(person.getVersion()))

        then: "the person is found"
        results.andExpect(status().isOk())
        and: "the eTag is set"
        String eTag = results.andReturn().response.getHeader("ETag")
        eTag != null
        getTextBetweenQuotes(eTag).equals(expectedETag)
        and:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test person-1'
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all persons for a unit"() {
        given: "two saved persons"

        def person = entityFactory.createPerson()
        person.id = Key.newUuid()
        person.name = 'Test person-1'
        person.owner = unit

        def person2 = entityFactory.createPerson()
        person2.id = Key.newUuid()
        person2.name = 'Test person-2'
        person2.owner = unit

        (person, person2) = txTemplate.execute {
            [person, person2].collect(personRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/persons?unit=${unit.id.uuidValue()}")

        then: "the persons are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2

        result.sort{it.name}.first().name == 'Test person-1'
        result.sort{it.name}.first().owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
        result.sort{it.name}[1].name == 'Test person-2'
        result.sort{it.name}[1].owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put a person"() {
        given: "a saved person"

        Key<UUID> id = Key.newUuid()
        def person = entityFactory.createPerson()
        person.id = id
        person.name = 'Test person-1'
        person.owner = unit
        person.setDomains([domain1] as Set)

        person = txTemplate.execute {
            personRepository.save(person)
        }

        Map request = [
            name: 'New person-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ],  domains: [
                [
                    targetUri: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ]
        ]


        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(person.id.uuidValue(), 1)
        ]
        def results = put("/persons/${person.id.uuidValue()}", request, headers)

        then: "the person is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New person-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put a person with custom properties"() {
        given: "a saved person"

        CustomProperties cp = entityFactory.createCustomProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Person'] as Set)

        Key<UUID> id = Key.newUuid()
        def person = entityFactory.createPerson()
        person.id = id
        person.name = 'Test person-1'
        person.owner = unit
        person.setDomains([domain1] as Set)
        person.setCustomAspects([cp] as Set)

        person = txTemplate.execute {
            personRepository.save(person)
        }

        Map request = [
            name: 'New person-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ], domains: [
                [
                    targetUri: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ], customAspects:
            [
                'my.aspect-test' :
                [
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
        Map headers = [
            'If-Match': ETag.from(person.id.uuidValue(), 1)
        ]
        def results = put("/persons/${person.id.uuidValue()}", request, headers)

        then: "the person is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New person-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()

        when:
        def entity = txTemplate.execute {
            personRepository.findById(id).get().tap() {
                // resolve proxy:
                customAspects.first()
            }
        }

        then:
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
        def person = entityFactory.createPerson()
        person.id = id
        person.name = 'Test person-1'
        person.owner = unit
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

    @WithUserDetails("user@domain.example")
    def "can't put a person with another person's ID"() {
        given: "two persons"
        def person1 = txTemplate.execute({
            personRepository.save(newPerson(unit, {
                name = "old name 1"
            }))
        })
        def person2 = txTemplate.execute({
            personRepository.save(newPerson(unit, {
                name = "old name 2"
            }))
        })
        when: "a put request tries to update person 1 using the ID of person 2"
        Map headers = [
            'If-Match': ETag.from(person1.id.uuidValue(), 1)
        ]
        put("/persons/${person2.id.uuidValue()}", [
            id: person1.id.uuidValue(),
            name: "new name 1",
            owner: [targetUri: '/units/' + unit.id.uuidValue()]
        ], headers, false)
        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }
}
