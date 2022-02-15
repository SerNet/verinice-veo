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

import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.util.NestedServletException

import org.veo.adapter.presenter.api.DeviatingIdException
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Asset
import org.veo.core.entity.CustomAspect
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ScenarioData

/**
 * Integration test for the unit controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class AssetControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private AssetRepositoryImpl assetRepository

    @Autowired
    private ScenarioRepositoryImpl scenarioRepository

    @Autowired
    private PersonRepositoryImpl personRepository

    @Autowired
    private ControlRepositoryImpl controlRepository

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

            unit = unitRepository.save(newUnit(client) {
                name = "Test unit"
            })

            unit2 = unitRepository.save(newUnit(client) {
                name = "Unit2"
            })
        }
    }

    @WithUserDetails("user@domain.example")
    def "create an asset"() {
        given: "a request body"

        Map request = [
            name: 'New Asset',
            owner: [
                displayName: 'test2',
                targetUri: 'http://localhost/units/' + unit.id.uuidValue()
            ]
        ]

        when: "a request is made to the server"

        def result = parseJson(post('/assets', request))

        then: "the location of the new asset is returned"
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Asset created successfully.'
    }


    @WithUserDetails("user@domain.example")
    def "create a composite asset with parts"() {
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
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ],
            parts: [
                [targetUri : "http://localhost/assets/${asset.id.uuidValue()}"]
            ]
        ]

        when: "the request is sent"
        def result = parseJson(post('/assets', request))
        then: "the location of the new composite asset is returned"
        result.success
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Asset created successfully.'
        when: "the server is queried for the asset"
        result = parseJson(get("/assets/${resourceId}"))
        then: "the expected name is present"
        result.name == 'My Assets'
        and: "the composite asset contains the other asset"
        result.parts.size() == 1
        result.parts.first().displayName == 'AST-1 Test asset'
    }


    @WithUserDetails("user@domain.example")
    def "retrieve an asset"() {
        given: "a saved asset"

        CustomAspect simpleProps = newCustomAspect("simpleAspect") {
            attributes = [
                "simpleProp": "simpleValue"
            ]
        }

        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = 'Test asset-1'
                customAspects = [simpleProps] as Set
            })
        }

        when: "a GET request is made to the server"
        def results = get("/assets/${asset.id.uuidValue()}")

        then: "the eTag is set"
        String eTag = getETag(results)
        eTag != null
        and: "the caching headers are set"
        def cacheControl = results.andReturn().response.getHeader(HttpHeaders.CACHE_CONTROL)
        cacheControl == 'no-cache'
        and: "the response contains the expected data"
        def result = parseJson(results)
        result == [
            _self: "http://localhost/assets/${asset.id.uuidValue()}",
            customAspects:[
                simpleAspect:[
                    attributes:[
                        simpleProp:'simpleValue'
                    ],
                    domains:[]]
            ],
            designator: asset.designator,
            domains:[:],
            id: asset.id.uuidValue(),
            links:[:],
            name:'Test asset-1',
            owner:[
                displayName:'Test unit',
                targetUri   : "http://localhost/units/${unit.id.uuidValue()}",
                searchesUri : "http://localhost/units/searches",
                resourcesUri: "http://localhost/units{?parent,displayName}"
            ],
            type: 'asset',
            parts: [],
            createdBy: "user@domain.example",
            createdAt: asset.createdAt.toString(),
            updatedBy: "user@domain.example",
            updatedAt: asset.updatedAt.toString()
        ]
        when: "the asset is requested from the server again"
        results =
                mvc.perform(MockMvcRequestBuilders.get("/assets/${asset.id.uuidValue()}").accept(MediaType.APPLICATION_JSON).header(
                HttpHeaders.IF_NONE_MATCH, '"'+eTag+'"'
                ))
        then: "the server returns not-modified"
        results.andReturn().response.status == HttpStatus.SC_NOT_MODIFIED
    }


    @WithUserDetails("user@domain.example")
    def "search an asset"() {
        given: "a search request body"
        Map search = [
            unitId: [
                values: [
                    unit.id.uuidValue()
                ]
            ],
        ]

        and: "a saved asset"
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit))
        }

        when: "a search request is made to the server"
        def postSearchResponse = parseJson(post('http://localhost/assets/searches', search))

        and: "the search is run"
        def searchResult = parseJson(get(new URI(postSearchResponse.searchUrl)))

        then: "the response contains the expected data"
        searchResult.items*.id == [asset.id.uuidValue()]
    }

    @WithUserDetails("user@domain.example")
    def "retrieve an asset with a link"() {
        given: "a saved asset"
        def targetAsset = txTemplate.execute {
            assetRepository.save(newAsset(unit))
        }
        def sourceAsset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                domains = [dsgvoDomain] as Set
                links = [
                    newCustomLink(targetAsset, "mypreciouslink")
                ]
                name = "Test asset-1"
            })
        }

        when: "a request is made to the server"
        def results = get("/assets/${sourceAsset.id.uuidValue()}")
        then: "the asset is found"
        getETag(results) != null
        and: "the response contains the expected link"
        def result = parseJson(results)
        result.name == 'Test asset-1'
        result.links.size() == 1
        result.links.mypreciouslink.target.targetUri == [
            "http://localhost/assets/${targetAsset.id.uuidValue()}"
        ]

        when: "all assets are queried"
        def allAssets = parseJson(get("/assets"))
        then: "the asset with the link is retrieved"
        allAssets.items.sort{it.name}.first() == result
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all assets for a client"() {
        given: "a saved asset"

        def asset = newAsset(unit) {
            name = "Test asset-1"
            domains = [dsgvoDomain] as Set
        }

        def asset2 = newAsset(unit2) {
            name = "Test asset-2"
            domains = [dsgvoDomain] as Set
        }

        (asset, asset2) = txTemplate.execute {
            [asset, asset2].collect(assetRepository.&save)
        }

        when: "all assets are requested"
        def result = parseJson(get("/assets"))

        then: "the assets are returned"
        def sortedItems = result.items.sort { it.name }
        sortedItems.size == 2
        sortedItems[0].name == 'Test asset-1'
        sortedItems[0].owner.targetUri == "http://localhost/units/" + unit.id.uuidValue()
        sortedItems[1].name == 'Test asset-2'
        sortedItems[1].owner.targetUri == "http://localhost/units/" + unit2.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all assets for a unit"() {
        given: "a saved asset"

        def asset = newAsset(unit) {
            name = "Test asset-1"
        }

        def asset2 = newAsset(unit2) {
            name = "Test asset-2"
        }

        (asset, asset2) = txTemplate.execute {
            [asset, asset2].collect(assetRepository.&save)
        }

        when: "all assets in the first unit is queried"
        def result = parseJson(get("/assets?unit=${unit.id.uuidValue()}"))
        then: "asset 1 is returned"
        result.items*.name == ['Test asset-1']

        when: "a request is made to the server"
        result = parseJson(get("/assets?unit=${unit2.id.uuidValue()}"))
        then: "the asset of unit 2 is returned"
        result.items*.name == ['Test asset-2']
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all assets for a unit recursively"() {
        given: "A sub unit and a sub sub unit with one asset each"

        def subUnit = unitRepository.save(newUnit(unit.client) {
            parent = unit
        })
        def subSubUnit = unitRepository.save(newUnit(unit.client) {
            parent = subUnit
        })

        assetRepository.save(newAsset(subUnit) {
            name = "asset 0"
        })
        assetRepository.save(newAsset(subSubUnit) {
            name = "asset 1"
        })

        when: "all assets for the root unit are queried"
        def result = parseJson(get("/assets?unit=${unit.id.uuidValue()}"))
        then: "both assets from the unit's hierarchy are returned"
        result.items*.name.sort() == ['asset 0', 'asset 1']
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all assets for a unit filtering by displayName"() {
        given: "A sub unit and a sub sub unit with one asset each"

        def subUnit = unitRepository.save(newUnit(unit.client) {
            parent = unit
        })
        def subSubUnit = unitRepository.save(newUnit(unit.client) {
            parent = subUnit
        })

        assetRepository.save(newAsset( subUnit) {
            name = "asset 0"
        })
        assetRepository.save(newAsset( subSubUnit) {
            name = "asset 1"
        })

        when: "all assets for the root unit matching the filter"
        def result = parseJson(get("/assets?unit=${unit.id.uuidValue()}&displayName=sset 1"))
        then: "only the matching asset from the unit's hierarchy is returned"
        result.items*.name == ["asset 1"]
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all assets for a unit filtering by displayName with special characters"() {
        given: "A sub unit and a sub sub unit with one asset each"

        def subUnit = unitRepository.save(newUnit(unit.client) {
            parent = unit
        })
        def subSubUnit = unitRepository.save(newUnit(unit.client) {
            parent = subUnit
        })

        assetRepository.save(newAsset( subUnit) {
            name = "Fußballverein Äächen 0"
        })
        assetRepository.save(newAsset( subSubUnit) {
            name = "Fußballverein Äächen 1"
        })

        when: "all assets for the root unit matching the filter"
        def result = parseJson(get("/assets?unit=${unit.id.uuidValue()}&displayName=ballverein Äächen 1"))
        then: "only the matching asset from the unit's hierarchy is returned"
        result.items*.name == ["Fußballverein Äächen 1"]
    }

    @WithUserDetails("user@domain.example")
    def "put an asset"() {
        given: "a saved asset"

        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = 'New asset-2'
                domains = [dsgvoDomain] as Set
            })
        }

        Map request = [
            name: 'New asset-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: 'http://localhost/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ],
            domains: [
                (dsgvoDomain.id.uuidValue()): [:]
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(asset.id.uuidValue(), asset.version)
        ]
        def result = parseJson(put("/assets/${asset.id.uuidValue()}", request, headers))

        then: "the asset is found"
        result.name == 'New asset-2'
        result.abbreviation == 'u-2'
        result.domains[dsgvoDomain.id.uuidValue()] == [:]
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put an asset with a custom aspect"() {
        given: "a saved asset"

        CustomAspect customAspect = newCustomAspect("my.new.type")

        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = "Test asset-1"
                domains = [dsgvoDomain] as Set
                customAspects = [customAspect] as Set
            })
        }

        Map request = [
            name: 'New asset-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: 'http://localhost/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ],
            domains: [
                (dsgvoDomain.id.uuidValue()): [:]
            ],
            customAspects:
            [
                'asset_details' :
                [
                    domains: [],
                    attributes:  [
                        asset_details_number: 1,
                        asset_details_operatingStage: 'asset_details_operatingStage_planning'
                    ]
                ]
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(asset.id.uuidValue(), asset.version)
        ]
        def result = parseJson(put("/assets/${asset.id.uuidValue()}",request, headers))

        then: "the asset is found"
        result.name == 'New asset-2'
        result.abbreviation == 'u-2'
        result.domains[dsgvoDomain.id.uuidValue()] == [:]
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()

        when:
        def entity = txTemplate.execute {
            assetRepository.findById(asset.id).get().tap {
                // make sure that the proxy is resolved
                customAspects.first()
            }
        }

        then:
        entity.name == 'New asset-2'
        entity.abbreviation == 'u-2'
        with(entity.customAspects.first()) {
            type == 'asset_details'
            attributes["asset_details_number"] == 1
            attributes['asset_details_operatingStage'] == 'asset_details_operatingStage_planning'
        }
    }

    @WithUserDetails("user@domain.example")
    def "delete an asset"() {

        given: "an existing asset"
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = "Test asset-2"
                domains = [dsgvoDomain] as Set
            })
        }

        when: "a delete request is sent to the server"

        delete("/assets/${asset.id.uuidValue()}")
        then: "the asset is deleted"
        assetRepository.findById(asset.id).empty
    }

    @WithUserDetails("user@domain.example")
    def "delete an asset that is a link target"() {
        given: "two assets with a link between them"

        def targetAsset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                domains = [dsgvoDomain] as Set
            })
        }

        def sourceAsset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                domains = [dsgvoDomain] as Set
            })
        }
        CustomLink link = newCustomLink(targetAsset, "goodLink")
        sourceAsset.links =[link] as Set

        when: "a delete request is sent to the server for link target"
        delete("/assets/${targetAsset.id.uuidValue()}")

        then: "the asset is deleted"
        assetRepository.findById(targetAsset.id).empty
    }

    @WithUserDetails("user@domain.example")
    def "deleting a composite asset does not delete its parts"() {

        given: "an asset and a composite that contains it"
        def (asset, composite) = txTemplate.execute {
            def asset = assetRepository.save(newAsset(unit))
            def composite = assetRepository.save(newAsset(unit) {
                parts = [asset]
            })
            [asset, composite]
        }
        when: "a delete request is sent to the server"
        delete("/assets/${composite.id.uuidValue()}")

        then: "the composite is deleted"
        assetRepository.findById(composite.id).empty
        and: "the asset is not deleted"
        !assetRepository.findById(asset.id).empty
    }

    @WithUserDetails("user@domain.example")
    def "can't put an asset with another asset's ID"() {
        given: "two assets"
        def asset1 = txTemplate.execute({
            assetRepository.save(newAsset(unit, {
                name = "old name 1"
            }))
        })
        def asset2 = txTemplate.execute({
            assetRepository.save(newAsset(unit, {
                name = "old name 2"
            }))
        })
        when: "a put request tries to update asset 1 using the ID of asset 2"
        Map headers = [
            'If-Match': ETag.from(asset1.id.uuidValue(), 1)
        ]
        put("/assets/${asset2.id.uuidValue()}", [
            id: asset1.id.uuidValue(),
            name: "new name 1",
            owner: [targetUri: 'http://localhost/units/' + unit.id.uuidValue()]
        ], headers, false)
        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }

    @WithUserDetails("user@domain.example")
    def "can put back asset"() {
        given: "a new asset"
        def id = parseJson(post("/assets/", [
            name: "new name",
            owner: [targetUri: "http://localhost/units/"+unit.id.uuidValue()]
        ])).resourceId
        def getResult = get("/assets/$id")

        expect: "putting the retrieved asset back to be successful"
        put("/assets/$id", parseJson(getResult), [
            "If-Match": getTextBetweenQuotes(getResult.andReturn().response.getHeader("ETag"))
        ])
    }

    @WithUserDetails("user@domain.example")
    def "can put back asset with parts"() {
        given: "a saved asset and a composite"

        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = 'Test asset'
            })
        }
        Map request = [
            name: 'Composite asset',
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ],
            parts: [
                [targetUri : "http://localhost/assets/${asset.id.uuidValue()}"]
            ]
        ]


        def id = parseJson(post("/assets/", request)).resourceId
        def getResult = get("/assets/$id")

        expect: "putting the retrieved asset back to be successful"
        put("/assets/$id", parseJson(getResult), [
            "If-Match": getTextBetweenQuotes(getResult.andReturn().response.getHeader("ETag"))
        ])
    }

    @WithUserDetails("user@domain.example")
    def     "A risk can be created for an asset"() {
        given: "saved elements"
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = 'New asset-2'
                domains = [dsgvoDomain] as Set
            })
        }
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                domains = [dsgvoDomain] as Set
            })
        }

        when: "a new risk can be created successfully"
        def json = parseJson(post("/assets/"+asset.id.uuidValue()+"/risks", [
            scenario: [ targetUri: 'http://localhost/scenarios/'+ scenario.id.uuidValue() ],
            domains: [
                [targetUri: 'http://localhost/domains/'+ dsgvoDomain
                    .id.uuidValue() ]
            ]
        ]))

        then:
        with(json) {
            resourceId != null
            resourceId.length() == 36
            success
            message == "Asset risk created successfully."
        }
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be retrieved for an asset"() {
        given: "an asset risk"
        def beforeCreation = Instant.now()
        def (Asset asset, ScenarioData scenario, Object postResult) = createRisk()

        when: "the risk is requested"
        def getResult = parseJson(
                get("/assets/" + asset.id.uuidValue() + "/risks/" + scenario.id.uuidValue(),
                true)
                )

        then: "the correct object is returned"
        getResult != null
        with(getResult) {
            it.asset.targetUri ==~ /.*${asset.id.uuidValue()}.*/
            it.scenario.targetUri ==~ /.*${scenario.id.uuidValue()}.*/
            it.scenario.targetUri ==~ /.*${postResult.resourceId}.*/
            it.domains.first().displayName == this.dsgvoDomain
                    .displayName
            it._self ==~ /.*assets\/${asset.id.uuidValue()}\/risks\/${scenario.id.uuidValue()}.*/
            Instant.parse(it.createdAt) > beforeCreation
            Instant.parse(it.updatedAt) > beforeCreation
            it.createdBy == "user@domain.example"
            it.updatedBy == "user@domain.example"
        }
    }

    @WithUserDetails("user@domain.example")
    def "A list of risks can be retrieved for an asset"() {
        given: "An asset with multiple risks"
        def (Asset asset, ScenarioData scenario, Object postResult) = createRisk()
        def scenario2 = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                domains = [dsgvoDomain] as Set
            })
        }
        def scenario3 = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                domains = [dsgvoDomain] as Set
            })
        }
        post("/assets/"+asset.id.uuidValue()+"/risks", [
            scenario: [ targetUri: 'http://localhost/scenarios/'+ scenario2.id.uuidValue() ],
            domains: [
                [targetUri: 'http://localhost/domains/'+ dsgvoDomain
                    .id.uuidValue() ]
            ]
        ] as Map)
        post("/assets/"+asset.id.uuidValue()+"/risks", [
            scenario: [ targetUri: 'http://localhost/scenarios/'+ scenario3.id.uuidValue() ],
            domains: [
                [targetUri: 'http://localhost/domains/'+ dsgvoDomain
                    .id.uuidValue() ]
            ]
        ] as Map)

        when: "The risks are queried"
        def getResult = parseJson(
                get("/assets/${asset.id.uuidValue()}/risks/"))

        then: "The risks are retreived"
        getResult.size == 3
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be deleted"() {
        given: "an asset risk"
        def (Asset asset, ScenarioData scenario, Object postResult) = createRisk()

        when: "the risk is deleted"
        delete("/assets/${asset.id.uuidValue()}/risks/${scenario.id.uuidValue()}")

        then: "the risk has been removed"
        assetRepository.findByRisk(scenario).isEmpty()

        and: "all referenced objects are still present"
        assetRepository.findById(asset.id).isPresent()
        scenarioRepository.findById(scenario.id).isPresent()
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be updated with new information"() {
        given: "an asset risk and additional elements"
        def beforeCreation = Instant.now()
        def (Asset asset, ScenarioData scenario, Object postResult) = createRisk()

        def person = txTemplate.execute {
            personRepository.save(newPerson(unit) {
                name = 'New person-1'
                domains = [dsgvoDomain] as Set
            })
        }

        def control = txTemplate.execute {
            controlRepository.save(newControl(unit) {
                name = 'New control-1'
                domains = [dsgvoDomain] as Set
            })
        }

        and: "the created risk is retrieved"
        def getResponse = get("/assets/" + asset.id.uuidValue() + "/risks/" + scenario.id.uuidValue(),
                true)
        def getResult = parseJson(getResponse)
        String eTag = getResponse.andReturn().response.getHeader("ETag").replace("\"", "")


        when: "The risk is updated"
        def beforeUpdate = Instant.now()
        def putBody = getResult + [
            mitigation: [targetUri: 'http://localhost/controls/' + control.id.uuidValue()],
            riskOwner: [targetUri: 'http://localhost/persons/' + person.id.uuidValue()]
        ]
        Map headers = [
            'If-Match': eTag
        ]

        def putResult =
                put("/assets/${asset.id.uuidValue()}/risks/${scenario.id.uuidValue()}",
                putBody as Map, headers, true)

        and: "the risk is retrieved again"
        def riskJson = parseJson(
                get("/assets/" + asset.id.uuidValue() + "/risks/" + scenario.id.uuidValue(),
                true)
                )

        then: "the information was persisted"
        eTag.length() > 0
        riskJson != null
        with(riskJson) {
            it.mitigation.targetUri ==~ /.*${control.id.uuidValue()}.*/
            it.riskOwner.targetUri ==~ /.*${person.id.uuidValue()}.*/
            it.asset.targetUri ==~ /.*${asset.id.uuidValue()}.*/
            it.scenario.targetUri ==~ /.*${scenario.id.uuidValue()}.*/
            it.domains.first().displayName == this.dsgvoDomain
                    .displayName
            it._self ==~ /.*assets\/${asset.id.uuidValue()}\/risks\/${scenario.id.uuidValue()}.*/
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
                get("/assets/" + asset.id.uuidValue() + "/risks/" + scenario.id.uuidValue(),
                true)
                )

        then: "their references are removed from the risk"
        riskJson != null
        with(riskJson) {
            it._self ==~ /.*assets\/${asset.id.uuidValue()}\/risks\/${scenario.id.uuidValue()}.*/
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
        get("/assets/" + asset.id.uuidValue() + "/risks/" + scenario.id.uuidValue(),
                false)

        then: "the risk was removed as well"
        def e = thrown NestedServletException
        e.getCause() instanceof NoSuchElementException
    }

    private List createRisk() {
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                domains = [dsgvoDomain] as Set
            })
        }
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                domains = [dsgvoDomain] as Set
            })
        }
        def postResult = parseJson(
                post("/assets/" + asset.id.uuidValue() + "/risks", [
                    scenario: [targetUri: 'http://localhost/scenarios/' + scenario.id.uuidValue()],
                    domains : [
                        [targetUri: 'http://localhost/domains/' + dsgvoDomain
                            .id.uuidValue()]
                    ]
                ]))
        return [asset, scenario, postResult]
    }
}
