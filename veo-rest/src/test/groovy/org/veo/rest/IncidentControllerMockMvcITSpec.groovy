/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
import org.veo.persistence.access.IncidentRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Integration test for the incident controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
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

    def setup() {
        txTemplate.execute {
            def client = createTestClient()

            domain = newDomain(client) {
                abbreviation = "D"
                name = "Domain"
                applyElementTypeDefinition(newElementTypeDefinition("incident", it) {
                    subTypes = [
                        NormalIncident: newSubTypeDefinition()
                    ]
                })
            }

            domain1 = newDomain(client) {
                abbreviation = "D1"
                name = "Domain 1"
            }

            unit = newUnit(client) {
                name = "Test unit"
            }

            unit.client = client
            clientRepository.save(client)
            unitRepository.save(unit)
        }
    }

    @WithUserDetails("user@domain.example")
    def "create an incident"() {
        given: "a request body"
        Map request = [
            name: 'New Incident',
            owner: [
                displayName: 'incidentDataProtectionObjectivesEugdprEncryption',
                targetUri: 'http://localhost/units/' + unit.idAsString
            ]
        ]

        when: "a request is made to the server"
        def result = parseJson(post('/incidents', request))

        then: "the location of the new incident is returned"
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
        def results = get("/incidents/${incident.idAsString}")

        then: "the eTag is set"
        getETag(results) != null

        and:
        def result = parseJson(results)
        result._self == "http://localhost/incidents/${incident.idAsString}"
        result.name == 'Test incident-1'
        result.owner.targetUri == "http://localhost/units/"+unit.idAsString
    }

    @WithUserDetails("user@domain.example")
    def "regular JSON is returned if Accept header is missing"() {
        given:
        def incident = txTemplate.execute {
            incidentRepository.save(newIncident(unit) {
                name = 'Test incident-1'
            })
        }

        when:
        def results = get("/incidents/${incident.idAsString}", 200, null)

        then:
        results.andReturn().request.getHeader('Accept') == null
        results.andReturn().response.getHeader('Content-Type') == 'application/json'

        when:
        def result = parseJson(results)

        then:
        result._self == "http://localhost/incidents/${incident.idAsString}"
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

        when: "requesting all incidents in the unit"
        def result = parseJson(get("/incidents?unit=${unit.idAsString}"))

        then: "the incidents are returned"
        result.items*.name.sort() == [
            'Test incident-1',
            'Test incident-2'
        ]
    }

    @WithUserDetails("user@domain.example")
    def "retrieving all incidents for a unit returns composite elements and their parts"() {
        given: "a saved incident and a composite incident containing it"
        txTemplate.execute {
            incidentRepository.save(newIncident(unit) {
                name = 'Test composite incident-1'
                parts <<  newIncident(unit) {
                    name = 'Test incident-1'
                }
            })
        }

        when: "requesting all incidents in the unit"
        def result = parseJson(get("/incidents?unit=${unit.idAsString}"))

        then: "the incidents are returned"
        result.items*.name as Set == [
            'Test incident-1',
            'Test composite incident-1'
        ] as Set
    }

    @WithUserDetails("user@domain.example")
    def "put an incident"() {
        given: "a saved incident"
        def incident = txTemplate.execute {
            incidentRepository.save(newIncident(unit) {
                associateWithDomain(domain, "NormalIncident", "NEW")
            })
        }

        Map request = [
            name: 'New incident-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: 'http://localhost/units/'+unit.idAsString,
                displayName: 'test unit'
            ],
            domains: [
                (domain.idAsString): [
                    subType: "NormalIncident",
                    status: "NEW",
                ]
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(incident.idAsString, incident.version)
        ]
        def result = parseJson(put("/incidents/${incident.idAsString}", request, headers))

        then: "the incident is found"
        result.name == 'New incident-2'
        result.abbreviation == 'u-2'
        result.domains[domain.idAsString] == [
            subType: "NormalIncident",
            status: "NEW",
            decisionResults: [:]
        ]
        result.owner.targetUri == "http://localhost/units/"+unit.idAsString
    }

    @WithUserDetails("user@domain.example")
    def "delete an incident"() {
        given: "an existing incident"
        def incident = txTemplate.execute {
            incidentRepository.save(newIncident(unit))
        }

        when: "a delete request is sent to the server"
        delete("/incidents/${incident.idAsString}")

        then: "the incident is deleted"
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
            'If-Match': ETag.from(incident1.idAsString, 1)
        ]
        put("/incidents/${incident2.idAsString}", [
            id: incident1.idAsString,
            name: "new name 1",
            owner: [targetUri: 'http://localhost/units/' + unit.idAsString]
        ], headers, 400)

        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }

    @WithUserDetails("user@domain.example")
    def "can put back incident"() {
        given: "a new incident"
        def id = parseJson(post("/incidents", [
            name: "new name",
            owner: [targetUri: "http://localhost/units/"+unit.idAsString]
        ])).resourceId
        def getResult = get("/incidents/$id")

        expect: "putting the retrieved incident back to be successful"
        put("/incidents/$id", parseJson(getResult), [
            "If-Match": getETag(getResult)
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
                targetUri: "http://localhost/units/${unit.idAsString}"
            ],
            parts: [
                [targetUri : "http://localhost/incidents/${incident.idAsString}"]
            ]
        ]

        def id = parseJson(post("/incidents", request)).resourceId
        def getResult = get("/incidents/$id")

        expect: "putting the retrieved incident back to be successful"
        put("/incidents/$id", parseJson(getResult), [
            "If-Match": getETag(getResult)
        ])
    }

    @WithUserDetails("user@domain.example")
    def "deleting composite element leaves parts and containers intact"() {
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
        delete("/incidents/${b.idAsString}")

        then: "b is deleted"
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
