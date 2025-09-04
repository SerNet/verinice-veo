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
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.MethodArgumentNotValidException

import org.veo.adapter.presenter.api.DeviatingIdException
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.exception.ReferenceTargetNotFoundException
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.specification.MaxUnitsExceededException
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.rest.common.ClientNotActiveException

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
    private ScopeRepositoryImpl sRepository

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
    def "Import a unit for an existing client"() {
        given:
        def unitId = UUID.randomUUID().toString()
        def compositeId = UUID.randomUUID().toString()
        def partId = UUID.randomUUID().toString()
        def controlId = UUID.randomUUID().toString()
        def personId = UUID.randomUUID().toString()
        def domain = parseJson(get("/domains/${domain.idAsString}"))
        Map request = [
            unit:[
                name: 'New unit',
                id: unitId
            ],
            elements: [
                [
                    id: controlId,
                    name: 'Control',
                    owner: [targetUri:"/units/$unitId"],
                    type: 'control'
                ],
                [
                    id: personId,
                    name: 'Person',
                    owner: [targetUri:"/units/$unitId"],
                    type: 'person'
                ],
                [
                    id: compositeId,
                    name: 'Composite',
                    owner: [targetUri:"/units/$unitId"],
                    type: 'asset',
                    parts:[
                        [targetUri:"/assets/$partId"]
                    ]
                ],
                [
                    id: partId,
                    name: 'Part',
                    owner: [targetUri:"/units/$unitId"],
                    type: 'asset',
                    controlImplementations : [
                        [
                            control: [targetUri:"/controls/$controlId"],
                            responsible: [targetUri:"/persons/$personId"],
                            description : 'All done'
                        ]
                    ]
                ]
            ],
            domains: [
                domain
            ],
            risks: []
        ]

        when:
        def result = post('/units/import', request)

        then:
        result != null

        when:
        def savedPart = executeInTransaction {
            assetDataRepository.findAll().find{it.name == 'Part'}.tap {
                //initialize lazy associations
                it.controlImplementations.each {
                    it.control.name
                    it.responsible.name
                }
            }
        }

        then:
        savedPart != null
        savedPart.controlImplementations.size() == 1
        with(savedPart.controlImplementations.first()) {
            control.name == 'Control'
            responsible.name == 'Person'
        }
    }

    @WithUserDetails("user@domain.example")
    def "Sensible error is returned for structurally broken CI"() {
        given:
        def unitId = UUID.randomUUID().toString()
        def assetId = UUID.randomUUID().toString()
        def controlId = UUID.randomUUID().toString()
        def domain = parseJson(get("/domains/${domain.idAsString}"))
        Map request = [
            unit:[
                name: 'New unit',
                id: unitId
            ],
            elements: [
                [
                    id: controlId,
                    name: 'Control',
                    owner: [targetUri:"/units/$unitId"],
                    type: 'control'
                ],
                [
                    id: assetId,
                    name: 'Asset',
                    owner: [targetUri:"/units/$unitId"],
                    type: 'asset',
                    controlImplementations : [
                        [ : ]
                    ]
                ]
            ],
            domains: [
                domain
            ],
            risks: []
        ]

        when:
        def result = post('/units/import', request, 400)

        then:
        MethodArgumentNotValidException e = thrown()
        e.message.contains('elements[].controlImplementations[].control')
    }

    @WithUserDetails("user@domain.example")
    def "get a unit"() {
        def unit = urepository.save(newUnit(client) {
            name = "Test unit"
            abbreviation = "u-1"
            domains = [client.domains.first()] as Set
        })

        when: "a request is made to the server"
        def result = parseJson(get("/units/${unit.idAsString}"))

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
        result.first()._self == "http://localhost/units/${unit.idAsString}"
        result.first().name == "Test unit foo"
    }

    def createTestClientUnit() {
        return txTemplate.execute {
            urepository.save(newUnit(client))
        }
    }

    @WithUserDetails("user@domain.example")
    def "Requesting units for a non-existing client leads to an error"() {
        given: "no client"
        deleteTestClient()

        when: "a request is made to the server"
        get("/units", 403)

        then: "an exception is thrown"
        thrown(ClientNotActiveException)
    }

    @WithUserDetails("user@domain.example")
    def "update a unit"() {
        given: "a unit"
        def unit = createTestClientUnit()

        when: "the unit is updated by changing the name and adding a domain"
        Map request = [
            id          : unit.idAsString,
            name        : 'New unit-2',
            abbreviation: 'u-2',
            description : 'desc',
            domains     : [
                [
                    targetUri  : 'http://localhost/domains/' + client.domains.first().idAsString,
                    displayName: 'test ddd'
                ]
            ]
        ]

        Map headers = [
            'If-Match': ETag.from(unit.idAsString, 0)
        ]

        def result = parseJson(put("/units/${unit.idAsString}", request, headers))

        then: "the unit is updated"
        result.name == "New unit-2"
        result.abbreviation == "u-2"
        result.domains.size() == 1
    }

    @WithUserDetails("user@domain.example")
    def "Update a unit multiple times"() {
        given: "a unit"
        Unit unit = createTestClientUnit()

        when: "the unit is updated first by changing the name and adding a domain"
        Map request1 = [
            id: unit.idAsString,
            name: 'New unit-2',
            abbreviation: 'u-2',
            description: 'desc',
            domains: [
                [
                    targetUri: 'http://localhost/domains/'+client.domains.first().idAsString,
                    displayName: 'test ddd'
                ]
            ]
        ]

        Map headers = [
            'If-Match': ETag.from(unit.idAsString, 0)
        ]

        def request1Result = parseJson(put("/units/${unit.idAsString}", request1, headers))

        then: "the unit was correctly modified the first time"
        request1Result.name == "New unit-2"
        request1Result.abbreviation == "u-2"
        request1Result.domains.size() == 1

        when: "the unit is updated secondly by changing the name and removing a domain"
        Map request2 = [
            id: unit.idAsString,
            name: 'New unit-3',
            abbreviation: 'u-3',
            description: 'desc',
            domains: []]

        headers = [
            'If-Match': ETag.from(unit.idAsString, 1)
        ]

        def request2Result = parseJson(put("/units/${unit.idAsString}", request2, headers))

        then: "the unit was correctly modified the second time"
        request2Result.name == "New unit-3"
        request2Result.abbreviation == "u-3"
        request2Result.domains.size() == 0
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
        delete("/units/${unit.idAsString}")

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

        def assetId = parseJson(post("/domains/$domain.idAsString/assets", [
            name: 'New Asset',
            subType: "AST_Datatype",
            status: "NEW",
            owner: [
                targetUri: "http://localhost/units/${unit.idAsString}"
            ]
        ])).resourceId

        Map createProcessRequest = [
            name: 'New process',
            subType: "PRO_DataProcessing",
            status: "NEW",
            owner: [
                displayName: 'test2',
                targetUri: 'http://localhost/units/' + unit.idAsString
            ]
        ]
        def createProcessResponse = post("/domains/$domain.idAsString/processes", createProcessRequest)
        def createProcessResult = new JsonSlurper().parseText(createProcessResponse.andReturn().response.contentAsString)

        Map putProcessRequest = [
            name: 'New Process-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: 'http://localhost/units/'+unit.idAsString,
                displayName: 'test unit'
            ],
            subType: "PRO_DataProcessing",
            status: "NEW",
            links:
            [
                'process_dataType' : [
                    [
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
        put("/domains/$domain.idAsString/processes/${createProcessResult.resourceId}", putProcessRequest, headers)

        when: "the unit is loaded"
        def loadedUnit = txTemplate.execute {
            urepository.findById(unit.id)
        }

        then: "the loadedUnit is present"
        loadedUnit.present

        when: "the unit is deleted"
        delete("/units/${unit.idAsString}")

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
            'If-Match': getETag(get("/units/${unit1.idAsString}"))
        ]
        put("/units/${unit2.idAsString}", [
            id: unit1.idAsString,
            name: "new name 1"
        ], headers, 400)

        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }

    @WithUserDetails("user@domain.example")
    def "cannot create more than 2 units"() {
        when: "the user tries to create a unit"
        def result = parseJson(post('/units', [name: 'Unit 1']))
        def unitId = result.resourceId

        then: "the request is successful"
        result.success

        when: "we import a unit"
        def unitBackup = parseJson(get("/units/$unitId/export"))
        result = parseJson(post("/units/import", unitBackup))

        then: "the request is successful"
        result.success

        when: "the user tries to create a third unit"
        post('/units', [name: 'Unit 3'], HttpStatus.SC_FORBIDDEN)

        then: "the action is not performed"
        thrown(MaxUnitsExceededException)

        and: 'only 2 units have been created'
        parseJson(get("/units")).size() == 2

        when: "we import another unit"
        post("/units/import", unitBackup,  HttpStatus.SC_FORBIDDEN)

        then:"the action is not performed"
        thrown(MaxUnitsExceededException)
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

    @WithUserDetails("user@domain.example")
    def "export a unit"() {
        given:
        def unit = urepository.save(newUnit(client) {
            name = "My unit"
            addToDomains(domain)
        })
        sRepository.save(newScope(unit) {
            name = 'My scope'
        })

        when:
        def result = parseJson(get("/units/${unit.idAsString}/export"))

        then:
        with(result.unit) {
            name == "My unit"
        }
        with(result.domains) {
            size() == 1
            first().name == 'DSGVO-test'
        }
        with(result.elements) {
            size() == 1
            with(first()) {
                name == 'My scope'
                it.owner.containsKey('targetUri')
            }
        }
    }

    @WithUserDetails("user@domain.example")
    def "cannot export a unit from another client"() {
        given:
        def otherClient = repository.save(newClient())
        def otherClientsUnit = urepository.save(newUnit(otherClient))

        when:
        get("/units/${otherClientsUnit.idAsString}/export", 404)

        then: "an exception is thrown"
        thrown(NotFoundException)
    }

    @WithUserDetails("user@domain.example")
    def "cannot create a unit with an invalid domain"() {
        given:
        def randomUuid = UUID.randomUUID()
        Map request = [
            name: 'New unit',
            domains: [
                [targetUri:"/domains/$randomUuid"]
            ]
        ]

        when:
        post('/units', request, 422)

        then:
        thrown(ReferenceTargetNotFoundException)
    }

    @WithUserDetails("user@domain.example")
    def "cannot create a unit with another client's domain"() {
        given:
        def otherClient = repository.save(newClient())
        def otherClientsDomain = domainRepository.save(newDomain(otherClient))

        Map request = [
            name: 'New unit',
            domains: [
                [targetUri:"/domains/${otherClientsDomain.idAsString}"]
            ]
        ]

        when:
        post('/units', request, 422)

        then:
        thrown(ReferenceTargetNotFoundException)
    }

    @WithUserDetails("user@domain.example")
    def "cannot create a unit with an invalid domain id"() {
        given:
        Map request = [
            name: 'New unit',
            domains: [
                [targetUri:"/domains/NO-UUID"]
            ]
        ]

        when:
        post('/units', request, 422)

        then:
        thrown(HttpMessageNotReadableException)
    }

    @WithUserDetails("user@domain.example")
    def "Validate unit dump dto in import endpoint"() {
        given:
        Map request = [
            unit:[name: 'New unit']
        ]

        when:
        post('/units/import', request, 400)

        then:
        MethodArgumentNotValidException e = thrown()
        e.message.contains('Domain references must be present')
    }

    @WithUserDetails("user@domain.example")
    def "Null values in risks array are not accepted"() {
        given:
        def domain = parseJson(get("/domains/${domain.idAsString}"))

        Map request = [
            unit    : [
                name: 'New unit',
                id: UUID.randomUUID().toString()
            ],
            domains : [
                domain
            ],
            elements: [],
            risks   : [null]
        ]

        when:
        post('/units/import', request, 400)

        then:
        MethodArgumentNotValidException e = thrown()
        e.message.contains('Field error in object \'unitDumpDto\' on field \'risks[]\': rejected value [null]')
    }
}
