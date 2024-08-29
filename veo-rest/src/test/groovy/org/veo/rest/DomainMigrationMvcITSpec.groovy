/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS
import static org.veo.rest.VeoRestConfiguration.PROFILE_BACKGROUND_TASKS

import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.repository.CatalogItemRepository
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.ProfileItemRepository
import org.veo.core.repository.ProfileRepository
import org.veo.core.repository.UnitRepository
import org.veo.jobs.MessagingJob
import org.veo.message.TestContainersUtil

import spock.lang.AutoCleanup
import spock.lang.Shared

@ActiveProfiles(["test", PROFILE_BACKGROUND_TASKS])
@DirtiesContext(classMode = AFTER_CLASS)
@WithUserDetails("content-creator")
class DomainMigrationMvcITSpec extends VeoMvcSpec {
    @Shared
    @AutoCleanup("stop")
    private GenericContainer rabbit

    @Autowired
    private RabbitAdmin rabbitAdmin

    @Value('${veo.message.queues.veo:veo}')
    String queue

    @Autowired
    ClientRepository clientRepo

    @Autowired
    UnitRepository unitRepo

    @Autowired
    CatalogItemRepository catalogItemRepository

    @Autowired
    MessagingJob messagingJob
    @Autowired
    ProfileItemRepository profileItemRepository
    @Autowired
    ProfileRepository profileRepository

    Domain domain
    String domainId
    String unitId

    def setupSpec() {
        rabbit = TestContainersUtil.startRabbitMqContainer()
    }

    def setup() {
        def client = createTestClient()
        domain = newDomain(client) {
            applyElementTypeDefinition(newElementTypeDefinition("asset", it) {
                subTypes = [
                    NormalAsset: newSubTypeDefinition {
                        statuses = ["NEW"]
                    }
                ]
                customAspects = [
                    aspectOne: newCustomAspectDefinition {
                        attributeDefinitions = [
                            attrOne: new TextAttributeDefinition(),
                            attrTwo: new TextAttributeDefinition(),
                        ]
                    },
                    aspectTwo: newCustomAspectDefinition {
                        attributeDefinitions = [
                            attrOne: new TextAttributeDefinition(),
                        ]
                    }
                ]
            })
            applyElementTypeDefinition(newElementTypeDefinition("scenario", it) {
                subTypes.NormalScenario = newSubTypeDefinition()
            })
        }
        domainId = domain.idAsString
        clientRepo.save(client)

        unitId = unitRepo.save(newUnit(client)).idAsString
    }

    @WithUserDetails("content-creator")
    def 'data is migrated when element type definition is changed'() {
        given: "an asset that conforms to the element type definition"
        def assetId = parseJson(post("/assets", [
            domains: [
                (domainId): [
                    subType: "NormalAsset",
                    status: "NEW",
                ]
            ],
            name: "my little asset",
            owner: [targetUri: "http://localhost/units/$unitId"],
            customAspects: [
                aspectOne: [
                    attributes: [
                        attrOne: "valueOne",
                        attrTwo: "valueTwo"
                    ]
                ],
                aspectTwo: [
                    attributes: [
                        attrOne: "valueOne"
                    ]
                ]
            ]
        ])).resourceId
        // TODO VEO-399 create catalog item via API endpoint

        and: "a catalog item that conforms to the element type definition"
        executeInTransaction {
            catalogItemRepository.save(newCatalogItem(domain) {
                name = "my little catalog asset"
                status = "NEW"
                elementType = "asset"
                subType = "NormalAsset"
                customAspects = [
                    "aspectOne": [
                        'attrOne': "catalogValueOne",
                        'attrTwo': "catalogValueTwo",
                    ]
                ]
            })
        }

        and: "a profile with the element"
        def profile = parseJson(post("/content-creation/domains/${domainId}/profiles?unit=$unitId",
                [
                    name: 'test',
                    description: 'All the good stuff',
                    language: 'de_DE'
                ], 201))

        when: "removing one custom aspect type and one custom aspect attribute from the element type definition"
        def etd = parseJson(get("/domains/$domainId")).elementTypeDefinitions.asset
        etd.customAspects.remove("aspectTwo")
        etd.customAspects.aspectOne.attributeDefinitions.remove("attrTwo")
        put("/content-creation/domains/$domainId/element-type-definitions/asset", etd, 204)

        and: "triggering message processing"
        messagingJob.sendMessages()

        and: "wait for the asset to be migrated"
        def retrievedAsset = null
        for (def i = 0; i < 50; i++) {
            retrievedAsset = parseJson(get("/assets/$assetId"))
            if (retrievedAsset.customAspects.size() == 1) {
                break
            }
            sleep(200)
        }

        then: "the content of the asset that still conforms to the current element type definition is still there"
        retrievedAsset.customAspects.aspectOne != null
        retrievedAsset.customAspects.aspectOne.attributes.attrOne != null

        and: "the obsolete content has been removed from the asset"
        retrievedAsset.customAspects.aspectOne.attributes.attrTwo == null
        retrievedAsset.customAspects.aspectTwo == null

        when: "retrieving the catalog item"
        def catalogItem = catalogItemRepository.findAllByDomain(domain).first()

        then:"the content of the catalog item that still conforms to the current element type definition is still there"
        catalogItem.customAspects.aspectOne != null
        catalogItem.customAspects.aspectOne.attrOne != null

        and: "the obsolete content has been removed from the catalog item"
        catalogItem.customAspects.aspectOne.attrTwo == null

        when:
        def profileItem = executeInTransaction {
            profileRepository.findAllByDomain(domain).first().getItems().first()
        }

        then:"the content of the profile item that still conforms to the current element type definition is still there"
        profileItem.customAspects.aspectOne != null
        profileItem.customAspects.aspectOne.attrOne != null

        and: "the obsolete content has been removed from the profile item"
        profileItem.customAspects.aspectOne.attrTwo == null
    }

    @WithUserDetails("content-creator")
    def 'risk is removed when element is dissociated from a domain'() {
        given: "an asset and a risk"
        def assetId = parseJson(post("/domains/$domainId/assets", [
            name: "my little asset",
            subType: "NormalAsset",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def scenarioId = parseJson(post("/domains/$domainId/scenarios", [
            name: "my little scenario",
            subType: "NormalScenario",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        post("/assets/$assetId/risks", [
            scenario: [targetUri: "/scenarios/$scenarioId"],
            domains: [
                (domainId): [
                    reference: [targetUri: "/domains/$domainId"],
                ]
            ],
        ])

        expect: "the risk to exist"
        parseJson(get("/assets/$assetId/risks")).size() == 1

        when: "removing the subtype from the element type definition"
        parseJson(get("/domains/$domainId")).elementTypeDefinitions.asset.with {
            it.subTypes = [:]
            put("/content-creation/domains/$owner.domainId/element-type-definitions/asset", it, 204)
        }

        and: "triggering message processing"
        messagingJob.sendMessages()

        and: "waiting for the asset to be migrated"
        defaultPolling.eventually {
            parseJson(get("/assets/$assetId")).domains.isEmpty()
        }

        then: "the risk is gone"
        parseJson(get("/assets/$assetId/risks")).isEmpty()
    }

    def cleanup() {
        def purgeCount = rabbitAdmin.purgeQueue(queue)
        if (purgeCount>0)
            log.info("Test cleanup: purged {} remaining messages in test queue.", purgeCount)
        eventStoreDataRepository.deleteAll()
    }
}
