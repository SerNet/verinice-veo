/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DocumentRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Integration test for the document controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
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

    def setup() {
        txTemplate.execute {
            def client= createTestClient()
            domain = newDomain(client) {
                abbreviation = "D"
                name = "Domain"
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.DOCUMENT, it) {
                    subTypes = [
                        Manual: newSubTypeDefinition()
                    ]
                })
            }

            domain1 = newDomain(client) {
                abbreviation = "D1"
                name = "Domain 1"
            }

            client = clientRepository.save(client)

            unit = newUnit(client) {
                name = "Test unit"
            }
            unitRepository.save(unit)
        }
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
        def results = get("/documents/${document.idAsString}")

        then: "the eTag is set"
        getETag(results) != null

        and:
        def result = parseJson(results)
        result._self == "http://localhost/documents/${document.idAsString}"
        result.name == 'Test document-1'
        result.owner.targetUri == "http://localhost/units/"+unit.idAsString
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

        when: "all documents in the unit are requested"
        def result = parseJson(get("/documents?unit=${unit.idAsString}"))

        then:
        result.items*.name.sort() == [
            'Test document-1',
            'Test document-2'
        ]
    }

    @WithUserDetails("user@domain.example")
    def "retrieving all documents for a unit returns composite elements and their parts"() {
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
        def result = parseJson(get("/documents?unit=${unit.idAsString}"))

        then: "the documents are returned"
        result.items*.name as Set == [
            'Test document-1',
            'Test composite document-1'
        ] as Set
    }

    @WithUserDetails("user@domain.example")
    def "delete a document"() {
        given: "an existing document"
        def document = txTemplate.execute {
            documentRepository.save(newDocument(unit))
        }

        when: "a delete request is sent to the server"
        delete("/documents/${document.idAsString}")

        then: "the document is deleted"
        documentRepository.findById(document.id).empty
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
        delete("/documents/${document.idAsString}")

        then: "the document is deleted"
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
