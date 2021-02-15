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

import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.util.NestedServletException

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Asset
import org.veo.core.entity.CustomLink
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ScenarioData
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
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
    TransactionTemplate txTemplate

    @Autowired
    private EntityDataFactory entityFactory

    private Unit unit
    private Unit unit2
    private Domain domain
    private Domain domain1
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
    String salt = "salt-for-etag"

    def setup() {
        txTemplate.execute {
            domain = newDomain {
                description = "ISO/IEC"
                abbreviation = "ISO"
                name = "ISO"
            }

            domain1 = newDomain {
                description = "ISO/IEC2"
                abbreviation = "ISO"
                name = "ISO"
            }

            def client= newClient {
                id = clientId
                domains = [domain, domain1] as Set
            }

            unit = newUnit(client) {
                name = "Test unit"
            }

            unit2 = newUnit(client) {
                name = "Unit2"
            }

            clientRepository.save(client)
            unitRepository.save(unit)
            unitRepository.save(unit2)
        }
        ETag.setSalt(salt)
    }

    @WithUserDetails("user@domain.example")
    def "create an asset"() {
        given: "a request body"

        Map request = [
            name: 'New Asset',
            owner: [
                displayName: 'test2',
                targetUri: '/units/' + unit.id.uuidValue()
            ]
        ]

        when: "a request is made to the server"

        def results = post('/assets', request)

        then: "the asset is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new asset is returned"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
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
            })
        }
        Map request = [
            name: 'My Assets',
            owner: [
                targetUri: "/units/${unit.id.uuidValue()}"
            ],
            parts: [
                [targetUri : "http://localhost/assets/${asset.id.uuidValue()}"]]
        ]

        when: "the request is sent"

        def results = post('/assets', request)

        then: "the composite asset is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new composite asset is returned"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Asset created successfully.'
        when: "the server is queried for the asset"
        results = get("/assets/${resourceId}")

        then: "the asset is found"
        results.andExpect(status().isOk())
        when: "the returned content is parsed"
        result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then: "the expected name is present"
        result.name == 'My Assets'
        and: "the composite asset contains the other asset"
        result.parts.size() == 1
        result.parts.first().displayName == '- Test asset (Test unit)'
    }


    @WithUserDetails("user@domain.example")
    def "retrieve an asset"() {
        given: "a saved asset"

        CustomProperties simpleProps = entityFactory.createCustomProperties()
        simpleProps.setType("simpleAspect")
        simpleProps.setProperty("simpleProp", "simpleValue")

        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = 'Test asset-1'
                customAspects = [simpleProps] as Set
                createdBy = "me"
                createdAt = Instant.parse("2020-09-01T00:00:00Z")
                updatedBy = "you"
                updatedAt = Instant.parse("2020-09-02T00:00:00Z")
            })
        }

        when: "a GET request is made to the server"
        def results = get("/assets/${asset.id.uuidValue()}")
        String expectedETag = DigestUtils.sha256Hex(asset.id.uuidValue() + "_" + salt + "_" + Long.toString(asset.getVersion()))

        then: "the asset is found"
        results.andExpect(status().isOk())
        and: "the eTag is set"
        String eTag = results.andReturn().response.getHeader("ETag")
        eTag != null
        getTextBetweenQuotes(eTag).equals(expectedETag)
        and: "the response contains the expected data"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result == [
            customAspects:[
                simpleAspect:[
                    applicableTo:[],
                    attributes:[
                        simpleProp:'simpleValue'
                    ],
                    domains:[]]
            ],
            domains:[],
            id: asset.id.uuidValue(),
            links:[:],
            name:'Test asset-1',
            owner:[
                displayName:'Test unit',
                targetUri   : "http://localhost/units/${unit.id.uuidValue()}",
                searchesUri : "http://localhost/units/searches",
                resourcesUri: "http://localhost/units{?parent,displayName}"
            ],
            subType: [:],
            parts: [],
            createdBy: "me",
            createdAt: "2020-09-01T00:00:00Z",
            updatedBy: "you",
            updatedAt: "2020-09-02T00:00:00Z"
        ]
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
        CustomProperties simpleProps = entityFactory.createCustomProperties()
        simpleProps.setType("simpleAspect")
        simpleProps.setProperty("simpleProp", "simpleValue")

        def asset = newAsset(unit) {
        }
        asset.id = Key.newUuid()
        asset.name = 'Test asset-1'
        asset.owner = unit
        asset.setCustomAspects([simpleProps] as Set)

        asset = txTemplate.execute {
            assetRepository.save(asset)
        }

        when: "a search request is made to the server"
        def postSearchResult = post('http://localhost/assets/searches', search)

        then: "the server redirects to the created search resource"
        postSearchResult.andExpect(status().isCreated())
        def postSearchResponse = parseJson(postSearchResult)

        when: "the search is run"
        def getSearchResult = get(postSearchResponse.searchUrl)

        then: "the response contains the expected data"
        def getSearchResponse = parseJson(getSearchResult)
        getSearchResponse.size == 1
        getSearchResponse[0].id == asset.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve an asset with a link"() {
        given: "a saved asset"

        CustomProperties simpleProps = entityFactory.createCustomProperties()
        simpleProps.setType("simpleAspect")
        simpleProps.setProperty("simpleProp", "simpleValue")

        def sourceAsset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                domains = [domain1] as Set
            })
        }
        def targetAsset = newAsset(unit) {
            name = "Test asset-1"
            customAspects = [simpleProps] as Set
        }

        CustomLink link = entityFactory.createCustomLink("requires", sourceAsset, targetAsset)
        link.setType("mypreciouslink")
        link.setApplicableTo(["Asset"] as Set)
        targetAsset.links.add(link)

        targetAsset = txTemplate.execute {
            assetRepository.save(targetAsset)
        }

        when: "a request is made to the server"
        def results = get("/assets/${targetAsset.id.uuidValue()}")
        String expectedETag = DigestUtils.sha256Hex(targetAsset.id.uuidValue() + "_" + salt + "_" + Long.toString(targetAsset.getVersion()))
        then: "the asset is found"
        results.andExpect(status().isOk())
        and:
        String eTag = results.andReturn().response.getHeader("ETag")
        eTag != null
        getTextBetweenQuotes(eTag).equals(expectedETag)
        and: "the response contains the expected link"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test asset-1'
        result.links.size() == 1
        result.links.mypreciouslink.target.targetUri == [
            "http://localhost/assets/${sourceAsset.id.uuidValue()}"
        ]
        result.links.mypreciouslink.applicableTo[0] == ['Asset']

        when: "all assets are queried"
        def allAssets = parseJson(get("/assets"))
        then: "the asset with the link is retrieved"
        allAssets.sort{it.name}.first() == result
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all assets for a client"() {
        given: "a saved asset"

        def asset = newAsset(unit) {
            name = "Test asset-1"
            domains = [domain1] as Set
        }

        def asset2 = newAsset(unit2) {
            name = "Test asset-2"
            domains = [domain1] as Set
        }

        (asset, asset2) = txTemplate.execute {
            [asset, asset2].collect(assetRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/assets")

        then: "the assets are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2
        result.sort{it.name}.first().name == 'Test asset-1'
        result.sort{it.name}.first().owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()

        result.sort{it.name}[1].name == 'Test asset-2'
        result.sort{it.name}[1].owner.targetUri == "http://localhost/units/"+unit2.id.uuidValue()
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

        when: "a request is made to the server"
        def results = get("/assets?unit=${unit.id.uuidValue()}")

        then: "the assets are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 1

        result.first().name == 'Test asset-1'
        result.first().owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()

        when: "a request is made to the server"
        results = get("/assets?unit=${unit2.id.uuidValue()}")

        then: "the assets are returned"
        results.andExpect(status().isOk())
        when:
        result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 1

        result.first().name == 'Test asset-2'
        result.first().owner.targetUri == "http://localhost/units/"+unit2.id.uuidValue()
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
        def results = get("/assets?unit=${unit.id.uuidValue()}")
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then: "both assets from the unit's hierarchy are returned"
        with(result.sort{it.name}) {
            size == 2
            it[0].name == "asset 0"
            it[1].name == "asset 1"
        }
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
        with(result) {
            size == 1
            it[0].name == "asset 1"
        }
    }

    @WithUserDetails("user@domain.example")
    def "put an asset"() {
        given: "a saved asset"

        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = 'New asset-2'
                domains = [domain1] as Set
            })
        }

        Map request = [
            name: 'New asset-2',
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
            'If-Match': ETag.from(asset.id.uuidValue(), 1)
        ]
        def results = put("/assets/${asset.id.uuidValue()}", request, headers)

        then: "the asset is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New asset-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put an asset with custom properties"() {
        given: "a saved asset"

        CustomProperties cp = entityFactory.createCustomProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Asset'] as Set)

        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = "Test asset-1"
                domains = [domain1] as Set
                customAspects = [cp] as Set
            })
        }

        Map request = [
            name: 'New asset-2',
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
            ],
            customAspects:
            [
                'AssetCommons' :
                [
                    applicableTo: [
                        "Asset"
                    ],
                    domains: [],
                    attributes:  [
                        assetNum: '001',
                        assetPlatform: '9 3/4'
                    ]
                ]
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(asset.id.uuidValue(), 1)
        ]

        def results = put("/assets/${asset.id.uuidValue()}",request, headers)

        then: "the asset is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New asset-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
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
        entity.customAspects.first().type == 'AssetCommons'
        entity.customAspects.first().applicableTo == ['Asset'] as Set
        entity.customAspects.first().stringProperties.assetNum == '001'
        entity.customAspects.first().stringProperties.assetPlatform == '9 3/4'
    }

    @WithUserDetails("user@domain.example")
    def "delete an asset"() {

        given: "an existing asset"
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = "Test asset-2"
                domains = [domain1] as Set
            })
        }

        when: "a delete request is sent to the server"

        def results = delete("/assets/${asset.id.uuidValue()}")
        then: "the asset is deleted"
        results.andExpect(status().isOk())
        assetRepository.findById(asset.id).empty
    }

    @WithUserDetails("user@domain.example")
    def "delete an asset that is a link target"() {

        given: "two assets with a link between them"

        def targetAsset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                domains = [domain1] as Set
            })
        }

        def sourceAsset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                domains = [domain1] as Set
            })
        }
        CustomLink link = entityFactory.createCustomLink("requires", targetAsset, sourceAsset)
        sourceAsset.links =[link] as Set

        when: "a delete request is sent to the server for link target"

        def results = delete("/assets/${targetAsset.id.uuidValue()}")
        then: "the asset is deleted"
        results.andExpect(status().isOk())
        assetRepository.findById(targetAsset.id).empty
    }

    @WithUserDetails("user@domain.example")
    def "deleting a composite asset does not delete its parts"() {

        given: "an asset and a composite that contains it"
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit))
        }
        def composite = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                parts = [asset]
            })
        }
        when: "a delete request is sent to the server"

        def results = delete("/assets/${composite.id.uuidValue()}")

        then: "the composite is deleted"
        results.andExpect(status().isOk())
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
            owner: [targetUri: '/units/' + unit.id.uuidValue()]
        ], headers, false)
        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }

    @WithUserDetails("user@domain.example")
    def "can put back asset"() {
        given: "a new asset"
        def id = parseJson(post("/assets/", [
            name: "new name",
            owner: [targetUri: "/units/"+unit.id.uuidValue()]
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
                targetUri: "/units/${unit.id.uuidValue()}"
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
    def "A risk can be created for an asset"() {
        given: "saved entities"
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = 'New asset-2'
                domains = [domain1] as Set
            })
        }
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                domains = [domain1] as Set
            })
        }

        when: "a new risk can be created successfully"
        def result= post("/assets/"+asset.id.uuidValue()+"/risks", [
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
        getResult.with {
            it.asset.targetUri ==~ /.*${asset.id.uuidValue()}.*/
            it.scenario.targetUri ==~ /.*${scenario.id.uuidValue()}.*/
            it.scenario.targetUri ==~ /.*${postResult.resourceId}.*/
            it.domains.first().displayName == this.domain1.displayName
            it._self ==~ /.*assets\/${asset.id.uuidValue()}\/risks\/${scenario.id.uuidValue()}.*/
            Instant.parse(it.createdAt) > beforeCreation
            Instant.parse(it.updatedAt) > beforeCreation
        }
    }

    @WithUserDetails("user@domain.example")
    def "A list of risks can be retrieved for an asset"() {
        given: "An asset with multiple risks"
        def (Asset asset, ScenarioData scenario, Object postResult) = createRisk()
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
        post("/assets/"+asset.id.uuidValue()+"/risks", [
            scenario: [ targetUri: '/scenarios/'+ scenario2.id.uuidValue() ],
            domains: [
                [targetUri: '/domains/'+ domain1.id.uuidValue() ] ]
        ] as Map)
        post("/assets/"+asset.id.uuidValue()+"/risks", [
            scenario: [ targetUri: '/scenarios/'+ scenario3.id.uuidValue() ],
            domains: [
                [targetUri: '/domains/'+ domain1.id.uuidValue() ] ]
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
        def result = delete("/assets/${asset.id.uuidValue()}/risks/${scenario.id.uuidValue()}", true)

        then: "the risk has been removed"
        result.andExpect(status().isOk())
        assetRepository.findByRisk(scenario).isEmpty()

        and: "all referenced objects are still present"
        assetRepository.findById(asset.id).isPresent()
        scenarioRepository.findById(scenario.id).isPresent()
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be updated with new information"() {
        given: "an asset risk and additional entities"
        def beforeCreation = Instant.now()
        def (Asset asset, ScenarioData scenario, Object postResult) = createRisk()

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
        def getResponse = get("/assets/" + asset.id.uuidValue() + "/risks/" + scenario.id.uuidValue(),
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
            it.domains.first().displayName == this.domain1.displayName
            it._self ==~ /.*assets\/${asset.id.uuidValue()}\/risks\/${scenario.id.uuidValue()}.*/
            Instant.parse(it.createdAt) > beforeCreation
            Instant.parse(it.createdAt) < beforeUpdate
            Instant.parse(it.updatedAt) > beforeUpdate
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
                domains = [domain1] as Set
            })
        }
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                domains = [domain1] as Set
            })
        }
        def postResult = parseJson(
                post("/assets/" + asset.id.uuidValue() + "/risks", [
                    scenario: [targetUri: '/scenarios/' + scenario.id.uuidValue()],
                    domains : [
                        [targetUri: '/domains/' + domain1.id.uuidValue()]]
                ]))
        return [asset, scenario, postResult]
    }
}
