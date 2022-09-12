/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.repository.DomainRepository
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.StoredEventRepository
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.access.jpa.StoredEventDataRepository
import org.veo.test.VeoSpec

import spock.lang.Issue
/**
 * Integration test to verify entity event generation. Performs operations on the REST API and performs assertions on the {@link StoredEventRepository}.
 */
class StoredEventsMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private DomainRepository domainRepository

    @Autowired
    private DomainTemplateDataRepository domainTemplateRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private StoredEventDataRepository storedEventRepository

    private Client client
    private Domain domain
    private Unit unit

    def setup() {
        txTemplate.execute {
            client = createTestClient()
            def template = domainTemplateRepository.save(newDomainTemplate())
            domain = domainRepository.save(newDomain(client) {
                domainTemplate = template
                name = "ISO"
                elementTypeDefinitions = [
                    newElementTypeDefinition("asset", it) {
                        subTypes = [
                            EventfulAsset: newSubTypeDefinition()
                        ]
                    },
                    newElementTypeDefinition("process", it) {
                        subTypes = [
                            EventfulProcess: newSubTypeDefinition()
                        ]
                    },
                    newElementTypeDefinition("scenario", it) {
                        subTypes = [
                            EventfulScenario: newSubTypeDefinition()
                        ]
                    },
                ]
            })

            unit = newUnit(client) {
                name = "Test unit"
            }

            clientRepository.save(client)
            unitRepository.save(unit)
        }
    }

    @WithUserDetails("user@domain.example")
    def "domain creation event is generated"() {
        when:
        def event = getLatestStoredEventContent("domain_creation_event")

        then:
        event.clientId == client.id.uuidValue()
        event.domainId == domain.id.uuidValue()
        event.domainTemplateId == domain.domainTemplate.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "document events are generated"() {
        when: "creating a document"
        String documentId = parseJson(post("/documents", [
            name: "doc",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId

        then: "a CREATION event is stored"
        with(getLatestStoredEventContent()) {
            type == "CREATION"
            uri == "/documents/$documentId"
            author == "user@domain.example"
            changeNumber == 0
            with(content) {
                id == documentId
                name == "doc"
            }
        }

        when: "updating the document"
        def eTag = getETag(get("/documents/$documentId"))
        put("/documents/$documentId", [
            name: "super doc",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ], ["If-Match": eTag])

        then: "a MODIFICATION event is stored"
        with(getLatestStoredEventContent()) {
            type == "MODIFICATION"
            uri == "/documents/$documentId"
            author == "user@domain.example"
            changeNumber == 1
            with(content) {
                id == documentId
                name == "super doc"
            }
        }

        when: "deleting the document"
        delete("/documents/$documentId")

        then: "a HARD_DELETION event is stored"
        with(getLatestStoredEventContent()) {
            type == "HARD_DELETION"
            uri == "/documents/$documentId"
            author == "user@domain.example"
            changeNumber == 2
            with(content) {
                id == documentId
                name == "super doc"
            }
        }
    }

    @Issue('VEO-473')
    @WithUserDetails("user@domain.example")
    def "client events are ignored"() {
        given:
        def numberOfStoredEventsBefore = storedEventRepository.findAll().size()

        when: "creating a client"
        def client = clientRepository.save(newClient())

        then: "no event is stored for the client"
        storedEventRepository.findAll().size() == numberOfStoredEventsBefore

        when: "updating the client"
        client.setName("new name")
        clientRepository.save(client)

        then: "no event is stored for the client"
        storedEventRepository.findAll().size() == numberOfStoredEventsBefore
    }

    @WithUserDetails("user@domain.example")
    def "asset risk events are generated"() {
        given: "an asset risk"
        when: "creating an asset risk"
        String assetId = parseJson(post("/assets", [
            name: "acid",
            domains: [
                (domain.id.uuidValue()): [
                    subType: "EventfulAsset",
                    status: "NEW",
                ]
            ],
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        String scenarioId = parseJson(post("/scenarios", [
            name: "scenario",
            domains: [
                (domain.id.uuidValue()): [
                    subType: "EventfulScenario",
                    status: "NEW",
                ]
            ],
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        post("/assets/$assetId/risks", [
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"],
            domains : [
                (domain.getIdAsString()) : [
                    reference: [targetUri: "http://localhost/domains/${domain.id.uuidValue()}"]
                ]
            ]
        ])

        then:
        with(getLatestStoredEventContent()) {
            type == "CREATION"
            uri == "/assets/$assetId/risks/$scenarioId"
            author =="user@domain.example"
            content.scenario.targetUri == "/scenarios/$scenarioId"
        }

        when:
        String controlId = parseJson(post("/controls", [
            name: "Im in control",
            owner: [targetUri: "http://localhost/units/${unit.id.uuidValue()}"]
        ])).resourceId
        String riskETag = parseETag(get("/assets/$assetId/risks/$scenarioId"))
        put("/assets/$assetId/risks/$scenarioId", [
            mitigation: [targetUri:  "/controls/$controlId"],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"],
            domains : [
                (domain.getIdAsString()) : [
                    reference: [targetUri: "http://localhost/domains/${domain.id.uuidValue()}"]
                ]
            ]
        ], ["If-Match": riskETag])

        then:
        with(getLatestStoredEventContent()) {
            type == "MODIFICATION"
            uri == "/assets/$assetId/risks/$scenarioId"
            author =="user@domain.example"
            content.mitigation.targetUri == "/controls/$controlId"
        }
    }

    @WithUserDetails("user@domain.example")
    def "process risk events are generated"() {
        given: "a process risk"
        when: "creating a process risk"
        String processId = parseJson(post("/processes", [
            name: "pro",
            domains: [
                (domain.id.uuidValue()): [
                    subType: "EventfulProcess",
                    status: "NEW",
                ]
            ],
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        String scenarioId = parseJson(post("/scenarios", [
            name: "scenario",
            domains: [
                (domain.id.uuidValue()): [
                    subType: "EventfulScenario",
                    status: "NEW",
                ]
            ],
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        post("/processes/$processId/risks", [
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"],
            domains : [
                (domain.getIdAsString()) : [
                    reference: [targetUri: "http://localhost/domains/${domain.id.uuidValue()}"]
                ]
            ]
        ])

        then:
        with(getLatestStoredEventContent()) {
            type == "CREATION"
            uri == "/processes/$processId/risks/$scenarioId"
            author =="user@domain.example"
            content.scenario.targetUri == "/scenarios/$scenarioId"
        }

        when:
        String controlId = parseJson(post("/controls", [
            name: "Im in control",
            owner: [targetUri: "http://localhost/units/${unit.id.uuidValue()}"]
        ])).resourceId
        String riskETag = parseETag(get("/processes/$processId/risks/$scenarioId"))
        put("/processes/$processId/risks/$scenarioId", [
            mitigation: [targetUri:  "/controls/$controlId"],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"],
            domains : [
                (domain.getIdAsString()) : [
                    reference: [targetUri: "http://localhost/domains/${domain.id.uuidValue()}"]
                ]
            ]
        ], ["If-Match": riskETag])

        then:
        with(getLatestStoredEventContent()) {
            type == "MODIFICATION"
            uri == "/processes/$processId/risks/$scenarioId"
            author =="user@domain.example"
            content.mitigation.targetUri == "/controls/$controlId"
        }
    }

    @Issue('VEO-556')
    @WithUserDetails("user@domain.example")
    def "events for domain template entities are ignored"() {
        given:
        def numberOfStoredEventsBefore = storedEventRepository.findAll().size()
        println "Found $numberOfStoredEventsBefore events"
        when: "creating a domain template"
        def domainTemplate = domainTemplateRepository.save(newDomainTemplate() {
            templateVersion = '1'
            authority = 'me'

            newCatalog(it) {
                def item1 = newCatalogItem(it, VeoSpec.&newAsset)
                def item2 = newCatalogItem(it, VeoSpec.&newControl)
                newTailoringReference(item2, TailoringReferenceType.COPY) {
                    catalogItem = item1
                }
            }
        })

        then: "no event is stored"
        storedEventRepository.findAll().size() == numberOfStoredEventsBefore

        when: "adding a catalog to the domain template"
        domainTemplate.addToCatalogs( newCatalog(domainTemplate) {
            newCatalogItem(it, {
                newAsset(it)
            })
        })
        domainTemplate = domainTemplateRepository.save(domainTemplate)
        then: "no event is stored"
        storedEventRepository.findAll().size() == numberOfStoredEventsBefore

        when: "updating some entities in the template"
        domainTemplate.tap {
            templateVersion = '1'
            catalogs.first().tap {
                description = 'This is very important!'
                catalogItems.first().tap {
                    namespace = 'my namespace'
                    element.tap {
                        description = 'Ignore this!'
                    }
                }
                catalogItems[1].tap {
                    tailoringReferences.first().tap {
                        referenceType = TailoringReferenceType.COPY_ALWAYS
                    }
                }
            }
        }
        domainTemplateRepository.save(domainTemplate)

        then: "no event is stored"
        storedEventRepository.findAll().size() == numberOfStoredEventsBefore
    }

    private Object getLatestStoredEventContent(String routingKey = "") {
        parseJson(storedEventRepository
                .findAll()
                .findAll {
                    it.routingKey.endsWith(routingKey)
                }
                .sort {
                    it.id
                }
                .last()
                .content)
    }
}
