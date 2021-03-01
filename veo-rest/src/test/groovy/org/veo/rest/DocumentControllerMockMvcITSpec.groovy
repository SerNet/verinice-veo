/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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

import org.veo.adapter.presenter.api.DeviatingIdException
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DocumentRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.rest.configuration.WebMvcSecurityConfiguration

import groovy.json.JsonSlurper

/**
 * Integration test for the document controller. Uses mocked spring MVC environment.
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
class DocumentControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private DocumentRepositoryImpl documentRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Domain domain
    private Domain domain1
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
    String salt = "salt-for-etag"

    def setup() {
        txTemplate.execute {
            domain = newDomain {
                abbreviation = "D"
                name = "Domain"
            }

            domain1 = newDomain {
                abbreviation = "D1"
                name = "Domain 1"
            }

            def client= newClient {
                id = clientId
                domains = [domain, domain1] as Set
            }

            unit = newUnit(client) {
                name = "Test unit"
            }

            unit.client = client
            clientRepository.save(client)
            unitRepository.save(unit)
        }
        ETag.setSalt(salt)
    }


    @WithUserDetails("user@domain.example")
    def "create a document"() {
        given: "a request body"

        Map request = [
            name: 'New Document',
            owner: [
                displayName: 'documentDataProtectionObjectivesEugdprEncryption',
                targetUri: '/units/' + unit.id.uuidValue()
            ]
        ]

        when: "a request is made to the server"

        def results = post('/documents', request)

        then: "the document is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new document is returned"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Document created successfully.'
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a document"() {
        given: "a saved document"
        def document = txTemplate.execute {
            documentRepository.save(newDocument(unit) {
                name = 'Test document-1'
            })
        }

        when: "a request is made to the server"
        def results = get("/documents/${document.id.uuidValue()}")
        String expectedETag = DigestUtils.sha256Hex(document.id.uuidValue() + "_" + salt + "_" + Long.toString(document.getVersion()))

        then: "the document is found"
        results.andExpect(status().isOk())
        and: "the eTag is set"
        String eTag = results.andReturn().response.getHeader("ETag")
        eTag != null
        getTextBetweenQuotes(eTag).equals(expectedETag)
        and:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test document-1'
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all documents for a unit"() {
        given: "a saved document"
        def document = newDocument(unit) {
            name = 'Test document-1'
        }
        def document2 = newDocument(unit) {
            name = 'Test document-2'
        }
        (document, document2) = txTemplate.execute {
            [document, document2].collect(documentRepository.&save)
        }

        when: "a request is made to the server"
        def results = get("/documents?unit=${unit.id.uuidValue()}")

        then: "the documents are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2

        result.sort{it.name}.first().name == 'Test document-1'
        result.sort{it.name}.first().owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
        result.sort{it.name}[1].name == 'Test document-2'
        result.sort{it.name}[1].owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "retrieving all documents for a unit returns composite entities and their parts"() {
        given: "a saved document and a composite document containing it"

        txTemplate.execute {
            documentRepository.save( newDocument(unit) {
                name = 'Test composite document-1'
                parts << newDocument(unit) {
                    name = 'Test document-1'
                }
            })
        }

        when: "a request is made to the server"
        def results = get("/documents?unit=${unit.id.uuidValue()}")

        then: "the documents are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2
        result*.name as Set == [
            'Test document-1',
            'Test composite document-1'
        ] as Set
    }

    @WithUserDetails("user@domain.example")
    def "put a document"() {
        given: "a saved document"
        def document = txTemplate.execute {
            documentRepository.save(newDocument(unit) {
                domains = [domain1] as Set
            })
        }

        Map request = [
            name: 'New document-2',
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
            'If-Match': ETag.from(document.id.uuidValue(), 1)
        ]
        def results = put("/documents/${document.id.uuidValue()}", request, headers)

        then: "the document is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New document-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == domain.abbreviation+" "+domain.name
        result.owner.targetUri == "http://localhost/units/"+unit.id.uuidValue()
    }

    @WithUserDetails("user@domain.example")
    def "delete a document"() {
        given: "an existing document"
        def document = txTemplate.execute {
            documentRepository.save(newDocument(unit))
        }

        when: "a delete request is sent to the server"
        def results = delete("/documents/${document.id.uuidValue()}")

        then: "the document is deleted"
        results.andExpect(status().isOk())
        documentRepository.findById(document.id).empty
    }

    @WithUserDetails("user@domain.example")
    def "can't put a document with another document's ID"() {
        given: "two documents"
        def document1 = txTemplate.execute({
            documentRepository.save(newDocument(unit, {
                name = "old name 1"
            }))
        })
        def document2 = txTemplate.execute({
            documentRepository.save(newDocument(unit, {
                name = "old name 2"
            }))
        })
        when: "a put request tries to update document 1 using the ID of document 2"
        Map headers = [
            'If-Match': ETag.from(document1.id.uuidValue(), 1)
        ]
        put("/documents/${document2.id.uuidValue()}", [
            id: document1.id.uuidValue(),
            name: "new name 1",
            owner: [targetUri: '/units/' + unit.id.uuidValue()]
        ], headers, false)
        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }

    @WithUserDetails("user@domain.example")
    def "can put back document"() {
        given: "a new document"
        def id = parseJson(post("/documents/", [
            name: "new name",
            owner: [targetUri: "/units/"+unit.id.uuidValue()]
        ])).resourceId
        def getResult = get("/documents/$id")

        expect: "putting the retrieved document back to be successful"
        put("/documents/$id", parseJson(getResult), [
            "If-Match": getTextBetweenQuotes(getResult.andReturn().response.getHeader("ETag"))
        ])
    }

    @WithUserDetails("user@domain.example")
    def "deleting a part of a composite document does not delete the composite itself"() {
        given: "a document and a composite that contains it"
        def (document, composite) = txTemplate.execute {
            def document = documentRepository.save(newDocument(unit))
            def composite = documentRepository.save(newDocument(unit) {
                parts = [document]
            })
            [document, composite]
        }

        when: "a delete request is sent to the server"
        def results = delete("/documents/${document.id.uuidValue()}")

        then: "the document is deleted"
        results.andExpect(status().isOk())
        documentRepository.findById(document.id).empty

        expect: "the composite is retrieved with no parts"
        txTemplate.execute {
            def proxy = documentRepository.findById(composite.id)
            assert !proxy.empty
            assert proxy.get().parts.empty
            proxy
        }
    }
}
