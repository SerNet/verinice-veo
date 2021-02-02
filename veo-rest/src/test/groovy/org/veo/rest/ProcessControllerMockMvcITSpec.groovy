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

import java.time.Instant

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.util.NestedServletException

import org.veo.adapter.presenter.api.DeviatingIdException
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ScenarioData
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
class ProcessControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private ControlRepositoryImpl controlRepository

    @Autowired
    private ProcessRepositoryImpl processRepository

    @Autowired
    private PersonRepositoryImpl personRepository

    @Autowired
    private ScenarioRepositoryImpl scenarioRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private DomainRepositoryImpl domainRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Unit unit2
    private Domain domain
    private Domain domain1
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
    String salt = "salt-for-etag"

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

            unit = newUnit(client) {
                name = "Test unit"
            }

            clientRepository.save(client)

            unit2 = newUnit(client) {
                name = "Test unit2"
            }

            clientRepository.save(client)
            unitRepository.save(unit)
            unitRepository.save(unit2)
        }
    }

    @WithUserDetails("user@domain.example")
    def "create a process"() {
        given: "a request body"

        Map request = [
            name: 'New process',
            owner: [
                displayName: 'test2',
                targetUri: '/units/' + unit.id.uuidValue()
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
        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                name = 'Test process'
            })
        }

        when: "a request is made to the server"
        def results = get("/processes/${process.id.uuidValue()}")

        then: "the process is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test process'
        result.owner.targetUri == "http://localhost/units/" + unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "try to put a process without a name"() {
        given: "a saved process"

        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                domains = [domain1] as Set
            })
        }

        Map request = [
            // note that currently the name must not be null but it can be empty ("")
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
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(process.id.uuidValue(), 1)
        ]
        def results = put("/processes/${process.id.uuidValue()}", request, headers, false)

        then: "the process is not updated"
        MethodArgumentNotValidException ex = thrown()

        and: "the reason is given"
        ex.message ==~ /.*Validation failed for argument.*name must be present.*/

    }

    @WithUserDetails("user@domain.example")
    def "put a process"() {
        given: "a saved process"

        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                domains = [domain1] as Set
            })
        }

        Map request = [
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
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(process.id.uuidValue(), process.version)
        ]
        def results = put("/processes/${process.id.uuidValue()}", request, headers)

        then: "the process is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New Process-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "delete a process"() {

        given: "an existing process"
        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                domains = [domain1] as Set
            })
        }

        when: "a delete request is sent to the server"
        def results = delete("/processes/${process.id.uuidValue()}")

        then: "the process is deleted"
        results.andExpect(status().isOk())
        !processRepository.exists(process.id)
    }

    @WithUserDetails("user@domain.example")
    def "put a process with custom aspect"() {
        given: "a saved process"

        CustomProperties cp = newCustomProperties("my.new.type") {
            applicableTo = ['Process'] as Set
        }

        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                domains = [domain1] as Set
                customAspects = [cp] as Set
            })
        }

        Map request = [
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
            customAspects:
            [
                'process_SensitiveData' :
                [
                    applicableTo: [
                        "Process"
                    ],
                    domains: [],
                    attributes: [
                        process_SensitiveData_SensitiveData: true,
                        process_SensitiveData_comment: 'no comment'
                    ]
                ]

            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(process.id.uuidValue(), process.version)
        ]
        def results = put("/processes/${process.id.uuidValue()}", request, headers)

        then: "the process is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New Process-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()

        when:
        def entity = txTemplate.execute {
            processRepository.findById(process.id).get().tap {
                // make sure that the proxy is resolved
                customAspects.first()
            }
        }

        then:
        entity.name == 'New Process-2'
        entity.abbreviation == 'u-2'
        entity.customAspects.first().type == 'process_SensitiveData'
        entity.customAspects.first().applicableTo == ['Process'] as Set
        entity.customAspects.first().booleanProperties.process_SensitiveData_SensitiveData
        entity.customAspects.first().stringProperties.process_SensitiveData_comment == 'no comment'
    }

    @WithUserDetails("user@domain.example")
    def "overwrite a custom aspect attribute"() {
        given: "a saved process"

        CustomProperties cp = newCustomProperties("process_SensitiveData") {
            applicableTo = ['Process'] as Set
            it.setProperty('process_SensitiveData_comment', 'old comment')
        }

        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                domains = [domain1] as Set
                customAspects = [cp] as Set
            })
        }

        when: "a request is made to the server"
        Map request = [
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
            customAspects:
            [
                'process_SensitiveData' :
                [
                    applicableTo: [
                        "Process"
                    ],
                    domains: [],
                    attributes: [
                        process_SensitiveData_comment:'new comment'
                    ]
                ]
            ]
        ]
        Map headers = [
            'If-Match': ETag.from(process.id.uuidValue(), process.version)
        ]
        def results = put("/processes/${process.id.uuidValue()}", request, headers)

        then: "the process is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New Process-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()

        when:
        def entity = txTemplate.execute {
            processRepository.findById(process.id).get().tap {
                // make sure that the proxy is resolved
                customAspects.first()
            }
        }

        then:
        entity.name == 'New Process-2'
        entity.abbreviation == 'u-2'
        entity.customAspects.first().type == 'process_SensitiveData'
        entity.customAspects.first().applicableTo == ['Process'] as Set
        entity.customAspects.first().stringProperties.process_SensitiveData_comment == 'new comment'
    }

    @WithUserDetails("user@domain.example")
    def "put a process with link"() {
        given: "a created asset and process"

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
                'process_DataCategories' : [
                    [
                        applicableTo: [
                            "Process"
                        ],
                        name:'test link prcess->asset',
                        domains: [],
                        attributes: [
                            process_DataCategories_dataOrigin: 'process_DataCategories_dataOrigin_direct',
                            process_DataCategories_comment: 'ok'
                        ],
                        target:
                        [
                            targetUri: '/assets/'+createAssetResult.resourceId,
                            displayName: 'test ddd'
                        ]
                    ]]
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(createProcessResult.resourceId, 0)
        ]
        def results = put("/processes/${createProcessResult.resourceId}", putProcessRequest, headers)

        then: "the process is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New Process-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
        and: 'there is one type of links'
        def links = result.links
        links.size() == 1
        and: 'there is one link of the expected type'
        def linksOfExpectedType = links.'process_DataCategories'
        linksOfExpectedType.size() == 1
        and: 'the expected link is present'
        linksOfExpectedType.first().name == 'test link prcess->asset'
    }

    @WithUserDetails("user@domain.example")
    def "post a process with link"() {
        when:
        def result = parseJson(post('/assets', [
            name : 'My asset',
            owner: [
                targetUri: '/units/'+unit.id.uuidValue()
            ]
        ]))
        def assetId = result.resourceId
        result = parseJson(post('/processes', [
            name : 'My process',
            owner: [
                targetUri: '/units/'+unit.id.uuidValue()
            ],
            links: [
                'process_DataCategories': [
                    [
                        name  : 'categories',
                        target:
                        [
                            targetUri: "/assets/$assetId"
                        ]
                    ]
                ]
            ]
        ]))
        def processId = result.resourceId
        def process1 = txTemplate.execute{
            Process process = processRepository.findById(Key.uuidFrom(processId)).get()
            with(process.links) {
                //need to be in the open session
                size() == 1
                first().type == 'process_DataCategories'
                first().name == 'categories'
                first().target.id.uuidValue() == assetId
            }
            return process
        }
        then:
        process1 != null
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all processes for a client"() {
        given: "two saved processes in different units of the same client"

        txTemplate.execute {
            processRepository.save(newProcess(unit) {
                name = 'Test process-1'
            })
            processRepository.save(newProcess(unit2) {
                name = 'Test process-2'
            })
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
        sortedResult.first().owner.targetUri == "http://localhost/units/${unit.id.uuidValue()}"
        sortedResult[1].name == 'Test process-2'
        sortedResult[1].owner.targetUri == "http://localhost/units/${unit2.id.uuidValue()}"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all processes for a unit"() {
        given: "two saved process in different units"

        txTemplate.execute {
            processRepository.save(newProcess(unit) {
                name = 'Test process-1'
            })
            processRepository.save(newProcess(unit2) {
                name = 'Test process-2'
            })
        }

        when: "a request is made to the server"
        def result = parseJson(get("/processes?unit=${unit.id.uuidValue()}"))

        then:
        result.size == 1
        result.first().name == 'Test process-1'
        result.first().owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()

        when: "a request is made to the server"
        result = parseJson(get("/processes?unit=${unit2.id.uuidValue()}"))

        then:
        result.size == 1
        result.first().name == 'Test process-2'
        result.first().owner.targetUri == "http://localhost/units/"+unit2.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "filter processes by sub type"() {
        given: "one VT and one process without a sub type"

        txTemplate.execute {
            processRepository.save(newProcess(unit) {
                name = 'Test process-1'
                setSubType(domain, 'VT')
            })
            processRepository.save(newProcess(unit) {
                name = 'Test process-2'
            })
        }

        when: "the sub type param is omitted"
        def result = parseJson(get("/processes"))
        then: "both processes are returned"
        result.size == 2

        when: "VT processes are queried"
        result = parseJson(get("/processes?subType=VT"))
        then: "only the VT process is returned"
        result.size == 1
        result.first().name == 'Test process-1'

        when: "processes without a sub type are queried"
        result = parseJson(get("/processes?subType="))
        then: "only the process without a sub type is returned"
        result.size == 1
        result.first().name == 'Test process-2'
    }

    @WithUserDetails("user@domain.example")
    def "can't put a process with another process's ID"() {
        given: "two processes"
        def process1 = txTemplate.execute({
            processRepository.save(newProcess(unit, {
                name = "old name 1"
            }))
        })
        def process2 = txTemplate.execute({
            processRepository.save(newProcess(unit, {
                name = "old name 2"
            }))
        })
        when: "a put request tries to update process 1 using the ID of process 2"
        Map headers = [
            'If-Match': ETag.from(process1.id.uuidValue(), 1)
        ]
        put("/processes/${process2.id.uuidValue()}", [
            id: process1.id.uuidValue(),
            name: "new name 1",
            owner: [targetUri: '/units/' + unit.id.uuidValue()]
        ], headers, false)
        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }


    @WithUserDetails("user@domain.example")
    def "can put back process"() {
        given: "a new process"
        def id = parseJson(post("/processes/", [
            name: "new name",
            owner: [targetUri: "/units/"+unit.id.uuidValue()]
        ])).resourceId
        def getResult = get("/processes/$id")

        expect: "putting the retrieved process back to be successful"
        put("/processes/$id", parseJson(getResult), [
            "If-Match": getTextBetweenQuotes(getResult.andReturn().response.getHeader("ETag"))
        ])
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be created for a process"() {
        given: "saved entities"
        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                name = 'New process-2'
                domains = [domain1] as Set
            })
        }
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                domains = [domain1] as Set
            })
        }

        when: "a new risk can be created successfully"
        def result= post("/processes/"+process.id.uuidValue()+"/risks", [
            scenario: [ targetUri: '/scenarios/'+ scenario.id.uuidValue() ],
            domains: [
                [targetUri: '/domains/'+ domain1.id.uuidValue() ] ]
        ] as Map)

        then:
        result.andExpect(status().isCreated())
        def json = parseJson(result)
        json.with {
            resourceId != null
            resourceId.length() == 36
            success == true
            message == "Process risk created successfully."
        }
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be retrieved for a process"() {
        given: "a process risk"
        def beforeCreation = Instant.now()
        def (Process process, ScenarioData scenario, Object postResult) = createRisk()

        when: "the risk is requested"
        def getResult = parseJson(
                get("/processes/" + process.id.uuidValue() + "/risks/" + scenario.id.uuidValue(),
                true)
                )

        then: "the correct object is returned"
        getResult != null
        with(getResult) {
            it.process.targetUri ==~ /.*${process.id.uuidValue()}.*/
            it.scenario.targetUri ==~ /.*${scenario.id.uuidValue()}.*/
            it.scenario.targetUri ==~ /.*${postResult.resourceId}.*/
            it.domains.first().displayName == this.domain1.displayName
            it._self ==~ /.*processes\/${process.id.uuidValue()}\/risks\/${scenario.id.uuidValue()}.*/
            Instant.parse(it.createdAt) > beforeCreation
            Instant.parse(it.updatedAt) > beforeCreation
            it.createdBy == "user@domain.example"
            it.updatedBy == "user@domain.example"
        }
    }

    @WithUserDetails("user@domain.example")
    def "A list of risks can be retrieved for a process"() {
        given: "A process with multiple risks"
        def (Process process, ScenarioData scenario, Object postResult) = createRisk()
        def scenario2 = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                domains = [domain1] as Set
            })
        }
        def scenario3 = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                domains = [domain1] as Set
            })
        }
        post("/processes/"+process.id.uuidValue()+"/risks", [
            scenario: [ targetUri: '/scenarios/'+ scenario2.id.uuidValue() ],
            domains: [
                [targetUri: '/domains/'+ domain1.id.uuidValue() ] ]
        ] as Map)
        post("/processes/"+process.id.uuidValue()+"/risks", [
            scenario: [ targetUri: '/scenarios/'+ scenario3.id.uuidValue() ],
            domains: [
                [targetUri: '/domains/'+ domain1.id.uuidValue() ] ]
        ] as Map)

        when: "The risks are queried"
        def getResult = parseJson(
                get("/processes/${process.id.uuidValue()}/risks/"))

        then: "The risks are retreived"
        getResult.size == 3
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be deleted"() {
        given: "a process risk"
        def (Process process, ScenarioData scenario, Object postResult) = createRisk()

        when: "the risk is deleted"
        def result = delete("/processes/${process.id.uuidValue()}/risks/${scenario.id.uuidValue()}", true)

        then: "the risk has been removed"
        result.andExpect(status().isOk())
        processRepository.findByRisk(scenario).isEmpty()

        and: "all referenced objects are still present"
        processRepository.findById(process.id).isPresent()
        scenarioRepository.findById(scenario.id).isPresent()
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be updated with new information"() {
        given: "a process risk and additional entities"
        def beforeCreation = Instant.now()
        def (Process process, ScenarioData scenario, Object postResult) = createRisk()

        def person = txTemplate.execute {
            personRepository.save(newPerson(unit) {
                name = 'New person-1'
                domains = [domain1] as Set
            })
        }

        def control = txTemplate.execute {
            controlRepository.save(newControl(unit) {
                name = 'New control-1'
                domains = [domain1] as Set
            })
        }

        and: "the created risk is retrieved"
        def getResponse = get("/processes/" + process.id.uuidValue() + "/risks/" + scenario.id.uuidValue(),
                true)
        def getResult = parseJson(getResponse)
        String eTag = getResponse.andReturn().response.getHeader("ETag").replace("\"", "")


        when: "The risk is updated"
        def beforeUpdate = Instant.now()
        def putBody = getResult + [
            mitigation: [targetUri: '/controls/' + control.id.uuidValue()],
            riskOwner: [targetUri: '/persons/' + person.id.uuidValue()]
        ]
        Map headers = [
            'If-Match': eTag
        ]

        def putResult =
                put("/processes/${process.id.uuidValue()}/risks/${scenario.id.uuidValue()}",
                putBody as Map, headers, true)

        and: "the risk is retrieved again"
        def riskJson = parseJson(
                get("/processes/" + process.id.uuidValue() + "/risks/" + scenario.id.uuidValue(),
                true)
                )

        then: "the information was persisted"
        eTag.length() > 0
        riskJson != null
        with(riskJson) {
            it.mitigation.targetUri ==~ /.*${control.id.uuidValue()}.*/
            it.riskOwner.targetUri ==~ /.*${person.id.uuidValue()}.*/
            it.process.targetUri ==~ /.*${process.id.uuidValue()}.*/
            it.scenario.targetUri ==~ /.*${scenario.id.uuidValue()}.*/
            it.domains.first().displayName == this.domain1.displayName
            it._self ==~ /.*processes\/${process.id.uuidValue()}\/risks\/${scenario.id.uuidValue()}.*/
            Instant.parse(it.createdAt) > beforeCreation
            Instant.parse(it.createdAt) < beforeUpdate
            Instant.parse(it.updatedAt) > beforeUpdate
            it.createdBy == "user@domain.example"
            it.updatedBy == "user@domain.example"
        }

        when: "the person and control are removed"
        beforeUpdate = Instant.now()
        delete("/persons/${person.id.uuidValue()}")
        delete("/controls/${control.id.uuidValue()}")
        riskJson = parseJson(
                get("/processes/" + process.id.uuidValue() + "/risks/" + scenario.id.uuidValue(),
                true)
                )

        then: "their references are removed from the risk"
        riskJson != null
        with(riskJson) {
            it._self ==~ /.*processes\/${process.id.uuidValue()}\/risks\/${scenario.id.uuidValue()}.*/
            it.mitigation == null
            it.riskOwner == null
            Instant.parse(it.createdAt) > beforeCreation
            Instant.parse(it.createdAt) < beforeUpdate
            Instant.parse(it.updatedAt) > beforeUpdate
            it.createdBy == "user@domain.example"
            it.updatedBy == "user@domain.example"
        }

        when: "the scenario is removed"
        delete("/scenarios/${scenario.id.uuidValue()}")

        and: "the risk is requested"
        get("/processes/" + process.id.uuidValue() + "/risks/" + scenario.id.uuidValue(),
                false)

        then: "the risk was removed as well"
        def e = thrown NestedServletException
        e.getCause() instanceof NoSuchElementException
    }

    private List createRisk() {
        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                domains = [domain1] as Set
            })
        }
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                domains = [domain1] as Set
            })
        }
        def postResult = parseJson(
                post("/processes/" + process.id.uuidValue() + "/risks", [
                    scenario: [targetUri: '/scenarios/' + scenario.id.uuidValue()],
                    domains : [
                        [targetUri: '/domains/' + domain1.id.uuidValue()]]
                ]))
        return [
            process,
            scenario,
            postResult
        ]
    }
}
