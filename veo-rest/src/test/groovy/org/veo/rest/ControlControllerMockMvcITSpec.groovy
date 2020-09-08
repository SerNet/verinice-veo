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
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Domain
import org.veo.core.entity.GroupType
import org.veo.core.entity.Key
import org.veo.core.entity.ModelGroup
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ControlData
import org.veo.persistence.entity.jpa.CustomPropertiesData
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.rest.configuration.WebMvcSecurityConfiguration

import groovy.json.JsonSlurper

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
class ControlControllerMockMvcITSpec extends VeoRestMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private ControlRepositoryImpl controlRepository

    @Autowired
    TransactionTemplate txTemplate
    @Autowired
    private EntityDataFactory entityFactory

    private Unit unit
    private Domain domain
    private Domain domain1
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)

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
    }


    @WithUserDetails("user@domain.example")
    def "create a control"() {
        given: "a request body"

        Map request = [
            name: 'New Control',
            owner: [
                displayName: 'test2',
                targetUri: '/units/' + unit.id.uuidValue()
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
    def "create a control with custom properties"() {
        given: "a request body"

        Map request = [
            name: 'New Control',
            owner: [
                displayName: 'test2',
                targetUri: '/units/' + unit.id.uuidValue()
            ], customAspects:
            [
                'my.aspect-test' :
                [
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

        when:
        Control savedControl = txTemplate.execute {
            controlRepository.findById(Key.uuidFrom(resourceId)).get().tap() {
                // resolve proxy:
                customAspects.first()
            }
        }

        then: 'the custom properties are saved'
        savedControl.customAspects.first().type == 'my.aspect-test1'
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a control"() {
        given: "a saved control"

        def control = entityFactory.createControl()
        control.name = 'Test control-1'
        control.owner = unit
        control.id = Key.newUuid()

        control = txTemplate.execute {
            controlRepository.save(control)
        }


        when: "a request is made to the server"
        def results = get("/controls/${control.id.uuidValue()}")

        then: "the control is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test control-1'
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all controls for a unit"() {
        given: "a saved asset"

        def control = entityFactory.createControl()
        control.id = Key.newUuid()
        control.name = 'Test control-1'
        control.owner = unit

        def control2 = entityFactory.createControl()
        control2.id = Key.newUuid()
        control2.name = 'Test control-2'
        control2.owner = unit

        (control, control2) = txTemplate.execute {
            [control, control2].collect(controlRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/controls?unit=${unit.id.uuidValue()}")

        then: "the controls are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2

        result.sort{it.name}.first().name == 'Test control-1'
        result.sort{it.name}.first().owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
        result.sort{it.name}[1].name == 'Test control-2'
        result.sort{it.name}[1].owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieveíng all controls for a unit does not return groups"() {
        given: "a saved control and a saved control group"

        def control = entityFactory.createControl(Key.newUuid(), 'Test control-1', unit)

        ModelGroup controlGroup = entityFactory.createGroup(GroupType.Control)
        controlGroup.id= Key.newUuid()
        controlGroup.name = 'Group 1'
        controlGroup.owner = unit

        txTemplate.execute {
            [control, controlGroup].collect(controlRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/controls?unit=${unit.id.uuidValue()}")

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
        def control = entityFactory.createControl()
        control.id = Key.newUuid()
        control.name = 'Test control-1'
        control.owner = unit
        control.domains = [domain1] as Set

        control = txTemplate.execute {
            controlRepository.save(control)
        }

        Map request = [
            name: 'New control-2',
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
        def results = put("/controls/${control.id.uuidValue()}", request)

        then: "the control is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New control-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put a control with custom properties"() {
        given: "a saved control"

        CustomProperties cp = entityFactory.createCustomProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Control'] as Set)

        Key<UUID> id = Key.newUuid()
        def control = entityFactory.createControl()
        control.id = id
        control.name ="C"
        control.setCustomAspects([cp] as Set)
        control.setDomains([domain1] as Set)
        control.owner = unit


        control = txTemplate.execute {
            controlRepository.save(control)
        }
        Map request = [
            name: 'New control-2',
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
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put a control with a string property that is too long"() {
        given: "a saved control"

        CustomProperties cp = new CustomPropertiesData()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Control'] as Set)

        Key<UUID> id = Key.newUuid()
        def control = new ControlData()
        control.id = id
        control.setCustomAspects([cp] as Set)
        control.setDomains([domain1] as Set)
        control.owner = unit
        control.name = "c-1"

        control = txTemplate.execute {
            controlRepository.save(control)
        }
        Map request = [
            name: 'New control-2',
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
                        "Control"
                    ],
                    domains: [],
                    attributes:  [
                        test: 'X' * 20000
                    ]
                ]
            ]
        ]

        when: "a request is made to the server"
        def results = put("/controls/${control.id.uuidValue()}", request, false)

        then: "the data is rejected"
        HttpMessageNotReadableException ex = thrown()

        and: "the reason is given"
        ex.message =~ /Property value for test exceeds maximum length of 18.000 characters./
    }

    @WithUserDetails("user@domain.example")
    def "delete a control"() {

        given: "an existing control"
        Key<UUID> id = Key.newUuid()

        def control = entityFactory.createControl()
        control.name = 'Test control-delete'
        control.domains = [domain1] as Set
        control.name ="C"
        control.owner = unit
        control.id = id

        control = txTemplate.execute {
            controlRepository.save(control)
        }


        when: "a delete request is sent to the server"

        def results = delete("/controls/${control.id.uuidValue()}")

        then: "the control is deleted"
        results.andExpect(status().isOk())
        controlRepository.findById(id).empty
    }

    @WithUserDetails("user@domain.example")
    def "can't put a control with another control's ID"() {
        given: "two controls"
        def control1 = txTemplate.execute({
            controlRepository.save(newControl(unit, {
                name = "old name 1"
            }))
        })
        def control2 = txTemplate.execute({
            controlRepository.save(newControl(unit, {
                name = "old name 2"
            }))
        })
        when: "a put request tries to update control 1 using the ID of control 2"
        put("/controls/${control2.id.uuidValue()}", [
            id: control1.id.uuidValue(),
            name: "new name 1",
            owner: [targetUri: '/units/' + unit.id.uuidValue()]
        ], false)
        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }
}