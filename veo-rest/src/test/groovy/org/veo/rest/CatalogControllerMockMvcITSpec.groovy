/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Catalog
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.CatalogRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.rest.configuration.WebMvcSecurityConfiguration

/**
 * Integration test for the catalog. Uses mocked spring MVC environment.
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
class CatalogControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private DomainRepositoryImpl domainRepository

    @Autowired
    private CatalogRepositoryImpl catalogRepository

    @Autowired
    TransactionTemplate txTemplate
    @Autowired
    private EntityDataFactory entityFactory

    private Domain domain
    private Domain domain1
    private Catalog catalog
    private CatalogItem item1
    private CatalogItem otherItem
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
    private Client client
    private Client secondClient
    private Domain domain3
    private Catalog catalog1
    String salt = "salt-for-etag"

    def setup() {
        ETag.setSalt(salt)
        txTemplate.execute {
            client = newClient {
                id = clientId
            }

            domain = newDomain {
                description = "ISO/IEC"
                abbreviation = "ISO"
                name = "ISO"
                authority = 'ta'
                revision = '1'
                templateVersion = '1.0'
                domainTemplate = domainTemplate
            }
            catalog = newCatalog(domain) {
                name= 'a'
            }

            item1 = newCatalogItem(catalog) {
                element = newControl(it) {
                    name = 'c1'
                }
            }
            CatalogItem item2 = newCatalogItem(catalog) {
                element = newControl(it) {
                    name = 'c2'
                }
            }
            CatalogItem item3 = newCatalogItem(catalog) {
                element = newControl(it) {
                    name = 'c3'
                }
                tailoringReferences = [
                    newTailoringReference(it) {
                        catalogItem = item2
                        referenceType = TailoringReferenceType.COPY
                    },
                    newTailoringReference(it) {
                        catalogItem = item1
                        referenceType = TailoringReferenceType.COPY_ALWAYS
                    }
                ] as Set
            }
            CatalogItem item4 = newCatalogItem(catalog) {
                element = newProcess(it) {
                    name = 'p1'
                    description = "a process example entry"
                }
                tailoringReferences = [
                    newTailoringReference(it) {
                        catalogItem = item1
                        referenceType = TailoringReferenceType.LINK
                    },
                    newTailoringReference(it) {
                        catalogItem = item2
                        referenceType = TailoringReferenceType.LINK
                    }
                ] as Set
            }

            catalog.catalogItems = [item1, item2, item3, item4] as Set

            domain1 = newDomain {
                description = "ISO/IEC2"
                abbreviation = "ISO"
                name = "ISO"
                authority = 'ta'
                revision = '1'
                templateVersion = '1.0'
            }

            client.domains = [domain, domain1] as Set
            client = clientRepository.save(client)

            domain = client.domains.toList().get(0)
            domain1 = client.domains.toList().get(1)
            catalog = domain.catalogs.first()
            item1 = catalog.catalogItems.first()
            domain3 = newDomain {
                abbreviation = "D1"
                name = "Domain 1"
                authority = 'ta'
                revision = '1'
                templateVersion = '1.0'
            }
            catalog1 = newCatalog(domain3) {
                name = 'b'
                catalogItems = [
                    newCatalogItem(it) {
                        element = newControl(it) {
                            name = 'c15'
                        }
                    }
                ]
            }

            secondClient = newClient()
            secondClient.addToDomains(domain3)
            secondClient = clientRepository.save(secondClient)

            domain3 = secondClient.getDomains().iterator().next()
            catalog1 = domain3.catalogs.first()
            otherItem =catalog1.catalogItems.first()
        }
    }



    @WithUserDetails("user@domain.example")
    def "retrieve a catalog"() {
        given: "a catalog"

        when: "a request is made to the server"
        def results = get("/catalogs/${catalog.id.uuidValue()}")

        then: "the catalog is found"
        results.andExpect(status().isOk())
        and:
        def result = parseJson(results)
        result.id == catalog.id.uuidValue()
        result.domainTemplate.targetUri == "http://localhost/domains/"+domain.id.uuidValue()

        and: "it contains a reference to its items"
        result.catalogItems.size() == 4
        when:
        def refc1 = result.catalogItems.find {it.displayName.contains('c1')}
        then:
        refc1.displayName == 'null-- c1'
        refc1.targetUri.contains(item1.dbId)
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a catalog for wrong client"() {
        given: "a catalog"

        when: "a request is made to the server"
        def results = get("/catalogs/${catalog1.id.uuidValue()}", false)

        then: "the data is rejected"
        ClientBoundaryViolationException ex = thrown()

        and: "the reason is given"
        ex.message =~ /This object is not accessable from this client./
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all catalogs"() {
        given: "two catalogs"

        Catalog catalog1 = newCatalog(domain1) {
            name = 'c'
        }

        txTemplate.execute {
            domain1 = domainRepository.save(domain1)
        }
        catalog1 = domain1.catalogs.first()

        when: "a request is made to the server"
        def results = get("/catalogs?")

        then: "the catalogs are found"
        results.andExpect(status().isOk())
        and:
        def result = parseJson(results)
        result.size() == 2
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a catalog item"() {
        given: "a saved catalogitem with a catalog"

        when: "a request is made to the server"
        def results = get("/catalogs/${catalog.id.uuidValue()}/items/${item1.id.uuidValue()}")
        String expectedETag = DigestUtils.sha256Hex(item1.id.uuidValue() + "_" + salt + "_" + Long.toString(domain.version))

        then: "the item is found"
        results.andExpect(status().isOk())
        and: "the eTag is set"
        String eTag = results.andReturn().response.getHeader("ETag")
        eTag != null
        getTextBetweenQuotes(eTag).equals(expectedETag)
        and:
        def result = parseJson(results)
        result.id == item1.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a catalog item wrong client"() {
        given: "a saved catalogitem with a catalog"

        when: "a request is made to the server"
        def results = get("/catalogs/${catalog1.id.uuidValue()}/items/${otherItem.id.uuidValue()}", false)

        then: "the data is rejected"
        NotFoundException ex = thrown()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all items for a catalog"() {
        given: "the created catalogitems"

        when: "a request is made to the server"
        def results = get("/catalogs/${catalog.dbId}/items")

        then: "the domains are returned"
        results.andExpect(status().isOk())
        when:
        def result = parseJson(results)
        then:
        result.size == 4
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all items for a catalog wrong client"() {
        given: "the created catalogitems"

        when: "a request is made to the server"
        def results = get("/catalogs/${catalog1.dbId}/items", false)

        then: "the data is rejected"
        NotFoundException ex = thrown()
    }
}