/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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

import static org.hamcrest.Matchers.emptyOrNullString
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.not
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.adapter.presenter.api.DeviatingIdException
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
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
class UnitControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private UnitRepositoryImpl urepository

    @Autowired
    private ClientRepositoryImpl repository

    @Autowired
    private DomainRepositoryImpl domainRepository

    @Autowired
    private TransactionTemplate txTemplate

    private Client client

    private Domain domain

    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)

    def setup() {
        client = repository.save(newClient {
            id = clientId
        })
        domain = domainRepository.save(newDomain {
            owner = this.client
            description = "ISO/IEC"
            abbreviation = "ISO"
            name = "27001"
        })
        client.addToDomains(domain)
    }

    @WithUserDetails("user@domain.example")
    def "create a unit for an existing client"() {

        given: "a request body"

        Map request = [
            name: 'New unit'
        ]

        when: "a request is made to the server"

        def results = post('/units', request)

        then: "the unit is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new unit is returned"
        results.andExpect(jsonPath('$.success').value("true"))
        results.andExpect(jsonPath('$.resourceId', is(not(emptyOrNullString()))))
        results.andExpect(jsonPath('$.message').value('Unit created successfully.'))
    }

    @WithUserDetails("user@domain.example")
    def "get a unit"() {
        def unit = urepository.save(newUnit(client) {
            name = "Test unit"
            abbreviation = "u-1"
            domains = [client.domains.first()] as Set
        })

        when: "a request is made to the server"

        def results = get("/units/${unit.id.uuidValue()}")

        then: "the unit is returned with HTTP status code 200"

        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == "Test unit"
        result.abbreviation == "u-1"
        result.domains.first().displayName == "ISO 27001"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all units"() {
        def unit = urepository.save(newUnit(client) {
            name = "Test unit foo"
            abbreviation = "u-1"
            domains = [client.domains.first()] as Set
        })

        when: "a request is made to the server"

        def results = get("/units")

        then: "the units are returned"

        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.size() == 1
        result.first().name == "Test unit foo"
    }

    def createTestClientUnit() {
        return txTemplate.execute {
            urepository.save(newUnit(client))
        }
    }

    @WithUserDetails("user@domain.example")
    def "update a unit"() {
        given: "a unit"
        def unit = createTestClientUnit()

        when: "the unit is updated by changing the name and adding a domain"

        Map request = [
            id: unit.id.uuidValue(),
            name: 'New unit-2',
            abbreviation: 'u-2',
            description: 'desc',
            domains: [
                [
                    targetUri: '/domains/'+client.domains.first().id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ]
        ]

        Map headers = [
            'If-Match': ETag.from(unit.id.uuidValue(), 0)
        ]

        def results = put("/units/${unit.id.uuidValue()}", request, headers)

        then: "the unit is updated and a status code returned"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == "New unit-2"
        result.abbreviation == "u-2"
    }

    @WithUserDetails("user@domain.example")
    def "Update a unit multiple times"() {

        given: "a unit"
        Unit unit = createTestClientUnit()

        when: "the unit is updated first by changing the name and adding a domain"
        Map request1 = [
            id: unit.id.uuidValue(),
            name: 'New unit-2',
            abbreviation: 'u-2',
            description: 'desc',
            domains: [
                [
                    targetUri: '/domains/'+client.domains.first().id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ]
        ]

        Map headers = [
            'If-Match': ETag.from(unit.id.uuidValue(), 0)
        ]

        def request1Result = parseJson(put("/units/${unit.id.uuidValue()}", request1, headers))

        then: "the unit was correctly modified the first time"
        request1Result.name == "New unit-2"
        request1Result.abbreviation == "u-2"
        request1Result.domains.size() == 1

        when: "the unit is updated secondly by changing the name and removing a domain"
        Map request2 = [
            id: unit.id.uuidValue(),
            name: 'New unit-3',
            abbreviation: 'u-3',
            description: 'desc',
            domains: []]

        headers = [
            'If-Match': ETag.from(unit.id.uuidValue(), 1)
        ]

        def request2Result = parseJson(put("/units/${unit.id.uuidValue()}", request2, headers))

        then: "the unit was correctly modified the second time"
        request2Result.name == "New unit-3"
        request2Result.abbreviation == "u-3"
        request2Result.domains.size() == 0
    }

    @WithUserDetails("user@domain.example")
    def "create sub unit for a unit"() {
        given: "a parent unit"
        def parent = parseJson(post('/units', [
            name: 'parent-unit-1'
        ]))

        when: "a sub unit is POSTed"
        def postSubUnitResult = post('/units', [
            name: 'sub-unit-1',
            parent: [
                targetUri: '/units/'+parent.resourceId,
                displayName: 'test ddd'
            ]
        ])

        then: "the sub unit is created and a status code returned"
        postSubUnitResult.andExpect(status().isCreated())

        and: "the location of the new unit is returned"
        postSubUnitResult.andExpect(jsonPath('$.success').value("true"))
        postSubUnitResult.andExpect(jsonPath('$.resourceId', is(not(emptyOrNullString()))))
        postSubUnitResult.andExpect(jsonPath('$.message').value('Unit created successfully.'))

        when: "get the sub unit"
        def subUnitId = parseJson(postSubUnitResult).resourceId
        def subUnit = parseJson(get("/units/${subUnitId}"))

        then: "the sub unit has the right parent"
        subUnit.name == "sub-unit-1"
        subUnit.parent.targetUri == "http://localhost/units/"+parent.resourceId

        and: "the retrieved parent contains the sub unit"
        with(parseJson(get("/units/$parent.resourceId")).units) {
            size() == 1
            get(0).targetUri == "http://localhost/units/$subUnitId"
        }

        when: "load the client"
        def allUnits = txTemplate.execute {
            urepository.findByClient(client)
        }

        then: "the data is persistent"
        allUnits.size() == 2
    }

    @WithUserDetails("user@domain.example")
    def "sub units are read-only when creating a unit"() {
        given: "a parent and sub unit"
        def parentUnitId = parseJson(post("/units", [
            name: "parent"
        ])).resourceId
        def subUnitId = parseJson(post("/units", [
            name: "sub",
            parent: [
                targetUri: "/units/$parentUnitId"
            ]
        ])).resourceId

        when: "trying to create a new parent for the existing sub unit"
        def newUnitId = parseJson(post("/units", [
            name: "new unit",
            units: [
                [
                    targetUri: "/units/$subUnitId"
                ]
            ]
        ])).resourceId

        then: "the sub unit list was ignored"
        parseJson(get("/units/$newUnitId")).units.size() == 0

        and: "the sub unit is still assigned to the original parent"
        parseJson(get("/units/$subUnitId")).parent.targetUri == "http://localhost/units/$parentUnitId"
        parseJson(get("/units/$parentUnitId")).units[0].targetUri == "http://localhost/units/$subUnitId"
    }

    @WithUserDetails("user@domain.example")
    def "sub units are read-only when updating a unit"() {
        given: "a parent and sub unit"
        def parentUnitId = parseJson(post("/units", [
            name: "parent"
        ])).resourceId
        def subUnitId = parseJson(post("/units", [
            name: "sub",
            parent: [
                targetUri: "/units/$parentUnitId"
            ]
        ])).resourceId

        when: "trying to update the parent with an empty sub unit list"
        def parentUnitETag = parseETag(get("/units/$parentUnitId"))
        put("/units/$parentUnitId", [
            id: parentUnitId,
            name: "parent",
            units: []], ["If-Match": parentUnitETag])

        then: "the unit hierarchy is still the same"
        parseJson(get("/units/$subUnitId")).parent.targetUri == "http://localhost/units/$parentUnitId"
        parseJson(get("/units/$parentUnitId")).units[0].targetUri == "http://localhost/units/$subUnitId"
    }

    @WithUserDetails("user@domain.example")
    def "delete a unit"() {
        given: "a unit"
        def unit = createTestClientUnit()

        when: "the unit is loaded"
        def loadedUnit = txTemplate.execute {
            urepository.findById(unit.id)
        }

        then: "the loadedUnit is present"
        loadedUnit.present

        when: "the unit is deleted"
        def results = delete("/units/${unit.id.uuidValue()}")

        then: "the unit is removed and a status code returned"
        results.andExpect(status().isNoContent())

        when: "the unit is loaded again"
        loadedUnit = txTemplate.execute {
            urepository.findById(unit.id)
        }

        then: "the unit is no longer present"
        loadedUnit.empty
    }

    @WithUserDetails("user@domain.example")
    def "delete a unit containing linked entities"() {
        given: "a unit with linked asset and process"
        def unit = createTestClientUnit()

        Map createAssetRequest = [
            name: 'New Asset',
            owner: [
                displayName: 'test2',
                targetUri: '/units/' + unit.id.uuidValue()
            ]
        ]
        def creatAssetResponse = post('/assets', createAssetRequest)
        def createAssetResult = new JsonSlurper().parseText(creatAssetResponse.andReturn().response.contentAsString)

        Map createProcessRequest = [
            name: 'New process',
            owner: [
                displayName: 'test2',
                targetUri: '/units/' + unit.id.uuidValue()
            ]
        ]
        def createProcessResponse = post('/processes', createProcessRequest)
        def createProcessResult = new JsonSlurper().parseText(createProcessResponse.andReturn().response.contentAsString)
        def processId = createProcessResult.resourceId

        Map putProcessRequest = [
            name: 'New Process-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ]
            ,
            domains: [
                [
                    targetUri: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ],
            links:
            [
                'process_dataType' : [
                    [
                        applicableTo: [
                            "Process"
                        ],
                        name:'test link prcess->asset',
                        domains: [],
                        attributes: [
                            process_dataType_dataOrigin: 'process_dataType_dataOrigin_direct',
                            process_dataType_transferPurpose: 'ok'
                        ],
                        target:
                        [
                            targetUri: '/assets/'+createAssetResult.resourceId,
                            displayName: 'test ddd'
                        ]
                    ]
                ]
            ]
        ]

        Map headers = [
            'If-Match': ETag.from(createProcessResult.resourceId, 0)
        ]
        put("/processes/${createProcessResult.resourceId}", putProcessRequest, headers)


        when: "the unit is loaded"
        def loadedUnit = txTemplate.execute {
            urepository.findById(unit.id)
        }

        then: "the loadedUnit is present"
        loadedUnit.present

        when: "the unit is deleted"
        def results = delete("/units/${unit.id.uuidValue()}")

        then: "the unit is removed and a status code returned"
        results.andExpect(status().isNoContent())

        when: "the unit is loaded again"
        loadedUnit = txTemplate.execute {
            urepository.findById(unit.id)
        }

        then: "the unit is no longer present"
        loadedUnit.empty
    }


    @WithUserDetails("user@domain.example")
    def "can put back unit"() {
        given: "a new unit"
        def id = parseJson(post("/units/", [
            name: "new name"
        ])).resourceId
        def getResult = get("/units/$id")

        expect: "putting the retrieved unit back to be successful"
        put("/units/$id", parseJson(getResult), [
            "If-Match": getTextBetweenQuotes(getResult.andReturn().response.getHeader("ETag"))
        ])
    }

    @WithUserDetails("user@domain.example")
    def "can't put a unit with another unit's ID"() {
        given: "two units"
        def unit1 = txTemplate.execute({
            unitDataRepository.save(newUnit(client, {
                name = "old name 1"
            }))
        })
        def unit2 = txTemplate.execute({
            unitDataRepository.save(newUnit(client, {
                name = "old name 2"
            }))
        })
        when: "a put request tries to update unit 1 using the ID of unit 2"
        Map headers = [
            'If-Match': parseETag(get("/units/${unit1.id.uuidValue()}"))
        ]
        put("/units/${unit2.id.uuidValue()}", [
            id: unit1.id.uuidValue(),
            name: "new name 1"
        ], headers, false)
        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }
}
