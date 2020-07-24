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

import groovy.json.JsonSlurper

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.*
import org.veo.core.entity.custom.LinkImpl
import org.veo.core.entity.custom.SimpleProperties
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
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
class AssetControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private AssetRepositoryImpl assetRepository

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

            def client = newClient{
                id = clientId
                domains = [domain, domain1] as Set
            }
            unit = newUnit client, {
                name = "Test unit"
            }
            client.units << unit
            unit.client = client

            unit2 = newUnit client, {
                name = "Test unit2"
            }
            client.units << unit2
            unit2.client = client

            clientRepository.save(client)
        }
    }

    @WithUserDetails("user@domain.example")
    def "create an asset"() {
        given: "a request body"

        Map request = [
            name: 'New Asset',
            owner: [
                displayName: 'test2',
                href: '/units/' + unit.id.uuidValue()
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

        CustomProperties simpleProps = new SimpleProperties()
        simpleProps.setType("simpleAspect")
        simpleProps.setId(Key.newUuid())
        simpleProps.setProperty("simpleProp", "simpleValue")

        def asset = newAsset unit, {
            name = 'Test asset-1'
            setCustomAspects([simpleProps] as Set)
        }
        asset = txTemplate.execute {
            assetRepository.save(asset)
        }

        when: "a request is made to the server"
        def results = get("/assets/${asset.id.uuidValue()}")

        then: "the asset is found"
        results.andExpect(status().isOk())
        and: "the response contains the expected data"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result == [
            abbreviation:null,
            customAspects:[
                simpleAspect:[
                    applicableTo:[],
                    attributes:[
                        simpleProp:'simpleValue'
                    ],
                    domains:[],
                    id: simpleProps.id.uuidValue(),
                    references:[],
                    type: 'simpleAspect',
                    validFrom:null,
                    validUntil:null,
                    version:0
                ]
            ],
            description:null,
            domains:[],
            id: asset.id.uuidValue(),
            links:[:],
            name:'Test asset-1',
            owner:[
                displayName:'Test unit',
                href:"/units/${unit.id.uuidValue()}"
            ],
            references:[
                [
                    displayName:'Test unit',
                    href:"/units/${unit.id.uuidValue()}"
                ]
            ],
            validFrom:null,
            validUntil:null,
            version:0
        ]
    }

    @WithUserDetails("user@domain.example")
    def "retrieve an asset with a link"() {
        given: "a saved asset"

        CustomProperties simpleProps = new SimpleProperties()
        simpleProps.setType("simpleAspect")
        simpleProps.setId(Key.newUuid())
        simpleProps.setProperty("simpleProp", "simpleValue")

        def asset2 = txTemplate.execute {
            assetRepository.save(newAsset(unit, {
                domains = [domain1] as Set
            }))
        }
        def asset = newAsset unit, {
            name = 'Test asset-1'
            setCustomAspects([simpleProps] as Set)

            CustomLink link = new LinkImpl(Key.newUuid(), "requires", asset2, it)
            link.setVersion(1L);
            link.setType("mypreciouslink");
            link.setApplicableTo(["Asset"] as Set);
            addToLinks(link)
        }
        asset = txTemplate.execute {
            assetRepository.save(asset)
        }

        when: "a request is made to the server"
        def results = get("/assets/${asset.id.uuidValue()}")

        then: "the asset is found"
        results.andExpect(status().isOk())
        and: "the response contains the expected link"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test asset-1'
        result.links.size() == 1
        result.links.mypreciouslink.id[0] ==~ /[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/
        result.links.mypreciouslink.target.href == [
            "/assets/${asset2.id.uuidValue()}"
        ]
        result.links.mypreciouslink.applicableTo[0] == ['Asset']
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all assets for a client"() {
        given: "a saved asset"

        def asset = newAsset unit, {
            name = "Test asset-1"
        }

        def asset2 = newAsset unit2, {
            name = "Test asset-2"
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
        result.sort{it.name}.first().owner.href == "/units/"+unit.id.uuidValue()

        result.sort{it.name}[1].name == 'Test asset-2'
        result.sort{it.name}[1].owner.href == "/units/"+unit2.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all assets for a unit"() {
        given: "a saved asset"

        def asset = newAsset unit, {
            name = "Test asset-1"
        }

        def asset2 = newAsset unit2, {
            name = "Test asset-2"
        }

        (asset, asset2) = txTemplate.execute {
            [asset, asset2].collect(assetRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/assets?parent=${unit.id.uuidValue()}")

        then: "the assets are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 1

        result.first().name == 'Test asset-1'
        result.first().owner.href == "/units/"+unit.id.uuidValue()

        when: "a request is made to the server"
        results = get("/assets?parent=${unit2.id.uuidValue()}")

        then: "the assets are returned"
        results.andExpect(status().isOk())
        when:
        result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 1

        result.first().name == 'Test asset-2'
        result.first().owner.href == "/units/"+unit2.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put an asset"() {
        given: "a saved asset"

        Key<UUID> id = Key.newUuid()
        def asset = newAsset unit, {
            it.id = id
            name = 'New asset-2'
            domains = [domain1] as Set
        }

        asset = txTemplate.execute {
            assetRepository.save(asset)
        }

        Map request = [
            id: id.uuidValue(),
            name: 'New asset-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                href: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ],  domains: [
                [
                    href: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ]
        ]

        when: "a request is made to the server"
        def results = put("/assets/${asset.id.uuidValue()}", request)

        then: "the asset is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New asset-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.href == "/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "put an asset with custom properties"() {
        given: "a saved asset"

        CustomProperties cp = new SimpleProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Asset'] as Set)
        cp.setId(Key.newUuid())
        Key<UUID> id = Key.newUuid()
        def asset = newAsset unit, {
            it.id = id
            customAspects = [cp] as Set
            domains = [domain1] as Set
        }

        asset = txTemplate.execute {
            assetRepository.save(asset)
        }

        Map request = [
            id: id.uuidValue(),
            name: 'New asset-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                href: '/units/'+unit.id.uuidValue(),
                displayName: 'test unit'
            ], domains: [
                [
                    href: '/domains/'+domain.id.uuidValue(),
                    displayName: 'test ddd'
                ]
            ], customAspects:
            [
                'my.aspect-test' :
                [
                    id: '00000000-0000-0000-0000-000000000000',
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
        def results = put("/assets/${asset.id.uuidValue()}",request)

        then: "the asset is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New asset-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.href == "/units/"+unit.id.uuidValue()
        def entity = txTemplate.execute { assetRepository.findById(id).get() }
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
        def asset = newAsset unit, {
            domains = [domain1] as Set
        }

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

        def asset2 = txTemplate.execute {
            assetRepository.save(newAsset(unit, {
                domains = [domain1] as Set
            }))
        }

        def asset1 = txTemplate.execute {
            assetRepository.save(newAsset(unit, {
                domains = [domain1] as Set
                CustomLink link = new LinkImpl(Key.newUuid(), "requires", asset2, it)
                addToLinks(link)
            }))
        }

        when: "a delete request is sent to the server for link target"

        def results = delete("/assets/${asset2.id.uuidValue()}")
        then: "the asset is deleted"
        results.andExpect(status().isOk())
        assetRepository.findById(asset2.id).empty
    }
}
