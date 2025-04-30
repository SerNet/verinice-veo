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

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Asset
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
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
    def "retrieve an asset"() {
        given: "a saved asset"
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                associateWithDomain(dsgvoDomain, "AST_DataType", "NEW")
                name = 'Test asset-1'
                applyCustomAspect(newCustomAspect("asset_details", dsgvoDomain) {
                    attributes = [
                        asset_details_number: 42
                    ]
                })
            })
        }

        when: "a GET request is made to the server"
        def results = get("/assets/${asset.idAsString}")

        then: "the eTag is set"
        String eTag = getETag(results)
        eTag != null

        and: "the caching headers are set"
        def cacheControl = results.andReturn().response.getHeader(HttpHeaders.CACHE_CONTROL)
        cacheControl == 'no-cache'

        and: "the response contains the expected data"
        with(parseJson(results)) {
            _self == "http://localhost/assets/${asset.idAsString}"
            customAspects.asset_details.attributes.asset_details_number == 42
            customAspects.asset_details.domains[0].targetUri == "http://localhost/domains/$owner.dsgvoDomain.idAsString"
            designator == asset.designator
            domains[owner.dsgvoDomain.idAsString].subType == "AST_DataType"
            domains[owner.dsgvoDomain.idAsString].status == "NEW"
            id == asset.idAsString
            links == [:]
            name == 'Test asset-1'
            it.owner.displayName == "Test unit"
            it.owner.targetUri == "http://localhost/units/$owner.unit.idAsString"
            type == 'asset'
            parts == []
            createdBy == "user@domain.example"
            createdAt == roundToMicros(asset.createdAt).toString()
            updatedBy == "user@domain.example"
            updatedAt == roundToMicros(asset.updatedAt).toString()
        }

        when: "the asset is requested from the server again"
        results =
                mvc.perform(MockMvcRequestBuilders.get("/assets/${asset.idAsString}").accept(MediaType.APPLICATION_JSON).header(
                HttpHeaders.IF_NONE_MATCH, eTag
                ))

        then: "the server returns not-modified"
        results.andReturn().response.status == HttpStatus.SC_NOT_MODIFIED
    }

    @WithUserDetails("user@domain.example")
    def "retrieve an asset with a link"() {
        given: "a saved asset"
        def targetAsset = txTemplate.execute {
            assetRepository.save(newAsset(unit))
        }
        def sourceAsset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
                links = [
                    newCustomLink(targetAsset, "mypreciouslink", dsgvoDomain)
                ]
                name = "Test asset-1"
            })
        }

        when: "a request is made to the server"
        def results = get("/assets/${sourceAsset.idAsString}")

        then: "the asset is found"
        getETag(results) != null

        and: "the response contains the expected link"
        def result = parseJson(results)
        result.name == 'Test asset-1'
        result.links.size() == 1

        and: "the reference contains the expected data fields"
        with(result.links.mypreciouslink[0].target) {
            id == targetAsset.idAsString
            designator ==~ /AST-\d+/
            name == "asset null"
            type == "asset"
            targetUri == "http://localhost/assets/${targetAsset.idAsString}"
        }

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
            associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
        }

        def asset2 = newAsset(unit2) {
            name = "Test asset-2"
            associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
        }

        (asset, asset2) = txTemplate.execute {
            [asset, asset2].collect(assetRepository.&save)
        }

        when: "all assets are requested"
        def result = parseJson(get("/assets"))

        then: "the assets are returned"
        def sortedItems = result.items.sort { it.name }
        sortedItems.size() == 2
        sortedItems[0].name == 'Test asset-1'
        sortedItems[0].owner.targetUri == "http://localhost/units/" + unit.idAsString
        sortedItems[1].name == 'Test asset-2'
        sortedItems[1].owner.targetUri == "http://localhost/units/" + unit2.idAsString
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
        def result = parseJson(get("/assets?unit=${unit.idAsString}"))

        then: "asset 1 is returned"
        result.items*.name == ['Test asset-1']

        when: "a request is made to the server"
        result = parseJson(get("/assets?unit=${unit2.idAsString}"))

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
        def result = parseJson(get("/assets?unit=${unit.idAsString}"))

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
        def result = parseJson(get("/assets?unit=${unit.idAsString}&displayName=sset 1"))

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
        def result = parseJson(get("/assets?unit=${unit.idAsString}&displayName=ballverein Äächen 1"))

        then: "only the matching asset from the unit's hierarchy is returned"
        result.items*.name == ["Fußballverein Äächen 1"]
    }

    @WithUserDetails("user@domain.example")
    def "delete an asset"() {
        given: "an existing asset"
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = "Test asset-2"
                associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
            })
        }

        when: "a delete request is sent to the server"
        delete("/assets/${asset.idAsString}")

        then: "the asset is deleted"
        assetRepository.findById(asset.id).empty
    }

    @WithUserDetails("user@domain.example")
    def "delete an asset that is a link target"() {
        given: "two assets with a link between them"
        def targetAsset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
            })
        }

        def sourceAsset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
            })
        }
        CustomLink link = newCustomLink(targetAsset, "goodLink", dsgvoDomain)
        sourceAsset.links =[link] as Set

        when: "a delete request is sent to the server for link target"
        delete("/assets/${targetAsset.idAsString}")

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
        delete("/assets/${composite.idAsString}")

        then: "the composite is deleted"
        assetRepository.findById(composite.id).empty

        and: "the asset is not deleted"
        !assetRepository.findById(asset.id).empty
    }

    @WithUserDetails("user@domain.example")
    def     "A risk can be created for an asset"() {
        given: "saved elements"
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                name = 'New asset-2'
                associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
            })
        }
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
            })
        }

        when: "a new risk can be created successfully"
        def domainId = dsgvoDomain.getIdAsString()
        def json = parseJson(post("/assets/"+asset.idAsString+"/risks", [
            scenario: [ targetUri: 'http://localhost/scenarios/'+ scenario.idAsString ],
            domains: [
                (domainId) : [
                    reference: [ targetUri: 'http://localhost/domains/'+ domainId]
                ]
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
                get("/assets/" + asset.idAsString + "/risks/" + scenario.idAsString))

        then: "the correct object is returned"
        getResult != null
        with(getResult) {
            it.asset.targetUri ==~ /.*${asset.idAsString}.*/
            it.scenario.targetUri ==~ /.*${scenario.idAsString}.*/
            it.scenario.targetUri ==~ /.*${postResult.resourceId}.*/
            it.domains.values().first().reference.displayName == this.dsgvoDomain
                    .displayName
            it._self ==~ /.*assets\/${asset.idAsString}\/risks\/${scenario.idAsString}.*/
            Instant.parse(it.createdAt) > beforeCreation
            Instant.parse(it.updatedAt) > beforeCreation
            it.createdBy == "user@domain.example"
            it.updatedBy == "user@domain.example"
        }
    }

    @WithUserDetails("user@domain.example")
    def "A list of risks can be retrieved for an asset"() {
        given: "An asset with multiple risks"
        Asset asset = createRisk().first()
        def scenario2 = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
            })
        }
        def scenario3 = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
            })
        }
        post("/assets/"+asset.idAsString+"/risks", [
            scenario: [ targetUri: 'http://localhost/scenarios/'+ scenario2.idAsString ],
            domains: [
                (dsgvoDomain.getIdAsString()) : [
                    reference: [targetUri: 'http://localhost/domains/'+ dsgvoDomain
                        .idAsString ]
                ]

            ]
        ] as Map)
        post("/assets/"+asset.idAsString+"/risks", [
            scenario: [ targetUri: 'http://localhost/scenarios/'+ scenario3.idAsString ],
            domains: [
                (dsgvoDomain.getIdAsString()) : [
                    reference: [targetUri: 'http://localhost/domains/'+ dsgvoDomain
                        .idAsString ]
                ]
            ]
        ] as Map)

        when: "The risks are queried"
        def getResult = parseJson(
                get("/assets/${asset.idAsString}/risks"))

        then: "The risks are retreived"
        getResult.size() == 3
    }

    @WithUserDetails("user@domain.example")
    def "A risk can be deleted"() {
        given: "an asset risk"
        def (Asset asset, ScenarioData scenario, Object postResult) = createRisk()

        when: "the risk is deleted"
        delete("/assets/${asset.idAsString}/risks/${scenario.idAsString}")

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
                associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
            })
        }

        def control = txTemplate.execute {
            controlRepository.save(newControl(unit) {
                name = 'New control-1'
                associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
            })
        }

        and: "the created risk is retrieved"
        def getResponse = get("/assets/" + asset.idAsString + "/risks/" + scenario.idAsString)
        def getResult = parseJson(getResponse)
        String eTag = getResponse.andReturn().response.getHeader("ETag").replace("\"", "")

        when: "The risk is updated"
        def beforeUpdate = Instant.now()
        def putBody = getResult + [
            mitigation: [targetUri: 'http://localhost/controls/' + control.idAsString],
            riskOwner: [targetUri: 'http://localhost/persons/' + person.idAsString]
        ]
        Map headers = [
            'If-Match': eTag
        ]

        put("/assets/${asset.idAsString}/risks/${scenario.idAsString}", putBody as Map, headers)

        and: "the risk is retrieved again"
        def riskJson = parseJson(
                get("/assets/" + asset.idAsString + "/risks/" + scenario.idAsString))

        then: "the information was persisted"
        eTag.length() > 0
        riskJson != null
        with(riskJson) {
            it.mitigation.targetUri ==~ /.*${control.idAsString}.*/
            it.riskOwner.targetUri ==~ /.*${person.idAsString}.*/
            it.asset.targetUri ==~ /.*${asset.idAsString}.*/
            it.scenario.targetUri ==~ /.*${scenario.idAsString}.*/
            it.domains.values().first().reference.displayName == this.dsgvoDomain
                    .displayName
            it._self ==~ /.*assets\/${asset.idAsString}\/risks\/${scenario.idAsString}.*/
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
                get("/assets/" + asset.idAsString + "/risks/" + scenario.idAsString))

        then: "their references are removed from the risk"
        riskJson != null
        with(riskJson) {
            it._self ==~ /.*assets\/${asset.idAsString}\/risks\/${scenario.idAsString}.*/
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
        get("/assets/" + asset.idAsString + "/risks/" + scenario.idAsString, 404)

        then: "the risk was removed as well"
        thrown(NotFoundException)
    }

    private List createRisk() {
        def asset = txTemplate.execute {
            assetRepository.save(newAsset(unit) {
                associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
            })
        }
        def scenario = txTemplate.execute {
            scenarioDataRepository.save(newScenario(unit) {
                associateWithDomain(dsgvoDomain, "SCN_Scenario", "NEW")
            })
        }
        def postResult = parseJson(
                post("/assets/" + asset.idAsString + "/risks", [
                    scenario: [targetUri: 'http://localhost/scenarios/' + scenario.idAsString],
                    domains : [
                        (dsgvoDomain.getIdAsString()): [
                            reference: [targetUri: 'http://localhost/domains/' + dsgvoDomain
                                .idAsString]
                        ]
                    ]
                ]))
        return [asset, scenario, postResult]
    }
}
