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

import java.time.Instant

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ScenarioData

/**
 * Integration test for the unit controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
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
    private Domain dsgvoDomain

    def setup() {
        txTemplate.execute {
            def client = createTestClient()
            dsgvoDomain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)

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
    def "retrieve a process"() {
        given: "a saved process"
        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                name = 'Test process'
            })
        }

        when: "a request is made to the server"
        def result = parseJson(get("/processes/${process.idAsString}"))

        then: "the process is found"
        result._self == "http://localhost/processes/${process.idAsString}"
        result.name == 'Test process'
        result.owner.targetUri == "http://localhost/units/" + unit.idAsString

        and: "the risks property is not present"
        result.risks == null
    }

    @WithUserDetails("user@domain.example")
    def "delete a process"() {
        given: "an existing process"
        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                associateWithDomain(dsgvoDomain, "PRO_DataProcessing", "NEW")
            })
        }

        when: "a delete request is sent to the server"
        delete("/processes/${process.idAsString}")

        then: "the process is deleted"
        !processRepository.exists(process.id)
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

        when: "all processes are requested"
        def result = parseJson(get("/processes"))

        then: "the processes are returned"
        def sortedItems = result.items.sort{
            it.name
        }
        sortedItems[0].name == 'Test process-1'
        sortedItems[0].owner.targetUri == "http://localhost/units/${unit.idAsString}"
        sortedItems[1].name == 'Test process-2'
        sortedItems[1].owner.targetUri == "http://localhost/units/${unit2.idAsString}"
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

        when: "all processes in the first unit are requested"
        def result = parseJson(get("/processes?unit=${unit.idAsString}"))

        then:
        result.items.size() == 1
        result.items.first().name == 'Test process-1'
        result.items.first().owner.targetUri == "http://localhost/units/"+unit.idAsString

        when: "all processes in unit 2 are requested"
        result = parseJson(get("/processes?unit=${unit2.idAsString}"))

        then:
        result.items.size() == 1
        result.items.first().name == 'Test process-2'
        result.items.first().owner.targetUri == "http://localhost/units/"+unit2.idAsString
    }

    @WithUserDetails("user@domain.example")
    def "filter processes by sub type"() {
        given: "one VT and one process without a sub type"
        txTemplate.execute {
            processRepository.save(newProcess(unit) {
                name = 'Test process-1'
                associateWithDomain(dsgvoDomain, 'VT', "NEW")
            })
            processRepository.save(newProcess(unit) {
                name = 'Test process-2'
            })
        }

        when: "the sub type param is omitted"
        def result = parseJson(get("/processes"))

        then: "both processes are returned"
        result.items.size() == 2

        when: "VT processes are queried"
        result = parseJson(get("/processes?subType=VT"))

        then: "only the VT process is returned"
        result.items*.name == ['Test process-1']

        when: "processes without a sub type are queried"
        result = parseJson(get("/processes?subType="))

        then: "only the process without a sub type is returned"
        result.items*.name == ['Test process-2']
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be created for a process"() {
        given: "saved elements"
        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                name = 'New process-2'
                associateWithDomain(dsgvoDomain, "PRO_DataProcessing", "NEW")
            })
        }
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                associateWithDomain(dsgvoDomain, "SCN_Scenario", "NEW")
            })
        }

        when: "a new risk can be created successfully"
        def json = parseJson(post("/processes/"+process.idAsString+"/risks", [
            scenario: [ targetUri: '/scenarios/'+ scenario.idAsString ],
            domains: [
                (dsgvoDomain.getIdAsString()) : [
                    reference: [targetUri: '/domains/'+ dsgvoDomain.idAsString ]
                ]
            ]
        ] as Map))

        then:
        with(json) {
            resourceId != null
            resourceId.length() == 36
            success
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
                get("/processes/" + process.idAsString + "/risks/" + scenario.idAsString))

        then: "the correct object is returned"
        getResult != null
        with(getResult) {
            it.process.targetUri ==~ /.*${process.idAsString}.*/
            it.scenario.targetUri ==~ /.*${scenario.idAsString}.*/
            it.scenario.targetUri ==~ /.*${postResult.resourceId}.*/
            it.domains.values().first().reference.displayName == this.dsgvoDomain.displayName
            it._self ==~ /.*processes\/${process.idAsString}\/risks\/${scenario.idAsString}.*/
            Instant.parse(it.createdAt) > beforeCreation
            Instant.parse(it.updatedAt) > beforeCreation
            it.createdBy == "user@domain.example"
            it.updatedBy == "user@domain.example"
        }
    }

    @WithUserDetails("user@domain.example")
    def "A list of risks can be retrieved for a process"() {
        given: "a process with multiple risks"
        Process process = createRisk().first()
        createTwoRisks(process)

        when: "the risks are queried"
        def getResult = parseJson(
                get("/processes/${process.idAsString}/risks"))

        then: "the risks are retreived"
        getResult.size() == 3
    }

    private createTwoRisks(Process process) {
        def scenario2 = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                associateWithDomain(dsgvoDomain, "SCN_Scenario", "NEW")
            })
        }
        def scenario3 = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                associateWithDomain(dsgvoDomain, "SCN_Scenario", "NEW")
            })
        }
        post("/processes/"+process.idAsString+"/risks", [
            scenario: [ targetUri: '/scenarios/'+ scenario2.idAsString ],
            domains: [
                (dsgvoDomain.idAsString) : [
                    reference: [targetUri: '/domains/'+ dsgvoDomain.idAsString ]
                ]
            ]
        ] as Map)
        post("/processes/"+process.idAsString+"/risks", [
            scenario: [ targetUri: '/scenarios/'+ scenario3.idAsString ],
            domains: [
                (dsgvoDomain.idAsString) : [
                    reference: [targetUri: '/domains/'+ dsgvoDomain.idAsString ]
                ]
            ]
        ] as Map)
    }

    @WithUserDetails("user@domain.example")
    def "An embedded list of risks can be retrieved for a process"() {
        given: "a process with multiple risks"
        Process process = createRisk().first()
        createTwoRisks(process)

        when: "the embedded risks are queried"
        def response = parseJson(
                get("/processes/${process.idAsString}?embedRisks=true"))

        then: "the risks are retreived"
        response.name == "process null"
        response.risks != null
        response.risks.size() == 3
        response.risks*.scenario.forEach{assert it.displayName =~ /SCN-.*scenario null/}
        response.risks*.process.forEach{assert it.displayName =~ /PRO-.*process null/}
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be deleted"() {
        given: "a process risk"
        def (Process process, ScenarioData scenario, Object postResult) = createRisk()

        when: "the risk is deleted"
        delete("/processes/${process.idAsString}/risks/${scenario.idAsString}")

        then: "the risk has been removed"
        processRepository.findByRisk(scenario).isEmpty()

        and: "all referenced objects are still present"
        processRepository.findById(process.id).isPresent()
        scenarioRepository.findById(scenario.id).isPresent()
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be updated with new information"() {
        given: "a process risk and additional elements"
        def beforeCreation = Instant.now()
        def (Process process, ScenarioData scenario, Object postResult) = createRisk()

        def person = txTemplate.execute {
            personRepository.save(newPerson(unit) {
                name = 'New person-1'
                associateWithDomain(dsgvoDomain, "PER_Person", "NEW")
            })
        }

        def control = txTemplate.execute {
            controlRepository.save(newControl(unit) {
                name = 'New control-1'
                associateWithDomain(dsgvoDomain, "CTL_TOM", "NEW")
            })
        }

        and: "the created risk is retrieved"
        def getResponse = get("/processes/" + process.idAsString + "/risks/" + scenario.idAsString)
        def getResult = parseJson(getResponse)
        String eTag = getResponse.andReturn().response.getHeader("ETag").replace("\"", "")

        when: "the risk is updated"
        def beforeUpdate = Instant.now()
        def putBody = getResult + [
            mitigation: [targetUri: '/controls/' + control.idAsString],
            riskOwner: [targetUri: '/persons/' + person.idAsString]
        ]
        Map headers = [
            'If-Match': eTag
        ]

        put("/processes/${process.idAsString}/risks/${scenario.idAsString}", putBody as Map, headers)

        and: "the risk is retrieved again"
        def riskJson = parseJson(
                get("/processes/" + process.idAsString + "/risks/" + scenario.idAsString))

        then: "the information was persisted"
        eTag.length() > 0
        riskJson != null
        with(riskJson) {
            it.mitigation.targetUri ==~ /.*${control.idAsString}.*/
            it.riskOwner.targetUri ==~ /.*${person.idAsString}.*/
            it.process.targetUri ==~ /.*${process.idAsString}.*/
            it.scenario.targetUri ==~ /.*${scenario.idAsString}.*/
            it.domains.values().first().reference.displayName == this.dsgvoDomain.displayName
            it._self ==~ /.*processes\/${process.idAsString}\/risks\/${scenario.idAsString}.*/
            Instant.parse(it.createdAt) > beforeCreation
            Instant.parse(it.createdAt) < beforeUpdate
            Instant.parse(it.updatedAt) > beforeUpdate
            it.createdBy == "user@domain.example"
            it.updatedBy == "user@domain.example"
        }

        when: "the person and control are removed"
        beforeUpdate = Instant.now()
        delete("/persons/${person.idAsString}")
        delete("/controls/${control.idAsString}")
        riskJson = parseJson(
                get("/processes/" + process.idAsString + "/risks/" + scenario.idAsString))

        then: "their references are removed from the risk"
        riskJson != null
        with(riskJson) {
            it._self ==~ /.*processes\/${process.idAsString}\/risks\/${scenario.idAsString}.*/
            it.mitigation == null
            it.riskOwner == null
            Instant.parse(it.createdAt) > beforeCreation
            Instant.parse(it.createdAt) < beforeUpdate
            Instant.parse(it.updatedAt) > beforeUpdate
            it.createdBy == "user@domain.example"
            it.updatedBy == "user@domain.example"
        }

        when: "the scenario is removed"
        delete("/scenarios/${scenario.idAsString}")

        and: "the risk is requested"
        get("/processes/" + process.idAsString + "/risks/" + scenario.idAsString,
                404)

        then: "the risk was removed as well"
        thrown(NotFoundException)
    }

    @WithUserDetails("user@domain.example")
    def "inspection for unknown entity returns 404"() {
        given:
        def processId = '1a611e47-d76e-4659-ba28-6e7e736c562a'

        when:
        get("/processes/${processId}/inspection?domain=${dsgvoDomain.idAsString}", 404)

        then:
        thrown(NotFoundException)
    }

    @WithUserDetails("user@domain.example")
    def "inspection for unknown domain returns 404"() {
        given:
        def process = txTemplate.execute {
            processRepository.save(newProcess(unit))
        }

        def domainId = '565839cb-b562-4da6-b78f-f1d75ab8f84b'

        when:
        get("/processes/${process.idAsString}/inspection?domain=${domainId}", 404)

        then:
        thrown(NotFoundException)
    }

    @WithUserDetails("user@domain.example")
    def "inspection for invalid process id returns 400"() {
        when:
        get("/processes/helloworld/inspection?domain=${dsgvoDomain.idAsString}", 400)

        then:
        thrown(MethodArgumentTypeMismatchException)
    }

    @WithUserDetails("user@domain.example")
    def "inspection for invalid domain id returns 400"() {
        given:
        def process = txTemplate.execute {
            processRepository.save(newProcess(unit))
        }

        when:
        get("/processes/${process.idAsString}/inspection?domain=foobar", 400)

        then:
        thrown(MethodArgumentTypeMismatchException)
    }

    private List createRisk() {
        def process = txTemplate.execute {
            processRepository.save(newProcess(unit) {
                associateWithDomain(dsgvoDomain, "PRO_DataProcessing", "NEW")
            })
        }
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                associateWithDomain(dsgvoDomain, "SCN_Scenario", "NEW")
            })
        }
        def postResult = parseJson(
                post("/processes/" + process.idAsString + "/risks", [
                    scenario: [targetUri: '/scenarios/' + scenario.idAsString],
                    domains : [
                        (dsgvoDomain.getIdAsString()) : [
                            reference: [targetUri: '/domains/' + dsgvoDomain.idAsString]
                        ]
                    ]
                ]))
        return [
            process,
            scenario,
            postResult
        ]
    }
}
