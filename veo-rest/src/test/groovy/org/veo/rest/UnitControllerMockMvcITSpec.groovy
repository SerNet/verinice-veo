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

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.adapter.presenter.api.DeviatingIdException
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.specification.MaxUnitsExceededException
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

import groovy.json.JsonSlurper

/**
 * Integration test for the unit controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class UnitControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private UnitRepositoryImpl urepository

    @Autowired
    private ClientRepositoryImpl repository

    @Autowired
    private DomainRepositoryImpl domainRepository

    @Autowired
    private TransactionTemplate txTemplate

    @Autowired
    private UserDetailsService userDetailsService

    private Client client

    private Domain domain

    def setup() {
        executeInTransaction {
            client = createTestClient()
            domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        }
    }

    @WithUserDetails("user@domain.example")
    def "create a unit for an existing client"() {

        given: "a request body"

        Map request = [
            name: 'New unit'
        ]

        when: "a request is made to the server"
        def results = post('/units', request)

        then: "the location of the new unit is returned"
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
        def result = parseJson(get("/units/${unit.id.uuidValue()}"))

        then: "the unit is returned"
        result.name == "Test unit"
        result.abbreviation == "u-1"
        result.domains.first().displayName == "DSGVO DSGVO-test"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all units"() {
        def unit = urepository.save(newUnit(client) {
            name = "Test unit foo"
            abbreviation = "u-1"
            domains = [client.domains.first()] as Set
        })

        when: "a request is made to the server"
        def result = parseJson(get("/units"))

        then: "the units are returned"
        result.size() == 1
        result.first()._self == "http://localhost/units/${unit.id.uuidValue()}"
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
                    targetUri: 'http://localhost/domains/'+client.domains.first().id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ]
        ]

        Map headers = [
            'If-Match': ETag.from(unit.id.uuidValue(), 0)
        ]

        def result = parseJson(put("/units/${unit.id.uuidValue()}", request, headers))

        then: "the unit is updated"
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
                    targetUri: 'http://localhost/domains/'+client.domains.first().id.uuidValue(),
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
                targetUri: 'http://localhost/units/'+parent.resourceId,
                displayName: 'test ddd'
            ]
        ])

        then: "the location of the new unit is returned"
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

    @WithUserDetails("manyunitscreator@domain.example")
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
        def parentUnitETag = getETag(get("/units/$parentUnitId"))
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
        delete("/units/${unit.id.uuidValue()}")

        and: "the unit is loaded again"
        loadedUnit = txTemplate.execute {
            urepository.findById(unit.id)
        }

        then: "the unit is no longer present"
        loadedUnit.empty
    }

    @WithUserDetails("user@domain.example")
    def "delete a unit containing linked elements"() {
        given: "a unit with linked asset and process"
        def unit = createTestClientUnit()

        def assetId = parseJson(post('/assets', [
            name: 'New Asset',
            domains: [
                (domain.idAsString): [
                    subType: "AST_Datatype",
                    status: "NEW"
                ]
            ],
            owner: [
                targetUri: "http://localhost/units/${unit.idAsString}"
            ]
        ])).resourceId

        Map createProcessRequest = [
            name: 'New process',
            owner: [
                displayName: 'test2',
                targetUri: 'http://localhost/units/' + unit.id.uuidValue()
            ]
        ]
        def createProcessResponse = post('/processes', createProcessRequest)
        def createProcessResult = new JsonSlurper().parseText(createProcessResponse.andReturn().response.contentAsString)

        Map putProcessRequest = [
            name: 'New Process-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: 'http://localhost/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ],
            domains: [
                (domain.id.uuidValue()): [
                    subType: "PRO_DataProcessing",
                    status: "NEW",
                ]
            ],
            links:
            [
                'process_dataType' : [
                    [
                        domains: [],
                        attributes: [
                            process_dataType_comment: 'ok',
                            process_dataType_dataOrigin: 'process_dataType_dataOrigin_direct'
                        ],
                        target:
                        [
                            targetUri: "http://localhost/assets/$assetId",
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
        delete("/units/${unit.id.uuidValue()}")

        and: "the unit is loaded again"
        loadedUnit = txTemplate.execute {
            urepository.findById(unit.id)
        }

        then: "the unit is no longer present"
        loadedUnit.empty
    }

    @WithUserDetails("user@domain.example")
    def "can put back unit"() {
        given: "a new unit"
        def id = parseJson(post("/units", [
            name: "new name"
        ])).resourceId
        def getResult = get("/units/$id")

        expect: "putting the retrieved unit back to be successful"
        put("/units/$id", parseJson(getResult), [
            "If-Match": getETag(getResult)
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
            'If-Match': getETag(get("/units/${unit1.id.uuidValue()}"))
        ]
        put("/units/${unit2.id.uuidValue()}", [
            id: unit1.id.uuidValue(),
            name: "new name 1"
        ], headers, 400)
        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }

    @WithUserDetails("user@domain.example")
    def "cannot create more than 2 units"() {
        when: "the user tries to create a unit"
        def result = parseJson(post('/units', [name: 'Unit 1']))
        then: "the request is successful"
        result.success
        when: "the user tries to create another unit"
        result = parseJson(post('/units', [name: 'Unit 2']))
        then: "the request is successful"
        result.success
        when: "the user tries to create a third unit"
        post('/units', [name: 'Unit 3'], HttpStatus.SC_FORBIDDEN)
        then: "the action is not performed"
        thrown(MaxUnitsExceededException)
        and: 'only 2 units have been created'
        parseJson(get("/units")).size() == 2
    }

    @WithUserDetails("manyunitscreator@domain.example")
    def "cannot bypass maxUnits limit with parallel requests"() {
        given:
        int numRequests = 80
        int allowedUnitsForUser = 50
        def pool = Executors.newFixedThreadPool(8)
        AtomicInteger failedAttempts = new AtomicInteger()
        AtomicInteger successfulAttempts = new AtomicInteger()
        def tasks = (1..numRequests).collect { n-> {
                ->
                try {
                    UserDetails principal = userDetailsService.loadUserByUsername('manyunitscreator@domain.example')
                    Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(principal,
                            principal.getPassword(), principal.getAuthorities())
                    SecurityContext context = SecurityContextHolder.createEmptyContext()
                    context.setAuthentication(authentication)
                    TestSecurityContextHolder.setContext(context)
                    post('/units', [name: "Unit ${n} "])
                    successfulAttempts.incrementAndGet()
                } catch(Throwable t) {
                    failedAttempts.incrementAndGet()
                }
            } as Callable
        }
        when: "the user tries to create a lot of units"
        pool.invokeAll(tasks)
        pool.shutdown()
        then: 'all the requests are performed'
        pool.awaitTermination(30, TimeUnit.SECONDS)
        and: 'only the allowed number of units have been created'
        parseJson(get("/units")).size() == allowedUnitsForUser
        successfulAttempts.get() == allowedUnitsForUser
        and: 'the other creation attempts failed'
        failedAttempts.get() == numRequests - allowedUnitsForUser
    }
}
