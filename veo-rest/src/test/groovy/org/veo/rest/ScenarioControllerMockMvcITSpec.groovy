/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Ben Nasrallah.
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
import org.veo.core.entity.Domain
import org.veo.core.entity.Scenario
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Integration test for the scenario controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class ScenarioControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private ScenarioRepositoryImpl scenarioRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Domain domain
    private Domain domain1

    def setup() {
        txTemplate.execute {
            def client = createTestClient()

            domain = newDomain(client) {
                abbreviation = "D"
                name = "Domain"
                applyElementTypeDefinition(newElementTypeDefinition("process", it) {
                    subTypes = [
                        SomeProcess: newSubTypeDefinition()
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition("scenario", it) {
                    subTypes = [
                        WorstCase: newSubTypeDefinition()
                    ]
                })
            }

            domain1 = newDomain(client) {
                abbreviation = "D1"
                name = "Domain 1"
            }

            client = clientRepository.save(client)

            unit = newUnit(client) {
                name = "Test unit"
            }

            unitRepository.save(unit)
        }
    }

    @WithUserDetails("user@domain.example")
    def "create a scenario"() {
        given: "a request body"
        Map request = [
            name: 'New Scenario',
            owner: [
                displayName: 'scenarioDataProtectionObjectivesEugdprEncryption',
                targetUri: 'http://localhost/units/' + unit.idAsString
            ]
        ]

        when: "a request is made to the server"
        def result = parseJson(post('/scenarios', request))

        then: "the location of the new scenario is returned"
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Scenario created successfully.'
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a scenario"() {
        given: "a saved scenario"
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                name = 'Test scenario-1'
            })
        }

        when: "a request is made to the server"
        def results = get("/scenarios/${scenario.idAsString}")

        then: "the eTag is set"
        getETag(results) != null

        and:
        def result = parseJson(results)
        result._self == "http://localhost/scenarios/${scenario.idAsString}"
        result.name == 'Test scenario-1'
        result.owner.targetUri == "http://localhost/units/"+unit.idAsString
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all scenarios for a unit"() {
        given: "saved scenarios"
        def scenario = newScenario(unit) {
            name = 'Test scenario-1'
        }
        def scenario2 = newScenario(unit) {
            name = 'Test scenario-2'
        }
        (scenario, scenario2) = txTemplate.execute {
            [scenario, scenario2].collect(scenarioDataRepository.&save)
        }

        when: "requesting all scenarios in the unit"
        def result = parseJson(get("/scenarios?unit=${unit.idAsString}"))

        then: "the scenarios are returned"
        def expectedUnitUri = "http://localhost/units/${unit.idAsString}"
        with(result.items.sort{it.name}) {
            size() == 2
            it[0].name == 'Test scenario-1'
            it[0].owner.targetUri == expectedUnitUri
            it[1].name == 'Test scenario-2'
            it[1].owner.targetUri == expectedUnitUri
        }
    }

    @WithUserDetails("user@domain.example")
    def "retrieving all scenarios for a unit returns composite elements and their parts"() {
        given: "a saved scenario  and a composite document containing it"
        txTemplate.execute {
            scenarioRepository.save(newScenario(unit) {
                name = 'Test composite scenario-1'
                parts <<  newScenario(unit) {
                    name = 'Test scenario-1'
                }
            })
        }

        when: "requesting all scenarios in the unit"
        def result = parseJson(get("/scenarios?unit=${unit.idAsString}"))

        then: "the scenarios are returned"
        result.items*.name as Set == [
            'Test scenario-1',
            'Test composite scenario-1'
        ] as Set
    }

    @WithUserDetails("user@domain.example")
    def "put a scenario"() {
        given: "a saved scenario"
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                associateWithDomain(domain, "WorstCase", "NEW")
            })
        }

        Map request = [
            name: 'New scenario-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: 'http://localhost/units/'+unit.idAsString,
                displayName: 'test unit'
            ],  domains: [
                (domain.idAsString): [
                    subType: "WorstCase",
                    status: "NEW",
                ]
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(scenario.idAsString, scenario.version)
        ]
        def result = parseJson(put("/scenarios/${scenario.idAsString}", request, headers))

        then: "the scenario is found"
        result.name == 'New scenario-2'
        result.abbreviation == 'u-2'
        result.domains[domain.idAsString] == [
            subType: "WorstCase",
            status: "NEW",
            decisionResults: [:],
            riskValues: [:],
        ]
        result.owner.targetUri == "http://localhost/units/"+unit.idAsString
    }

    @WithUserDetails("user@domain.example")
    def "delete a scenario"() {
        given: "an existing scenario"
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit))
        }

        when: "a delete request is sent to the server"
        delete("/scenarios/${scenario.idAsString}")

        then: "the scenario is deleted"
        scenarioRepository.findById(scenario.id).empty
    }

    @WithUserDetails("user@domain.example")
    def "delete a scenario that is used by a risk"() {
        given: "a scenario that is used by a risk"
        def scenarioId = parseJson(post("/scenarios", [
            name: "scenery-oh",
            owner: [targetUri: "/units/$unit.idAsString"]
        ])).resourceId
        def processId = parseJson(post("/processes", [
            name: "possessive process",
            owner: [targetUri: "/units/$unit.idAsString"],
            domains: [
                (domain.idAsString): [
                    subType: "SomeProcess",
                    status: "NEW"
                ]
            ]
        ])).resourceId
        post("/processes/$processId/risks", [
            scenario: [targetUri: "/scenarios/$scenarioId"],
            domains: [
                (domain.idAsString): [
                    reference: [targetUri: "/domains/$domain.idAsString"]
                ]
            ]
        ])

        when: "the scenario is deleted"
        delete("/scenarios/$scenarioId")

        and: "trying to retrieve the risk"
        get("/processes/$processId/risks/$scenarioId", 404)

        then:
        thrown(NotFoundException)
    }

    @WithUserDetails("user@domain.example")
    def "can't put a scenario with another scenario's ID"() {
        given: "two scenarios"
        def scenario1 = txTemplate.execute({
            scenarioDataRepository.save(newScenario(unit, {
                name = "old name 1"
            }))
        })
        def scenario2 = txTemplate.execute({
            scenarioDataRepository.save(newScenario(unit, {
                name = "old name 2"
            }))
        })

        when: "a put request tries to update scenario 1 using the ID of scenario 2"
        Map headers = [
            'If-Match': ETag.from(scenario1.idAsString, 1)
        ]
        put("/scenarios/${scenario2.idAsString}", [
            id: scenario1.idAsString,
            name: "new name 1",
            owner: [targetUri: 'http://localhost/units/' + unit.idAsString]
        ], headers, 400)

        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }

    @WithUserDetails("user@domain.example")
    def "can put back scenario"() {
        given: "a new scenario"
        def id = parseJson(post("/scenarios", [
            name: "new name",
            owner: [targetUri: "http://localhost/units/"+unit.idAsString]
        ])).resourceId
        def getResult = get("/scenarios/$id")

        expect: "putting the retrieved scenario back to be successful"
        put("/scenarios/$id", parseJson(getResult), [
            "If-Match": getETag(getResult)
        ])
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a composite scenario's parts"() {
        given: "a saved scenario group with two parts"
        Scenario s1 = newScenario(unit) {
            name = "s1"
        }
        Scenario s2 = newScenario(unit) {
            name = "s2"
        }

        def compositeScenario = txTemplate.execute {
            scenarioRepository.save(newScenario(unit) {
                name = 'Test composite scenario'
                parts << s1 << s2
            })
        }

        when: "the server is queried for the scenario's parts"
        def result = parseJson(get("/scenarios/${compositeScenario.idAsString}/parts"))

        then: "the parts are found"
        result.size() == 2
        result.sort{it.name}.first().name == 's1'
        result.sort{it.name}[1].name == 's2'
    }

    @WithUserDetails("user@domain.example")
    // TODO VEO-1355: add a test for a scenario/domain with inspections
    def "there are no decisions for a scenario"() {
        given: "an existing scenario"
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                associateWithDomain(domain, "WorstCase", "NEW")
            })
        }

        when: "inspections are performed on the scenario"
        def result = parseJson(get("/scenarios/${scenario.idAsString}/inspection?domain=${domain.idAsString}"))

        then: "there are no results"
        result.empty
    }
}
