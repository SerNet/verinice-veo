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
import org.testcontainers.containers.wait.strategy.Wait

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.repository.CatalogRepository
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.UnitRepository
import org.veo.jobs.MessagingJob

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

    @Value('${veo.message.consume.queue:veo}')
    String queue

    @Autowired
    ClientRepository clientRepo

    @Autowired
    UnitRepository unitRepo

    @Autowired
    CatalogRepository catalogRepository

    @Autowired
    MessagingJob messagingJob

    Domain domain
    String domainId
    String unitId

    def setupSpec() {
        if (!System.env.containsKey('SPRING_RABBITMQ_HOST')) {
            println("Test will start RabbitMQ container...")

            rabbit = new GenericContainer("rabbitmq:3-management")
                    .withExposedPorts(5672, 15672)
                    .waitingFor(Wait.forListeningPort())
                    .tap {
                        it.start()
                    }

            System.properties.putAll([
                "spring.rabbitmq.host": rabbit.getContainerIpAddress(),
                "spring.rabbitmq.port": rabbit.getMappedPort(5672),
            ])
        }
    }

    def setup() {
        def client = createTestClient()
        domain = newDomain(client)
        domainId = domain.id.uuidValue()
        clientRepo.save(client)

        unitId = unitRepo.save(newUnit(client)).id.uuidValue()
    }

    def 'data is migrated when element type definition is changed'() {
        given: "a domain where assets can have the custom aspects aspectOne & aspectTwo"
        def schema = [
            properties: [
                customAspects: [
                    properties: [
                        aspectOne: [
                            properties: [
                                attributes: [
                                    properties:[
                                        attrOne: [
                                            type: "string"
                                        ],
                                        attrTwo: [
                                            type: "string"
                                        ]
                                    ]
                                ]
                            ]
                        ],
                        aspectTwo: [
                            properties: [
                                attributes: [
                                    properties:[
                                        attrOne: [
                                            type: "string"
                                        ]
                                    ]
                                ]
                            ]
                        ]
                    ]
                ],
                domains: [
                    properties: [
                        "70e5c01d-2f81-4940-8635-1078c057c34c": [
                            allOf: [
                                [
                                    if: [
                                        properties: [
                                            subType: [
                                                const: "NormalAsset"
                                            ]
                                        ]
                                    ],
                                    then: [
                                        properties: [
                                            status: [
                                                enum: ["NEW"]
                                            ]
                                        ]
                                    ]
                                ]
                            ]
                        ]
                    ]
                ],
                links: [properties:[:]],
                translations: [:]
            ]
        ]
        post("/domains/$domainId/elementtypedefinitions/asset/updatefromobjectschema", schema, 204)

        and: "an asset that conforms to the element type definition"
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
        catalogRepository.save(newCatalog(domain) {
            catalogItems = [
                newCatalogItem(it) {
                    it.element = newAsset(null) {
                        name = "my little catalog asset"
                        customAspects = [
                            newCustomAspect("aspectOne", domain) {
                                attributes = [
                                    'attrOne': "catalogValueOne",
                                    'attrTwo': "catalogValueTwo",
                                ]
                            }
                        ]
                    }
                }
            ]
        })

        when: "removing one custom aspect type and one custom aspect attribute from the element type definition"
        schema.properties.customAspects.properties.remove("aspectTwo")
        schema.properties.customAspects.properties.aspectOne.properties.attributes.properties.remove("attrTwo")
        post("/domains/$domainId/elementtypedefinitions/asset/updatefromobjectschema", schema, 204)

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
        def catalogId = parseJson(get("/catalogs")).first().id
        def catalogItem = parseJson(get("/catalogs/$catalogId/items")).first()
        def catalogItemElement = parseJson(get(catalogItem.element.targetUri))

        then:"the content of the catalog item that still conforms to the current element type definition is still there"
        catalogItemElement.customAspects.aspectOne != null
        catalogItemElement.customAspects.aspectOne.attributes.attrOne != null

        and: "the obsolete content has been removed from the catalog item"
        catalogItemElement.customAspects.aspectOne.attributes.attrTwo == null

    }

    def cleanup() {
        def purgeCount = rabbitAdmin.purgeQueue(queue)
        if (purgeCount>0)
            log.info("Test cleanup: purged {} remaining messages in test queue.", purgeCount)
        eventStoreDataRepository.deleteAll()
    }
}
