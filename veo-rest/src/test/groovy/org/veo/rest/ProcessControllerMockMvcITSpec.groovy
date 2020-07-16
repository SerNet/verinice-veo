/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
import org.springframework.web.bind.MethodArgumentNotValidException

import groovy.json.JsonSlurper

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.custom.SimpleProperties
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
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
class ProcessControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private ProcessRepositoryImpl processRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Unit unit2
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

            def client= newClient{
                id = clientId
                domains = [domain, domain1] as Set
            }

            unit = newUnit client, {
                name = "Test unit"
            }
            client.units << unit
            unit.client = client
            clientRepository.save(client)

            unit2 = newUnit client, {
                name = "Test unit2"
            }
            client.units << unit2
            unit2.client = client

            clientRepository.save(client)
        }
    }

    @WithUserDetails("user@domain.example")
    def "create a process"() {
        given: "a request body"

        Map request = [
            name: 'New process',
            owner: [
                displayName: 'test2',
                href: '/units/' + unit.id.uuidValue()
            ]
        ]

        when: "a request is made to the server"

        def results = post('/processes', request)

        then: "the process is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new unit is returned"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Process created successfully.'
    }

    @WithUserDetails("user@domain.example")
    def "try to create a process without owner"() {
        given: "a request body without an owner"

        Map request = [
            name: 'New process'
        ]

        when: "a request is made to the server"
        def results = post('/processes', request, false)

        then: "the process is not created"
        MethodArgumentNotValidException ex = thrown()

        and: "the reason is given"
        ex.message ==~ /.*Validation failed for argument.*owner must be present.*/
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a process"() {
        given: "a saved process"
        def id = Key.newUuid()
        def process = newProcess unit, {
            it.id = id
            name = 'Test process'
        }

        process = txTemplate.execute {
            processRepository.save(process)
        }

        when: "a request is made to the server"
        def results = get("/processes/${process.id.uuidValue()}")

        then: "the process is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test process'
        result.owner.href == "/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "try to put a process without a name"() {
        given: "a saved process"

        Key<UUID> id = Key.newUuid()
        def process = newProcess unit, {
            it.id = id
            name = 'Test process-put-noname'
            domains = [domain1] as Set
        }

        process = txTemplate.execute {
            processRepository.save(process)
        }

        Map request = [
            id: id.uuidValue(),
            // note that currently the name must not be null but it can be empty ("")
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                href: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ]
            ,
            domains: [
                [
                    href: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ]
        ]

        when: "a request is made to the server"
        def results = put("/processes/${process.id.uuidValue()}", request, false)

        then: "the process is not updated"
        MethodArgumentNotValidException ex = thrown()

        and: "the reason is given"
        ex.message ==~ /.*Validation failed for argument.*name must be present.*/

    }

    @WithUserDetails("user@domain.example")
    def "put a process"() {
        given: "a saved process"

        Key<UUID> id = Key.newUuid()
        def process = newProcess unit, {
            it.id = id
            name = 'Test process-put'
            domains = [domain1] as Set
        }

        process = txTemplate.execute {
            processRepository.save(process)
        }

        Map request = [
            id: id.uuidValue(),
            name: 'New Process-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                href: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ]
            ,
            domains: [
                [
                    href: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ]
        ]

        when: "a request is made to the server"
        def results = put("/processes/${process.id.uuidValue()}", request)

        then: "the process is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New Process-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.href == "/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "delete a process"() {

        given: "an existing process"
        Key<UUID> id = Key.newUuid()
        def process = newProcess unit, {
            it.id = id
            name = 'Test process-delete'
            domains = [domain1] as Set
        }
        process = txTemplate.execute {
            processRepository.save(process)
        }

        when: "a delete request is sent to the server"
        def results = delete("/processes/${process.id.uuidValue()}")

        then: "the process is deleted"
        results.andExpect(status().isOk())
        processRepository.exists(id) == false
    }

    @WithUserDetails("user@domain.example")
    def "put a process with custom aspect"() {
        given: "a saved process"

        CustomProperties cp = new SimpleProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Process'] as Set)
        cp.setId(Key.newUuid())
        Key<UUID> id = Key.newUuid()
        def process = newProcess unit, {
            it.id = id
            name = 'Test process-put'
            customAspects = [cp] as Set
            domains = [domain1] as Set
        }

        process = txTemplate.execute {
            processRepository.save(process)
        }

        Map request = [
            id: id.uuidValue(),
            name: 'New Process-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                href: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ]
            ,
            domains: [
                [
                    href: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ],
            customAspects:
            [
                [
                    id: '00000000-0000-0000-0000-000000000000',
                    type : 'my.aspect-test1',
                    applicableTo: [
                        "Process"
                    ],
                    domains: [],
                    attributes: [
                        test1:'value1',
                        test2:'value2'
                    ]
                ]

            ]
        ]

        when: "a request is made to the server"
        def results = put("/processes/${process.id.uuidValue()}", request)

        then: "the process is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New Process-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.href == "/units/"+unit.id.uuidValue()
        def entity = txTemplate.execute {
            processRepository.findById(id).get()
        }
        entity.name == 'New Process-2'
        entity.abbreviation == 'u-2'
        entity.customAspects.first().type == 'my.aspect-test1'
        entity.customAspects.first().applicableTo == ['Process'] as Set
        entity.customAspects.first().stringProperties.test1 == 'value1'
        entity.customAspects.first().stringProperties.test2 == 'value2'
    }

    @WithUserDetails("user@domain.example")
    def "overwrite a custom aspect attribute"() {
        given: "a saved process"

        CustomProperties cp = new SimpleProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Process'] as Set)
        cp.setId(Key.newUuid())
        cp.setProperty('test1', 'value1')
        Key<UUID> id = Key.newUuid()
        def process = newProcess unit, {
            it.id = id
            name = 'Test process-put'
            customAspects = [cp] as Set
            domains = [domain1] as Set
        }

        process = txTemplate.execute {
            processRepository.save(process)
        }

        when: "a request is made to the server"
        Map request = [
            id: id.uuidValue(),
            name: 'New Process-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                href: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ]
            ,
            domains: [
                [
                    href: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ],
            customAspects:
            [
                [
                    id: cp.id.uuidValue(),
                    type : 'my.new.type',
                    applicableTo: [
                        "Process"
                    ],
                    domains: [],
                    attributes: [
                        test1:'value2'
                    ]
                ]
            ]
        ]
        def results = put("/processes/${process.id.uuidValue()}", request)

        then: "the process is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New Process-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.href == "/units/"+unit.id.uuidValue()
        def entity = txTemplate.execute { processRepository.findById(id).get() }
        entity.name == 'New Process-2'
        entity.abbreviation == 'u-2'
        entity.customAspects.first().type == 'my.new.type'
        entity.customAspects.first().applicableTo == ['Process'] as Set
        entity.customAspects.first().stringProperties.test1 == 'value2'
    }

    @WithUserDetails("user@domain.example")
    def "id is required in custom aspects"() {
        given: "a saved process"

        CustomProperties cp = new SimpleProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Process'] as Set)
        cp.setId(Key.newUuid())
        Key<UUID> id = Key.newUuid()
        def process = newProcess unit, {
            it.id = id
            name = 'Test process-put'
            customAspects = [cp] as Set
            domains = [domain1] as Set
        }

        process = txTemplate.execute {
            processRepository.save(process)
        }

        Map request = [
            id: id.uuidValue(),
            name: 'New Process-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                href: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ]
            ,
            domains: [
                [
                    href: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ],
            customAspects:
            [
                [
                    type : 'my.aspect-test1',
                    applicableTo: [
                        "Process"
                    ],
                    domains: [],
                    attributes: [
                        test1:'value1',
                        test2:'value2'
                    ]
                ]

            ]
        ]

        when: "a request is made to the server"
        def results = put("/processes/${process.id.uuidValue()}", request, false)

        then: "the process is not updated"
        MethodArgumentNotValidException ex = thrown()

        and: "the reason is given"
        ex.message ==~ /.*Validation failed for argument.*must not be null.*/
    }

    @WithUserDetails("user@domain.example")
    def "put a process with link"() {
        given: "a created asset and process"

        Map createAssetRequest = [
            name: 'New Asset',
            owner: [
                displayName: 'test2',
                href: '/units/' + unit.id.uuidValue()
            ]
        ]

        def creatAssetResponse = post('/assets', createAssetRequest)

        def createAssetResult = new JsonSlurper().parseText(creatAssetResponse.andReturn().response.contentAsString)

        Map createProcessRequest = [
            name: 'New process',
            owner: [
                displayName: 'test2',
                href: '/units/' + unit.id.uuidValue()
            ]
        ]

        def createProcessResponse = post('/processes', createProcessRequest)
        def createProcessResult = new JsonSlurper().parseText(createProcessResponse.andReturn().response.contentAsString)
        def processId = createProcessResult.resourceId

        Map putProcessRequest = [
            id: processId,
            name: 'New Process-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                href: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ]
            ,
            domains: [
                [
                    href: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ],
            links:
            [
                [
                    id: '00000000-0000-0000-0000-000000000000',
                    type : 'my.link-test',
                    applicableTo: [
                        "Process"
                    ],
                    name:'test link prcess->asset',
                    domains: [],
                    attributes: [
                        test1:'value1',
                        test2:'value2'
                    ],
                    source:
                    [
                        href: '/processs/'+createProcessResult.resourceId,
                        displayName: 'test ddd'
                    ],
                    target:
                    [
                        href: '/assets/'+createAssetResult.resourceId,
                        displayName: 'test ddd'
                    ]
                ]
            ]
        ]

        when: "a request is made to the server"
        def results = put("/processes/${createProcessResult.resourceId}", putProcessRequest)

        then: "the process is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New Process-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.href == "/units/"+unit.id.uuidValue()
        and: 'there is one link'
        def links = result.links
        links.size() == 1
        and: 'the expected link is present'
        links.first().name == 'test link prcess->asset'
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all processes for a client"() {
        given: "a saved process"

        def process = newProcess unit, {
            name = 'Test process-1'
        }

        def process2 = newProcess unit2, {
            name = 'Test process-2'
        }

        (process, process2) = txTemplate.execute {
            [process, process2].collect(processRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/processes")

        then: "the processes are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2
        when:
        def sortedResult = result.sort{ it.name }
        then:
        sortedResult.first().name == 'Test process-1'
        sortedResult.first().owner.href == "/units/${unit.id.uuidValue()}"
        sortedResult[1].name == 'Test process-2'
        sortedResult[1].owner.href == "/units/${unit2.id.uuidValue()}"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all processes for a unit"() {
        given: "a saved process"

        def process = newProcess unit, {
            name = 'Test process-1'
        }

        def process2 = newProcess unit2, {
            name = 'Test process-2'
        }

        (process, process2) = txTemplate.execute {
            [process, process2].collect(processRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/processes?parent=${unit.id.uuidValue()}")

        then: "the processes are returned"
        results.andExpect(status().isOk())

        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)

        then:
        result.size == 1
        result.first().name == 'Test process-1'
        result.first().owner.href == "/units/"+unit.id.uuidValue()

        when: "a request is made to the server"
        results = get("/processes?parent=${unit2.id.uuidValue()}")

        then: "the processes are returned"
        results.andExpect(status().isOk())

        when:
        result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)

        then:
        result.size == 1
        result.first().name == 'Test process-2'
        result.first().owner.href == "/units/"+unit2.id.uuidValue()
    }
}
