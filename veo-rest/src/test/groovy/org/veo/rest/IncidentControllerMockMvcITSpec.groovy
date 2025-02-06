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

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.Unit
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
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.INCIDENT, it) {
                    subTypes = [
                        NormalIncident: newSubTypeDefinition()
                    ]
                })
            }

            domain1 = newDomain(client) {
                abbreviation = "D1"
                name = "Domain 1"
            }

            client = clientRepository.save(client)

            unit = newUnit(client) {
                name = "Test unit"
            }

            unitRepository.save(unit)
        }
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
