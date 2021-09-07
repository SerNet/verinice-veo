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
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.rest.configuration.WebMvcSecurityConfiguration

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
    private Domain domain
    private Domain domain1
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)

    def setup() {
        txTemplate.execute {
            def client = clientRepository.save(newClient {
                id = clientId
            })

            domain = domainRepository.save(newDomain {
                owner = client
                description = "ISO/IEC"
                abbreviation = "ISO"
                name = "ISO"
            })

            domain1 = domainRepository.save(newDomain {
                owner = client
                description = "ISO/IEC2"
                abbreviation = "ISO"
                name = "ISO"
            })

            unit = unitRepository.save(newUnit(client) {
                name = "Test unit"
            })
        }
    }


    @WithUserDetails("user@domain.example")
    def "create a control"() {
        given: "a request body"

        Map request = [
            name: 'New Control',
            owner: [
                displayName: 'controlDataProtectionObjectivesEugdprEncryption',
                targetUri: '/units/' + unit.id.uuidValue()
            ]
        ]

        when: "a request is made to the server"
        def result = parseJson(post('/controls', request))

        then: "the location of the new control is returned"
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
                displayName: 'controlDataProtectionObjectivesEugdprEncryption',
                targetUri: '/units/' + unit.id.uuidValue()
            ], customAspects:
            [
                'control_dataProtection' :
                [
                    domains: [],
                    attributes:  [
                        control_dataProtection_objectives:[
                            'control_dataProtection_objectives_pseudonymization'
                        ]
                    ]
                ]
            ]
        ]

        when: "a request is made to the server"
        def result = parseJson(post('/controls', request))

        then: "the location of the new control is returned"
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
        savedControl.customAspects.first().type == 'control_dataProtection'
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
        def results = get("/controls/${control.id.uuidValue()}")

        then: "the eTag is set"
        getETag(results) != null
        and:
        def result = parseJson(results)
        result.name == 'Test control-1'
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
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
        def result = parseJson(get("/controls/${compositeControl.id.uuidValue()}"))

        then: "the composite control is found"
        result.name == 'Test composite control'
        result.owner.targetUri == "http://localhost/units/${unit.id.uuidValue()}"
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
        def result = parseJson(get("/controls?unit=${unit.id.uuidValue()}"))
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
        def result = parseJson(get("/controls?unit=${unit.id.uuidValue()}"))
        then: "the controls are returned"
        result.items*.name as Set == [
            'Test control-1',
            'Test composite control-1'
        ] as Set
    }

    @WithUserDetails("user@domain.example")
    def "put a control"() {
        given: "a saved control"
        def control = txTemplate.execute {
            controlRepository.save(newControl(unit) {
                domains = [domain1] as Set
            })
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
        Map headers = [
            'If-Match': ETag.from(control.id.uuidValue(), control.version)
        ]
        def result = parseJson(put("/controls/${control.id.uuidValue()}", request, headers))

        then: "the control is found"
        result.name == 'New control-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put a control with custom properties"() {
        given: "a saved control"

        def cp = newCustomAspect("my.new.type")

        def control = txTemplate.execute {
            controlRepository.save(newControl(unit) {
                customAspects = [cp] as Set
                domains = [domain1] as Set
            })
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
                'control_dataProtection' :
                [
                    domains: [],
                    attributes:  [
                        control_dataProtection_objectives:[
                            'control_dataProtection_objectives_pseudonymization'
                        ]
                    ]
                ]
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(control.id.uuidValue(), control.version)
        ]
        def result = parseJson(put("/controls/${control.id.uuidValue()}", request, headers))

        then: "the control is found"
        result.name == 'New control-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "delete a control"() {
        given: "an existing control"
        def control = txTemplate.execute {
            controlRepository.save(newControl(unit))
        }

        when: "a delete request is sent to the server"
        delete("/controls/${control.id.uuidValue()}")

        then: "the control is deleted"
        controlRepository.findById(control.id).empty
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
        Map headers = [
            'If-Match': ETag.from(control1.id.uuidValue(), 1)
        ]
        put("/controls/${control2.id.uuidValue()}", [
            id: control1.id.uuidValue(),
            name: "new name 1",
            owner: [targetUri: '/units/' + unit.id.uuidValue()]
        ], headers, false)
        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }


    @WithUserDetails("user@domain.example")
    def "can put back control"() {
        given: "a new control"
        def id = parseJson(post("/controls/", [
            name: "new name",
            owner: [targetUri: "/units/"+unit.id.uuidValue()]
        ])).resourceId
        def getResult = get("/controls/$id")

        expect: "putting the retrieved control back to be successful"
        put("/controls/$id", parseJson(getResult), [
            "If-Match": getTextBetweenQuotes(getResult.andReturn().response.getHeader("ETag"))
        ])
    }
}
