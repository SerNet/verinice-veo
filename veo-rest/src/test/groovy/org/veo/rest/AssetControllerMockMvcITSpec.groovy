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

import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Asset
import org.veo.core.entity.CustomLink
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
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
class AssetControllerMockMvcITSpec extends VeoRestMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private AssetRepositoryImpl assetRepository
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
            domain = entityFactory.createDomain()
            domain.description = "ISO/IEC"
            domain.abbreviation = "ISO"
            domain.name = "ISO"
            domain.id = Key.newUuid()

            domain1 = entityFactory.createDomain()
            domain1.description = "ISO/IEC2"
            domain1.abbreviation = "ISO"
            domain1.name = "ISO"
            domain1.id = Key.newUuid()

            def client= entityFactory.createClient()
            client.id = clientId
            client.domains = [domain, domain1] as Set

            unit = entityFactory.createUnit()
            unit.name = "Test unit"
            unit.id = Key.newUuid()

            unit.client = client
            unit2 = entityFactory.createUnit(Key.newUuid(),"Unit2",null)
            unit2.client = client

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
    def "retrieve an asset"() {
        given: "a saved asset"

        CustomProperties simpleProps = entityFactory.createCustomProperties()
        simpleProps.setType("simpleAspect")
        simpleProps.setProperty("simpleProp", "simpleValue")

        def asset = entityFactory.createAsset()
        asset.id = Key.newUuid()
        asset.name = 'Test asset-1'
        asset.owner = unit
        asset.setCustomAspects([simpleProps] as Set)

        asset = txTemplate.execute {
            assetRepository.save(asset)
        }

        when: "a request is made to the server"
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
                    domains:[],
                    references:[],
                    type: 'simpleAspect'
                ]
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
            references:[
                [
                    displayName:'Test unit',
                    targetUri   : "http://localhost/units/${unit.id.uuidValue()}",
                    searchesUri : "http://localhost/units/searches",
                    resourcesUri: "http://localhost/units{?parent,displayName}"
                ]
            ]
        ]
    }


    @WithUserDetails("user@domain.example")
    def "search an asset"() {
        given: "a search request body"
        Map search = [
            unitId: unit.id.uuidValue(),
        ]

        and: "a saved asset"
        CustomProperties simpleProps = entityFactory.createCustomProperties()
        simpleProps.setType("simpleAspect")
        simpleProps.setProperty("simpleProp", "simpleValue")

        def asset = entityFactory.createAsset()
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
        getSearchResponse == [
            [
                customAspects:[
                    simpleAspect:[
                        applicableTo:[],
                        attributes:[
                            simpleProp:'simpleValue'
                        ],
                        domains:[],
                        references:[],
                        type: 'simpleAspect'
                    ]
                ],
                domains:[],
                id: asset.id.uuidValue(),
                links:[:],
                name:'Test asset-1',
                owner:[
                    displayName:'Test unit',
                    targetUri: "http://localhost/units/${unit.id.uuidValue()}",
                    searchesUri: "http://localhost/units/searches",
                    resourcesUri: "http://localhost/units{?parent,displayName}"
                ],
                references:[
                    [
                        displayName:'Test unit',
                        targetUri: "http://localhost/units/${unit.id.uuidValue()}",
                        searchesUri: "http://localhost/units/searches",
                        resourcesUri: "http://localhost/units{?parent,displayName}"
                    ]
                ]
            ]
        ]
    }

    @WithUserDetails("user@domain.example")
    def "retrieve an asset with a link"() {
        given: "a saved asset"

        CustomProperties simpleProps = entityFactory.createCustomProperties()
        simpleProps.setType("simpleAspect")
        simpleProps.setProperty("simpleProp", "simpleValue")

        def asset2 = txTemplate.execute {
            Asset newAsset = entityFactory.createAsset(Key.newUuid(),"Test asset", unit)
            newAsset.domains = [domain1] as Set
            assetRepository.save(newAsset)
        }
        def asset = entityFactory.createAsset(Key.newUuid(),"Test asset-1", unit)
        asset.setCustomAspects([simpleProps] as Set)

        CustomLink link = entityFactory.createCustomLink("requires", asset2, asset)
        link.setType("mypreciouslink")
        link.setApplicableTo(["Asset"] as Set)
        asset.setLinks([link] as Set)

        asset = txTemplate.execute {
            assetRepository.save(asset)
        }

        when: "a request is made to the server"
        def results = get("/assets/${asset.id.uuidValue()}")
        String expectedETag = DigestUtils.sha256Hex(asset.id.uuidValue() + "_" + salt + "_" + Long.toString(asset.getVersion()))
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
            "http://localhost/assets/${asset2.id.uuidValue()}"
        ]
        result.links.mypreciouslink.applicableTo[0] == ['Asset']
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all assets for a client"() {
        given: "a saved asset"

        def asset = entityFactory.createAsset()
        asset.id = Key.newUuid()
        asset.name = "Test asset-1"
        asset.owner = unit
        asset.domains = [domain1] as Set

        def asset2 = entityFactory.createAsset()
        asset2.id = Key.newUuid()
        asset2.name = "Test asset-2"
        asset2.owner = unit2
        asset2.domains = [domain1] as Set

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

        def asset = entityFactory.createAsset()
        asset.id = Key.newUuid()
        asset.name = "Test asset-1"
        asset.owner = unit
        asset.domains = [domain1] as Set

        def asset2 = entityFactory.createAsset()
        asset2.id = Key.newUuid()
        asset2.name = "Test asset-2"
        asset2.owner = unit2

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

        def subUnit = entityFactory.createUnit(Key.newUuid(), "Sub unit", unit)
        unitRepository.save(subUnit)

        def subSubUnit = entityFactory.createUnit(Key.newUuid(), "Sub sub unit", subUnit)
        unitRepository.save(subSubUnit)

        assetRepository.save(entityFactory.createAsset(Key.newUuid(), "asset 0", subUnit))
        assetRepository.save(entityFactory.createAsset(Key.newUuid(), "asset 1", subSubUnit))

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

        def subUnit = entityFactory.createUnit(Key.newUuid(), "Sub unit", unit)
        unitRepository.save(subUnit)

        def subSubUnit = entityFactory.createUnit(Key.newUuid(), "Sub sub unit", subUnit)
        unitRepository.save(subSubUnit)

        assetRepository.save(entityFactory.createAsset(Key.newUuid(), "asset 0", subUnit))
        assetRepository.save(entityFactory.createAsset(Key.newUuid(), "asset 1", subSubUnit))

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

        Key<UUID> id = Key.newUuid()
        def asset = entityFactory.createAsset()
        asset.id = id
        asset.name = 'New asset-2'
        asset.owner = unit
        asset.domains = [domain1] as Set

        asset = txTemplate.execute {
            assetRepository.save(asset)
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
            'If-Match': ETag.from(id.uuidValue(), 1)
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
        Key<UUID> id = Key.newUuid()
        def asset = entityFactory.createAsset()
        asset.id = id
        asset.name = "Test asset-1"
        asset.owner = unit
        asset.domains = [domain1] as Set
        asset.customAspects = [cp] as Set

        asset = txTemplate.execute {
            assetRepository.save(asset)
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
            ], customAspects:
            [
                'my.aspect-test' :
                [
                    type : 'my.aspect-test1',
                    applicableTo: [
                        "Asset"
                    ],
                    domains: [],
                    attributes:  [
                        test1:'value1',
                        test2:'value2'
                    ]
                ]
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(id.uuidValue(), 1)
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
            assetRepository.findById(id).get().tap {
                // make sure that the proxy is resolved
                customAspects.first()
            }
        }

        then:
        entity.name == 'New asset-2'
        entity.abbreviation == 'u-2'
        entity.customAspects.first().type == 'my.aspect-test1'
        entity.customAspects.first().applicableTo == ['Asset'] as Set
        entity.customAspects.first().stringProperties.test1 == 'value1'
        entity.customAspects.first().stringProperties.test2 == 'value2'
    }

    @WithUserDetails("user@domain.example")
    def "delete an asset"() {

        given: "an existing asset"
        def asset = entityFactory.createAsset()
        asset.id = Key.newUuid()
        asset.name = 'New asset-2'
        asset.owner = unit
        asset.domains = [domain1] as Set

        asset = txTemplate.execute {
            assetRepository.save(asset)
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

        Asset newAsset = entityFactory.createAsset(Key.newUuid(),"Test asset-1", unit)
        newAsset.domains = [domain1] as Set
        def asset2 = txTemplate.execute {
            assetRepository.save(newAsset)
        }

        newAsset = entityFactory.createAsset(Key.newUuid(),"Test asset-1", unit)
        newAsset.domains = [domain1] as Set
        CustomLink link = entityFactory.createCustomLink("requires", asset2, newAsset)
        newAsset.links =[link] as Set

        def asset1 = txTemplate.execute {
            assetRepository.save(newAsset)
        }

        when: "a delete request is sent to the server for link target"

        def results = delete("/assets/${asset2.id.uuidValue()}")
        then: "the asset is deleted"
        results.andExpect(status().isOk())
        assetRepository.findById(asset2.id).empty
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
}
