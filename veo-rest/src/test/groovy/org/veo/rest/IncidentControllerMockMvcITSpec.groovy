/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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

import org.veo.adapter.presenter.api.DeviatingIdException
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.IncidentRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.rest.configuration.WebMvcSecurityConfiguration

import groovy.json.JsonSlurper

/**
 * Integration test for the incident controller. Uses mocked spring MVC environment.
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
class IncidentControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private IncidentRepositoryImpl incidentRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Domain domain
    private Domain domain1
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
    String salt = "salt-for-etag"

    def setup() {
        txTemplate.execute {
            domain = newDomain {
                abbreviation = "D"
                name = "Domain"
            }

            domain1 = newDomain {
                abbreviation = "D1"
                name = "Domain 1"
            }

            def client= newClient {
                id = clientId
                domains = [domain, domain1] as Set
            }

            unit = newUnit(client) {
                name = "Test unit"
            }

            unit.client = client
            clientRepository.save(client)
            unitRepository.save(unit)
        }
        ETag.setSalt(salt)
    }


    @WithUserDetails("user@domain.example")
    def "create an incident"() {
        given: "a request body"

        Map request = [
            name: 'New Incident',
            owner: [
                displayName: 'incidentDataProtectionObjectivesEugdprEncryption',
                targetUri: '/units/' + unit.id.uuidValue()
            ]
        ]

        when: "a request is made to the server"

        def results = post('/incidents', request)

        then: "the incident is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new incident is returned"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Incident created successfully.'
    }

    @WithUserDetails("user@domain.example")
    def "retrieve an incident"() {
        given: "a saved incident"
        def incident = txTemplate.execute {
            incidentRepository.save(newIncident(unit) {
                name = 'Test incident-1'
            })
        }


        when: "a request is made to the server"
        def results = get("/incidents/${incident.id.uuidValue()}")
        String expectedETag = DigestUtils.sha256Hex(incident.id.uuidValue() + "_" + salt + "_" + Long.toString(incident.getVersion()))

        then: "the incident is found"
        results.andExpect(status().isOk())
        and: "the eTag is set"
        String eTag = results.andReturn().response.getHeader("ETag")
        eTag != null
        getTextBetweenQuotes(eTag).equals(expectedETag)
        and:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test incident-1'
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all incidents for a unit"() {
        given: "a saved incident"
        def incident = newIncident(unit) {
            name = 'Test incident-1'
        }
        def incident2 = newIncident(unit) {
            name = 'Test incident-2'
        }
        (incident, incident2) = txTemplate.execute {
            [incident, incident2].collect(incidentRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/incidents?unit=${unit.id.uuidValue()}")

        then: "the incidents are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2

        result.sort{it.name}.first().name == 'Test incident-1'
        result.sort{it.name}.first().owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
        result.sort{it.name}[1].name == 'Test incident-2'
        result.sort{it.name}[1].owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieving all incidents for a unit returns composite entities and their parts"() {
        given: "a saved incident and a composite incident containing it"
        txTemplate.execute {
            incidentRepository.save(newIncident(unit) {
                name = 'Test composite incident-1'
                parts <<  newIncident(unit) {
                    name = 'Test incident-1'
                }
            })
        }

        when: "a request is made to the server"
        def results = get("/incidents?unit=${unit.id.uuidValue()}")

        then: "the incidents are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2
        result*.name as Set == [
            'Test incident-1',
            'Test composite incident-1'
        ] as Set
    }

    @WithUserDetails("user@domain.example")
    def "put an incident"() {
        given: "a saved incident"
        def incident = txTemplate.execute {
            incidentRepository.save(newIncident(unit) {
                domains = [domain1] as Set
            })
        }

        Map request = [
            name: 'New incident-2',
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
            'If-Match': ETag.from(incident.id.uuidValue(), 1)
        ]
        def results = put("/incidents/${incident.id.uuidValue()}", request, headers)

        then: "the incident is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New incident-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "delete an incident"() {

        given: "an existing incident"
        def incident = txTemplate.execute {
            incidentRepository.save(newIncident(unit))
        }


        when: "a delete request is sent to the server"

        def results = delete("/incidents/${incident.id.uuidValue()}")

        then: "the incident is deleted"
        results.andExpect(status().isOk())
        incidentRepository.findById(incident.id).empty
    }

    @WithUserDetails("user@domain.example")
    def "can't put an incident with another incident's ID"() {
        given: "two incidents"
        def incident1 = txTemplate.execute({
            incidentRepository.save(newIncident(unit, {
                name = "old name 1"
            }))
        })
        def incident2 = txTemplate.execute({
            incidentRepository.save(newIncident(unit, {
                name = "old name 2"
            }))
        })
        when: "a put request tries to update incident 1 using the ID of incident 2"
        Map headers = [
            'If-Match': ETag.from(incident1.id.uuidValue(), 1)
        ]
        put("/incidents/${incident2.id.uuidValue()}", [
            id: incident1.id.uuidValue(),
            name: "new name 1",
            owner: [targetUri: '/units/' + unit.id.uuidValue()]
        ], headers, false)
        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }

    @WithUserDetails("user@domain.example")
    def "can put back incident"() {
        given: "a new incident"
        def id = parseJson(post("/incidents/", [
            name: "new name",
            owner: [targetUri: "/units/"+unit.id.uuidValue()]
        ])).resourceId
        def getResult = get("/incidents/$id")

        expect: "putting the retrieved incident back to be successful"
        put("/incidents/$id", parseJson(getResult), [
            "If-Match": getTextBetweenQuotes(getResult.andReturn().response.getHeader("ETag"))
        ])
    }

    @WithUserDetails("user@domain.example")
    def "can put back incident with parts"() {
        given: "a saved incident and a composite"

        def incident = txTemplate.execute {
            incidentRepository.save(newIncident(unit) {
                name = 'Test incident'
            })
        }
        Map request = [
            name: 'Composite incident',
            owner: [
                targetUri: "/units/${unit.id.uuidValue()}"
            ],
            parts: [
                [targetUri : "http://localhost/incidents/${incident.id.uuidValue()}"]
            ]
        ]


        def id = parseJson(post("/incidents/", request)).resourceId
        def getResult = get("/incidents/$id")

        expect: "putting the retrieved incident back to be successful"
        put("/incidents/$id", parseJson(getResult), [
            "If-Match": getTextBetweenQuotes(getResult.andReturn().response.getHeader("ETag"))
        ])
    }

    @WithUserDetails("user@domain.example")
    def "deleting composite entity leaves parts and containers intact"() {
        given: "Incident a, b, and C where a ∈ b, a ∈ c, b ∈ c"
        def (a,b,c) = txTemplate.execute {
            def a = incidentRepository.save(newIncident(unit))
            def b = incidentRepository.save(newIncident(unit) {
                parts << a
            })
            def c = incidentRepository.save(newIncident(unit) {
                parts << b << a
            })
            [a, b, c]
        }

        when: "the server is asked to delete b"
        def results = delete("/incidents/${b.id.uuidValue()}")

        then: "b is deleted"
        results.andExpect(status().isOk())
        incidentRepository.findById(b.id).empty

        and: "a and c are left intact"
        incidentRepository.findById(a.id).with {
            assert it.present
            assert it.get().id == a.id
        }
        def incidentFromDb = txTemplate.execute {
            return incidentRepository.findById(c.id).with {
                assert it.present
                it.get().with {
                    assert parts.size() == 1
                    assert parts.first().id == a.id
                }
                it.get()
            }
        }
        incidentFromDb.id == c.id
    }
}
