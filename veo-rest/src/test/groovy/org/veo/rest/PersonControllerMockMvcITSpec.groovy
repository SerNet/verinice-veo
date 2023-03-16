/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.adapter.presenter.api.DeviatingIdException
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Integration test for the unit personler. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class PersonControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private PersonRepositoryImpl personRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private DomainRepositoryImpl domainRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Domain dsgvoDomain

    def setup() {
        txTemplate.execute {
            def client = createTestClient()
            dsgvoDomain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)

            unit = newUnit(client) {
                name = "Test unit"
            }

            clientRepository.save(client)
            unitRepository.save(unit)
        }
    }

    @WithUserDetails("user@domain.example")
    def "create a person"() {
        given: "a request body"
        Map request = [
            name: 'New Person',
            owner: [
                displayName: 'test2',
                targetUri: 'http://localhost/units/' + unit.id.uuidValue()
            ]
        ]

        when: "a request is made to the server"
        def result = parseJson(post('/persons', request))

        then: "the location of the new person is returned"
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Person created successfully.'
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a person"() {
        given: "a saved person"
        def person = txTemplate.execute {
            personRepository.save(newPerson(unit) {
                name = 'Test person-1'
            })
        }

        when: "a request is made to the server"
        def results = get("/persons/${person.id.uuidValue()}")

        then: "the eTag is set"
        getETag(results) != null

        and:
        def result = parseJson(results)
        result._self == "http://localhost/persons/${person.id.uuidValue()}"
        result.name == 'Test person-1'
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all persons for a unit"() {
        given: "two saved persons"
        def person = newPerson(unit) {
            name = 'Test person-1'
        }
        def person2 = newPerson(unit) {
            name = 'Test person-2'
        }

        (person, person2) = txTemplate.execute {
            [person, person2].collect(personRepository.&save)
        }

        when: "a request is made to the server"
        def result = parseJson(get("/persons?unit=${unit.id.uuidValue()}"))

        then: "the persons are returned"
        result.items*.name.sort() == [
            'Test person-1',
            'Test person-2'
        ]
    }

    @WithUserDetails("user@domain.example")
    def "put a person"() {
        given: "a saved person"
        def person = txTemplate.execute {
            personRepository.save(newPerson(unit))
        }

        Map request = [
            name: 'New person-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: 'http://localhost/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ],
            domains: [
                (dsgvoDomain.id.uuidValue()): [
                    subType: "PER_Person",
                    status: "NEW",
                ]
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(person.id.uuidValue(), 0)
        ]
        def result = parseJson(put("/persons/${person.id.uuidValue()}", request, headers))

        then: "the person is found"
        result.name == 'New person-2'
        result.abbreviation == 'u-2'
        result.domains[dsgvoDomain.id.uuidValue()] == [
            subType: "PER_Person",
            status: "NEW",
            decisionResults: [:]
        ]
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put a person with a custom aspect"() {
        given: "a saved person"
        def person = txTemplate.execute {
            personRepository.save(newPerson(unit) {
                name = 'Test person-1'
                associateWithDomain(dsgvoDomain, "PER_Person", "NEW")
                applyCustomAspect(newCustomAspect("my.new.type", dsgvoDomain))
            })
        }

        Map request = [
            name: 'New person-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: 'http://localhost/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ], domains: [
                (dsgvoDomain.id.uuidValue()): [
                    subType: "PER_Person",
                    status: "NEW",
                ]
            ], customAspects:
            [
                'person_generalInformation' :
                [
                    domains: [],
                    attributes:  [
                        person_generalInformation_salutation:'Ms.',
                        person_generalInformation_familyName:'Schmidt'
                    ]
                ]
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(person.id.uuidValue(), person.version)
        ]
        def result = parseJson(put("/persons/${person.id.uuidValue()}", request, headers))

        then: "the person is found"
        result.name == 'New person-2'
        result.abbreviation == 'u-2'
        result.domains[dsgvoDomain.id.uuidValue()] == [
            subType: "PER_Person",
            status: "NEW",
            decisionResults: [:]
        ]
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()

        when:
        def entity = txTemplate.execute {
            personRepository.findById(person.id).get().tap() {
                // resolve proxy:
                customAspects.first()
            }
        }

        then:
        entity.name == 'New person-2'
        entity.abbreviation == 'u-2'
        with(entity.customAspects.first()) {
            type == 'person_generalInformation'
            attributes["person_generalInformation_salutation"] == 'Ms.'
            attributes["person_generalInformation_familyName"] == 'Schmidt'
        }
    }

    @WithUserDetails("user@domain.example")
    def "delete a person"() {
        given: "an existing person"
        def person = txTemplate.execute {
            personRepository.save(newPerson(unit) {
                name = 'Test person-1'
            })
        }

        when: "a delete request is sent to the server"
        delete("/persons/${person.id.uuidValue()}")

        then: "the person is deleted"
        !personRepository.exists(person.id)
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
            owner: [targetUri: 'http://localhost/units/' + unit.id.uuidValue()]
        ], headers, 403)

        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }

    @WithUserDetails("user@domain.example")
    def "can put back person"() {
        given: "a new person"
        def id = parseJson(post("/persons", [
            name: "new name",
            owner: [targetUri: "http://localhost/units/"+unit.id.uuidValue()]
        ])).resourceId
        def getResult = get("/persons/$id")

        expect: "putting the retrieved person back to be successful"
        put("/persons/$id", parseJson(getResult), [
            "If-Match": getETag(getResult)
        ])
    }
}
