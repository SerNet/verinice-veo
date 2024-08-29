/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
import org.veo.core.entity.Person
import org.veo.core.entity.Unit
import org.veo.core.repository.ScenarioRepository
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Integration test for the unit controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class ScopeControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private AssetRepositoryImpl assetRepository

    @Autowired
    private ScenarioRepository scenarioRepository

    @Autowired
    private ScopeRepositoryImpl scopeRepository

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

            unit2 = newUnit(client) {
                name = "Test unit"
            }

            clientRepository.save(client)
            unitRepository.save(unit)
            unitRepository.save(unit2)
        }
    }

    @WithUserDetails("user@domain.example")
    def "create a scope"() {
        given: "a request body"
        Map request = [
            name: 'My Scope',
            owner: [
                targetUri: "http://localhost/units/${unit.idAsString}"
            ]
        ]

        when: "the request is sent"
        def result = parseJson(post('/scopes', request))

        then: "the location of the new scope is returned"
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Scope created successfully.'
    }

    @WithUserDetails("user@domain.example")
    def "create a scope with members"() {
        given: "an exsting asset and a request body"
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = 'Test asset'
                designator = 'AST-1'
            })
        }
        Map request = [
            name: 'My Assets',
            owner: [
                targetUri: "http://localhost/units/${unit.idAsString}"
            ],
            members: [
                [targetUri : "http://localhost/assets/${asset.idAsString}"]
            ]
        ]

        when: "the request is sent"
        def result = parseJson(post('/scopes', request))

        then: "the location of the new scope is returned"
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Scope created successfully.'

        when: "the server is queried for the scope"
        result = parseJson(get("/scopes/${resourceId}"))

        then: "the expected name is present"
        result.name == 'My Assets'

        and: "the scope contains the asset"
        result.members.size() == 1
        result.members.first().displayName == 'AST-1 Test asset'
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a scope with members"() {
        given: "a saved scope with a composite with two parts"
        Person p1 = newPerson(unit) {
            name = "p1"
        }
        Person p2 = newPerson(unit) {
            name = "p2"
        }

        def composite = newPerson(unit) {
            name = 'Composite person'
            parts = [p1, p2]
            designator = 'PER-1'
        }

        def scope = txTemplate.execute {
            scopeRepository.save(newScope(unit) {
                name = 'Test scope'
                members = [composite]
            })
        }

        when: "the server is queried for the scope"
        def result = parseJson(get("/scopes/${scope.idAsString}"))

        then: "the scope is found"
        result._self == "http://localhost/scopes/${scope.idAsString}"
        result.name == 'Test scope'
        result.owner.targetUri == "http://localhost/units/${unit.idAsString}"

        and: "is has the correct members"
        result.members.size() == 1
        result.members.first().displayName == 'PER-1 Composite person'
    }

    @WithUserDetails("user@domain.example")
    def "query and modify a scope's members"() {
        given: "a saved scope and two composites"
        def (asset, scenario,scope ) = txTemplate.execute {

            def asset = assetRepository.save(newAsset(unit) {
                name = 'Test asset'
            })
            def scenario = scenarioRepository.save(newScenario(unit) {
                name = 'Test scenario'
            })
            def scope = scopeRepository.save(newScope(unit) {
                name = 'Test scope'
            })
            [
                asset,
                scenario,
                scope
            ]
        }

        when: "the server is queried for the scope"
        def result = parseJson(get("/scopes/${scope.idAsString}"))

        then: "the scope is found"
        result.name == 'Test scope'
        result.members.empty

        when: "the server is queried for the scope's members"
        def membersResult = parseJson(get("/scopes/${scope.idAsString}/members"))

        then:
        membersResult.empty

        when: "Updating the scope's members"
        result.members = [
            [targetUri : "http://localhost/assets/${asset.idAsString}"],
            [targetUri : "http://localhost/scenarios/${scenario.idAsString}"]
        ]
        put("/scopes/${scope.idAsString}", result, [
            "If-Match": getETag(get("/scopes/${scope.idAsString}"))
        ])

        then:
        txTemplate.execute { scopeRepository.findById(scope.id).get().members.size() }  == 2

        when: "querying the members again"
        def members = parseJson(get("/scopes/${scope.idAsString}/members"))

        then:
        with(members.sort{it.name}) {
            it.size() == 2
            it[0].name == "Test asset"
            it[0].type == "asset"
            it[1].name == "Test scenario"
            it[1].type == "scenario"
        }
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all scopes for a client"() {
        given: "two saved scopes"
        def scope1 = newScope(unit) {
            name = 'Test scope 1'
        }
        def scope2 = newScope(unit) {
            name = 'Test scope 2'
        }

        (scope1, scope2) =
                txTemplate.execute {
                    [scope1, scope2].collect(scopeRepository.&save)
                }

        when: "the server is queried for the scopes"
        def result = parseJson(get("/scopes"))

        then: "the scopes are returned"
        result.items*.name.sort() == [
            'Test scope 1',
            'Test scope 2'
        ]
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all scopes for a unit"() {
        given: "two saved scopes from different units"
        def scope1 = newScope(unit) {
            name = 'Test scope 1'
        }
        def scope2 = newScope(unit2) {
            name = 'Test scope 2'
        }

        (scope1, scope2) =
                txTemplate.execute {
                    [scope1, scope2].collect(assetRepository.&save)
                }

        when: "a request is made to the server for all scopes of a unit"
        def result = parseJson(get("/scopes?unit=${unit.idAsString}"))

        then: "the respective scope is returned"
        result.items*.name == ['Test scope 1']

        when: "a request is made to the server for all scopes of another unit"
        result = parseJson(get("/scopes?unit=${unit2.idAsString}"))

        then: "the respective scope is returned"
        result.items*.name == ['Test scope 2']
    }

    @WithUserDetails("user@domain.example")
    def "put a scope with a custom aspect"() {
        given: "a saved scope"
        def scope = txTemplate.execute {
            scopeRepository.save(newScope(unit) {
                associateWithDomain(dsgvoDomain, "SCP_Scope", "NEW")
                applyCustomAspect(newCustomAspect("scope_generalInformation", dsgvoDomain))
            })
        }

        Map request = [
            name: 'New scope 2',
            abbreviation: 's-2',
            description: 'desc',
            owner:
            [
                targetUri: "http://localhost/units/${unit.idAsString}",
                displayName: 'test unit'
            ], domains: [
                (dsgvoDomain.idAsString): [
                    subType: "SCP_Scope",
                    status: "NEW",
                ]
            ], customAspects:
            [
                'scope_address' :
                [
                    domains: [],
                    attributes:  [
                        scope_address_postcode: '1',
                        scope_address_country: 'Germany'
                    ]
                ]
            ]
        ]

        when: "the new scope data is sent to the server"
        Map headers = [
            'If-Match': ETag.from(scope.idAsString, scope.version)
        ]
        def result = parseJson(put("/scopes/${scope.idAsString}",request, headers))

        then: "the scope is found"
        result.name == 'New scope 2'
        result.abbreviation == 's-2'
        result.domains[dsgvoDomain.idAsString] == [
            subType: "SCP_Scope",
            status: "NEW",
            decisionResults: [:],
            riskValues:[:]
        ]
        result.owner.targetUri == "http://localhost/units/${unit.idAsString}"

        when:
        def entity = txTemplate.execute {
            scopeRepository.findById(scope.id).get().tap() {
                // resolve proxy:
                customAspects.first()
            }
        }

        then:
        entity.name == 'New scope 2'
        entity.abbreviation == 's-2'
        with(entity.customAspects.first()) {
            type == 'scope_address'
            attributes["scope_address_postcode"] == '1'
            attributes["scope_address_country"] == 'Germany'
        }
    }

    @WithUserDetails("user@domain.example")
    def "can't put a scope with another scope ID"() {
        given: "two scopes"
        def scope1 = txTemplate.execute({
            scopeRepository.save(newScope(unit))
        })
        def scope2 = txTemplate.execute({
            scopeRepository.save(newScope(unit))
        })

        when: "a put request tries to update scope 1 using the ID of scope 2"
        Map headers = [
            'If-Match': ETag.from(scope2.idAsString, 1)
        ]
        put("/scopes/${scope2.idAsString}", [
            id: scope1.idAsString,
            name: "new name 1",
            owner: [targetUri: 'http://localhost/units/' + unit.idAsString]
        ], headers, 400)

        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }

    @WithUserDetails("user@domain.example")
    def "can put back scope"() {
        given: "a new scope"
        def id = parseJson(post("/scopes", [
            name: 'My Scope',
            owner: [targetUri: "http://localhost/units/"+unit.idAsString]
        ])).resourceId
        def getResult = get("/scopes/$id")

        when: "putting the retrieved scope back to be successful"
        def resultActions = put("/scopes/$id", parseJson(getResult), [
            "If-Match": ETag.from(id, 0)
        ])

        then: 'the ETag is returned'
        getETag(resultActions) == ETag.from(id, 1)
    }

    @WithUserDetails("user@domain.example")
    def "can put back scope with members"() {
        given: "a saved asset and scope"
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = 'Test asset'
            })
        }
        Map request = [
            name: 'My Assets',
            owner: [
                targetUri: "http://localhost/units/${unit.idAsString}"
            ],
            members: [
                [targetUri : "http://localhost/assets/${asset.idAsString}"]
            ]
        ]

        def id = parseJson(post("/scopes", request)).resourceId
        def getResult = get("/scopes/$id")

        expect: "putting the retrieved scope back to be successful"
        put("/scopes/$id", parseJson(getResult), [
            "If-Match": getETag(getResult)
        ])
    }

    @WithUserDetails("user@domain.example")
    def "removing an entity that is a member of a scope does not remove the scope"() {
        given: "a saved scope with two members"
        def (asset, scope) = txTemplate.execute {
            def asset = assetRepository.save(newAsset(unit) {
                name = 'Test asset'
            })
            def scenario = scenarioRepository.save(newScenario(unit) {
                name = 'Test scenario'
                designator = 'SCN-1'
            })
            def scope = scopeRepository.save(newScope(unit) {
                name = 'Test scope'
                members << asset << scenario
            })
            [
                asset,
                scope
            ]
        }

        when: "the asset is deleted"
        delete("/assets/${asset.idAsString}")

        and: "the server is queried for the scope"
        def members = parseJson(get("/scopes/${scope.idAsString}")).members

        then: "the scope is found"
        members.size() == 1
        members.first().displayName == "SCN-1 Test scenario"
    }

    @WithUserDetails("user@domain.example")
    def "deleting scope leaves members and containers intact"() {
        given: "Scope a, b, and c where a ∈ b, a ∈ c, b ∈ c"
        def (a,b,c) = txTemplate.execute {
            def a = scopeRepository.save(newScope(unit))
            def b = scopeRepository.save(newScope(unit) {
                members << a
            })
            def c = scopeRepository.save(newScope(unit) {
                members << b << a
            })
            [a, b, c]
        }

        when: "the server is asked to delete b"
        delete("/scopes/${b.idAsString}")

        then: "b is deleted"
        scopeRepository.findById(b.id).empty

        and: "a and c are left intact"
        scopeRepository.findById(a.id).with {
            it.present
            it.get().id == a.id
        }
        txTemplate.execute {
            scopeRepository.findById(c.id).with {
                it.present
                it.get().with {
                    id == c.id
                    members.size() == 1
                    members.first().id == a.id
                }
            }
        }
    }
}
