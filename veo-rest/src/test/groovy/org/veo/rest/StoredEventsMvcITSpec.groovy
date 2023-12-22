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
import org.veo.core.entity.Key
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.repository.DocumentRepository
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.access.jpa.StoredEventDataRepository

import spock.lang.Issue

/**
 * Integration test to verify entity event generation. Performs operations on the REST API and
 * performs assertions on the {@link org.veo.persistence.access.StoredEventRepository}.
 */
class StoredEventsMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private DomainTemplateDataRepository domainTemplateRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private StoredEventDataRepository storedEventRepository

    @Autowired
    private DocumentRepository documentRepository

    private Client client
    private Domain domain
    private Unit unit

    def setup() {
        txTemplate.execute {
            storedEventRepository.deleteAll()
            client = createTestClient()
            def template = domainTemplateRepository.save(newDomainTemplate())
            domain = newDomain(client) {
                domainTemplate = template
                name = "ISO"
                applyElementTypeDefinition(newElementTypeDefinition("asset", it) {
                    subTypes = [
                        EventfulAsset: newSubTypeDefinition()
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition("document", it) {
                    subTypes = [
                        EventfulDocument: newSubTypeDefinition()
                    ]
                    customAspects = [
                        size: newCustomAspectDefinition {
                            attributeDefinitions = [
                                widthInMm: new IntegerAttributeDefinition(),
                                heightInMm: new IntegerAttributeDefinition(),
                            ]
                        }
                    ]
                    links = [
                        systems: newLinkDefinition("asset", "EventfulAsset")
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition("process", it) {
                    subTypes = [
                        EventfulProcess: newSubTypeDefinition()
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition("scenario", it) {
                    subTypes = [
                        EventfulScenario: newSubTypeDefinition()
                    ]
                })
            }

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
        def event = getLatestStoredEventContent("domain_creation")

        then:
        event.clientId == client.id.uuidValue()
        event.domainId == domain.id.uuidValue()
        event.domainTemplateId == domain.domainTemplate.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "document events are generated"() {
        when: "creating a document linked with an asset"
        def domainId = domain.idAsString
        String assetId = parseJson(post("/domains/$domainId/assets", [
            name: "ast",
            subType: "EventfulAsset",
            status: "NEW",
            owner: [
                targetUri: "http://localhost/units/${unit.idAsString}"
            ]
        ])).resourceId
        String documentId = parseJson(post("/domains/$domainId/documents", [
            name: "doc",
            subType: "EventfulDocument",
            status: "NEW",
            customAspects: [
                size: [
                    widthInMm: 210,
                    heightInMm: 297,
                ],
            ],
            links: [
                systems: [
                    [
                        target: [targetUri: "/assets/$assetId"]
                    ]
                ]
            ],
            owner: [
                targetUri: "http://localhost/units/${unit.idAsString}"
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
                domains[domainId].subType == "EventfulDocument"
                domains[domainId].status == "NEW"
                domains[domainId].customAspects.size.heightInMm == 297
                domains[domainId].links.systems.first().target.name == "ast"
                customAspects == null
                links == null
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

        then: "exactly one additional MODIFICATION event is stored"
        with(getNthStoredEventContent(-2)) {
            type == "CREATION"
            uri == "/documents/$documentId"
            author == "user@domain.example"
            changeNumber == 0
            with(content) {
                id == documentId
                name == "doc"
            }
        }
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

        when: "the entity is retrieved"
        def updatedDocument = documentRepository.getById(Key.uuidFrom(documentId), client.id)

        then: "the correct changeNumber was written to the entity"
        updatedDocument.changeNumber == 1

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

        when: "the document is retrieved"
        documentRepository.getById(Key.uuidFrom(documentId), client.id)

        then: "the document is gone"
        thrown(NotFoundException)
    }

    @WithUserDetails("user@domain.example")
    def "events are generated for domain associations"() {
        given: "a second domain"
        def domainId = domain.idAsString
        def domain2 = newDomain(client) {
            name = "the other one"
            applyElementTypeDefinition(newElementTypeDefinition("document", it) {
                subTypes = [
                    Dogmatic: newSubTypeDefinition()
                ]
            })
        }
        client = clientRepository.save(client)
        unit.domains.add(domain2)
        unitRepository.save(unit)
        def domainId2 = domain2.idAsString

        and: "a document in domain 1"
        String documentId = parseJson(post("/domains/$domainId/documents", [
            name: "doc",
            subType: "EventfulDocument",
            status: "NEW",
            owner: [
                targetUri: "http://localhost/units/${unit.idAsString}"
            ]
        ])).resourceId

        when: "assigning the document to the second domain"
        post("/domains/$domainId2/documents/$documentId", [
            subType: "Dogmatic",
            status: "NEW",
        ], 200)

        then: "a MODIFICATION event is stored"
        with(getLatestStoredEventContent()) {
            type == "MODIFICATION"
            uri == "/documents/$documentId"
            author == "user@domain.example"
            changeNumber == 1
            with(content) {
                id == documentId
                name == "doc"
                domains.keySet() ==~ [domainId, domainId2]
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
        String riskETag = getETag(get("/assets/$assetId/risks/$scenarioId"))
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
        String riskETag = getETag(get("/processes/$processId/risks/$scenarioId"))
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

            def item1 = newCatalogItem(it, {
                elementType = "asset"
                subType = "AST"
                status = "NEW"
            })
            def item2 = newCatalogItem(it, {
                elementType = "control"
                subType = "CTL_TOM"
                status = "NEW"
            })
            newTailoringReference(item2, item1, TailoringReferenceType.COPY)
        })

        then: "no event is stored"
        storedEventRepository.findAll().size() == numberOfStoredEventsBefore

        when: "adding a catalog to the domain template"
        newCatalogItem(domainTemplate) {
            elementType = "asset"
            name = "a2"
            subType = "CTL_TOM"
            status = "NEW"
        }
        domainTemplate = domainTemplateRepository.save(domainTemplate)

        then: "no event is stored"
        storedEventRepository.findAll().size() == numberOfStoredEventsBefore

        when: "updating some entities in the template"
        domainTemplate.tap {
            templateVersion = '1'
            catalogItems.first().tap {
                namespace = 'my namespace'
                description = 'Ignore this!'
            }
            catalogItems[1].tap {
                tailoringReferences.first().tap {
                    referenceType = TailoringReferenceType.COPY_ALWAYS
                }
            }
        }
        domainTemplateRepository.save(domainTemplate)

        then: "no event is stored"
        storedEventRepository.findAll().size() == numberOfStoredEventsBefore
    }

    private Object getLatestStoredEventContent(String routingKey = "") {
        getNthStoredEventContent(-1, routingKey)
    }

    private Object getNthStoredEventContent(int index = -1, String routingKey = "") {
        parseJson(storedEventRepository
                .findAll()
                .findAll {
                    it.routingKey.endsWith(routingKey)
                }
                .sort {
                    it.id
                }
                .getAt(index)
                .content)
    }
}
