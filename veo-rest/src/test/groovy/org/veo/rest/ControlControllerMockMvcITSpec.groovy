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

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Integration test for the unit controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class ControlControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private ControlRepositoryImpl controlRepository

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
            unit = unitRepository.save(newUnit(client) {
                name = "Test unit"
            })
        }
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a control"() {
        given: "a saved control"
        def control = txTemplate.execute {
            controlRepository.save(newControl(unit) {
                name = 'Test control-1'
            })
        }

        when: "a request is made to the server"
        def results = get("/controls/${control.idAsString}")

        then: "the eTag is set"
        getETag(results) != null

        and:
        def result = parseJson(results)
        result._self == "http://localhost/controls/${control.idAsString}"
        result.name == 'Test control-1'
        result.owner.targetUri == "http://localhost/units/"+unit.idAsString
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a composite control with parts"() {
        given: "a saved composite control with two parts"
        Control c1 = newControl(unit) {
            name = "c1"
            designator = 'CTL-1'
        }
        Control c2 = newControl(unit) {
            name = "c2"
            designator = 'CTL-2'
        }

        def compositeControl = txTemplate.execute {
            controlRepository.save(newControl(unit) {
                name = 'Test composite control'
                parts = [c1, c2]
            })
        }

        when: "the server is queried for the composite control"
        def result = parseJson(get("/controls/${compositeControl.idAsString}"))

        then: "the composite control is found"
        result.name == 'Test composite control'
        result.owner.targetUri == "http://localhost/units/${unit.idAsString}"
        result.parts.size() == 2
        result.parts*.displayName as Set == [
            'CTL-1 c1',
            'CTL-2 c2'
        ] as Set
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all controls for a unit"() {
        given: "a saved asset"
        def control = newControl(unit) {
            name = 'Test control-1'
        }
        def control2 = newControl(unit) {
            name = 'Test control-2'
        }
        (control, control2) = txTemplate.execute {
            [control, control2].collect(controlRepository.&save)
        }

        when: "a request is made to the server"
        def result = parseJson(get("/controls?unit=${unit.idAsString}"))

        then: "the controls are returned"
        result.items*.name.sort() == [
            'Test control-1',
            'Test control-2'
        ]
    }

    @WithUserDetails("user@domain.example")
    def "retrieving all controls for a unit returns composite elements and their parts"() {
        given: "a saved control  and a composite document containing it"
        txTemplate.execute {
            controlRepository.save(newControl(unit) {
                name = 'Test composite control-1'
                parts <<  newControl(unit) {
                    name = 'Test control-1'
                }
            })
        }

        when: "a request is made to the server"
        def result = parseJson(get("/controls?unit=${unit.idAsString}"))

        then: "the controls are returned"
        result.items*.name as Set == [
            'Test control-1',
            'Test composite control-1'
        ] as Set
    }

    @WithUserDetails("user@domain.example")
    def "delete a control"() {
        given: "an existing control"
        def control = txTemplate.execute {
            controlRepository.save(newControl(unit))
        }

        when: "a delete request is sent to the server"
        delete("/controls/${control.idAsString}")

        then: "the control is deleted"
        controlRepository.findById(control.id).empty
    }
}
