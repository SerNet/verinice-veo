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
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.custom.SimpleProperties
import org.veo.core.entity.groups.ControlGroup
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
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
class ControlControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private ControlRepositoryImpl controlRepository

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

            def client = newClient {
                id = clientId
                domains = [domain, domain1] as Set
            }
            unit = newUnit client, {
                name = "Test unit"
            }
            client.units << unit
            unit.client = client

            clientRepository.save(client)
        }
    }


    @WithUserDetails("user@domain.example")
    def "create a control"() {
        given: "a request body"

        Map request = [
            name: 'New Control',
            owner: [
                displayName: 'test2',
                href: '/units/' + unit.id.uuidValue()
            ]
        ]

        when: "a request is made to the server"

        def results = post('/controls', request)

        then: "the control is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new control is returned"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Control created successfully.'
    }


    @WithUserDetails("user@domain.example")
    def "retrieve a control"() {
        given: "a saved control"

        def control = newControl  unit, {
            name = 'Test control-1'
        }
        control = txTemplate.execute {
            controlRepository.save(control)
        }


        when: "a request is made to the server"
        def results = get("/controls/${control.id.uuidValue()}")

        then: "the control is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test control-1'
        result.owner.href == "/units/"+unit.id.uuidValue()
    }


    @WithUserDetails("user@domain.example")
    def "retrieve all controls for a unit"() {
        given: "a saved asset"

        def control = newControl  unit, {
            name = 'Test control-1'
        }

        def control2 = newControl  unit, {
            name = 'Test control-2'
        }

        (control, control2) = txTemplate.execute {
            [control, control2].collect(controlRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/controls?parent=${unit.id.uuidValue()}")

        then: "the controls are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2

        result.sort{it.name}.first().name == 'Test control-1'
        result.sort{it.name}.first().owner.href == "/units/"+unit.id.uuidValue()
        result.sort{it.name}[1].name == 'Test control-2'
        result.sort{it.name}[1].owner.href == "/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieveíng all controls for a unit does not return groups"() {
        given: "a saved control and a saved control group"

        def control = newControl  unit, {
            name = 'Test control-1'
        }

        def controlGroup = new ControlGroup().tap{
            name = 'Group 1'
            owner = unit
        }

        txTemplate.execute {
            [control, controlGroup].collect(controlRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/controls?parent=${unit.id.uuidValue()}")

        then: "the controls are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 1
        result.first().name == 'Test control-1'
    }


    @WithUserDetails("user@domain.example")
    def "put a control"() {
        given: "a saved control"

        Key<UUID> id = Key.newUuid()
        def control = newControl unit, {
            it.id = id
            setDomains([domain1] as Set)
        }

        control = txTemplate.execute {
            controlRepository.save(control)
        }

        Map request = [
            id: id.uuidValue(),
            name: 'New control-2',
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
        def results = put("/controls/${control.id.uuidValue()}", request)

        then: "the control is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New control-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.href == "/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put a control with custom properties"() {
        given: "a saved control"

        CustomProperties cp = new SimpleProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Control'] as Set)
        cp.setId(Key.newUuid())
        Key<UUID> id = Key.newUuid()
        def control = newControl unit, {
            it.id = id
            setCustomAspects([cp] as Set)
            setDomains([domain1] as Set)
        }

        control = txTemplate.execute {
            controlRepository.save(control)
        }
        Map request = [
            id: id.uuidValue(),
            name: 'New control-2',
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
                        "Control"
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
        def results = put("/controls/${control.id.uuidValue()}", request)

        then: "the control is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New control-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.href == "/units/"+unit.id.uuidValue()
    }


    @WithUserDetails("user@domain.example")
    def "delete a control"() {

        given: "an existing control"
        Key<UUID> id = Key.newUuid()
        def control = newControl  unit, {
            name = 'Test control-delete'
            domains = [domain1] as Set
        }

        control = txTemplate.execute {
            controlRepository.save(control)
        }


        when: "a delete request is sent to the server"

        def results = delete("/controls/${control.id.uuidValue()}")

        then: "the control is deleted"
        results.andExpect(status().isOk())
        controlRepository.findById(id).empty
    }
}