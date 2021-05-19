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

import java.time.Duration
import java.time.Instant

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Key
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.StoredEventRepository
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.rest.configuration.WebMvcSecurityConfiguration

import spock.lang.Issue

/**
 * Integration test to verify entity event generation. Performs operations on the REST API and performs assertions on the {@link StoredEventRepository}.
 */
@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = [WebMvcSecurityConfiguration]
)
@EnableAsync
class StoredEventsMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private DomainTemplateDataRepository domainTemplateRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private StoredEventRepository storedEventRepository

    private Unit unit
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)

    def setup() {
        txTemplate.execute {
            def client = clientRepository.save(newClient {
                id = clientId
            })

            unit = newUnit(client) {
                name = "Test unit"
            }

            clientRepository.save(client)
            unitRepository.save(unit)
        }
    }

    @WithUserDetails("user@domain.example")
    def "document events are generated"() {
        when: "creating a document"
        String documentId = parseJson(post("/documents", [
            name: "doc",
            owner: [
                targetUri: "/units/${unit.id.uuidValue()}"
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
                targetUri: "/units/${unit.id.uuidValue()}"
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
            content == null
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
        client.setValidUntil(Instant.now().plus(Duration.ofDays(3l)))
        clientRepository.save(client)

        then: "no event is stored for the client"
        storedEventRepository.findAll().size() == numberOfStoredEventsBefore
    }

    @Issue('VEO-556')
    @WithUserDetails("user@domain.example")
    def "events for domain template entities are ignored"() {
        given:
        def numberOfStoredEventsBefore = storedEventRepository.findAll().size()
        println "Found $numberOfStoredEventsBefore events"
        when: "creating a domain template"
        def domainTemplate = domainTemplateRepository.save(newDomainTemplate{
            templateVersion = '1'
            revision = ''
            authority = 'me'
            catalogs = [
                newCatalog(it) {
                    def item1 = newCatalogItem(it) {
                        element = newAsset(it)
                    }
                    def item2 = newCatalogItem(it) {
                        element = newControl(it)
                        tailoringReferences = [
                            newTailoringReference(it) {
                                catalogItem = item1
                            }
                        ]
                    }
                    catalogItems = [item1, item2]
                }
            ]
        })

        then: "no event is stored"
        storedEventRepository.findAll().size() == numberOfStoredEventsBefore

        when: "adding a catalog to the domain template"
        domainTemplate.addToCatalogs( newCatalog(domainTemplate) {
            catalogItems = [
                newCatalogItem(it) {
                    element = newAsset(it)
                }
            ]
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

    private Object getLatestStoredEventContent() {
        parseJson(storedEventRepository.findAll().sort {
            it.id
        }.last().content)
    }
}
