/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import static java.util.UUID.randomUUID

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.repository.DocumentRepository
import org.veo.core.repository.UnitRepository

@WithUserDetails("user@domain.example")
class DocumentInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository
    @Autowired
    private DocumentRepository documentRepository

    private String unitId
    private String testDomainId
    private String dsgvoTestDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        dsgvoTestDomainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "CRUD document in domain contexts"() {
        given: "an document with linked person and a part"
        def personId = parseJson(post("/persons", [
            name: "Ricky Writer",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def partId = parseJson(post("/domians/$testDomainId/documents", [
            name: "ISMS manual changelog",
            owner: [targetUri: "/units/$unitId"],
            subType: "Manual",
            status: "CURRENT",
        ])).resourceId
        def documentId = parseJson(post("/domians/$testDomainId/documents", [
            name: "ISMS manual",
            abbreviation: "KM",
            description: "How we do ISMS",
            owner: [targetUri: "/units/$unitId"],
            subType: "Manual",
            status: "CURRENT",
            customAspects: [
                details: [
                    numberOfPages: 84
                ]
            ],
            parts: [
                [targetUri: "/documents/$partId"]
            ],
            links: [
                author: [
                    [
                        target: [targetUri: "/persons/$personId"],
                        attributes: [
                            writingFinished: "2022-08-09"
                        ]
                    ]
                ]
            ]
        ])).resourceId

        when: "fetching it in the domain context"
        def response = parseJson(get("/domians/$testDomainId/documents/$documentId"))

        then: "basic properties are contained"
        response.id == documentId
        response.type == "document"
        response._self == "http://localhost/domians/$testDomainId/documents/$documentId"
        response.name == "ISMS manual"
        response.abbreviation == "KM"
        response.description == "How we do ISMS"
        response.designator =~ /DOC-\d+/
        response.owner.targetUri == "http://localhost/units/$unitId"
        response.createdBy == "user@domain.example"
        response.createdAt != null
        response.updatedBy == "user@domain.example"
        response.updatedAt == response.createdAt

        and: "domain-specific properties"
        response.subType == "Manual"
        response.status == "CURRENT"
        response.customAspects.details.numberOfPages == 84
        response.links.author[0].target.targetUri == "http://localhost/persons/$personId"
        response.links.author[0].target.targetInDomainUri == "http://localhost/domians/$testDomainId/persons/$personId"
        response.links.author[0].target.associatedWithDomain == false
        response.links.author[0].target.subType == null
        response.links.author[0].attributes.writingFinished == "2022-08-09"

        and: "parts"
        response.parts[0].targetUri == "http://localhost/documents/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domians/$testDomainId/documents/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "Manual"

        when: "associating document with a second domain"
        post("/domians/$dsgvoTestDomainId/documents/$documentId", [
            subType: "DOC_Document",
            status: "IN_PROGRESS"
        ], 200)

        and: "fetching document in second domain"
        def documentInDsgvo = parseJson(get("/domians/$dsgvoTestDomainId/documents/$documentId")) as Map

        then: "it contains basic values"
        documentInDsgvo.name == "ISMS manual"
        documentInDsgvo.description == "How we do ISMS"

        and: "values for second domain"
        documentInDsgvo.subType == "DOC_Document"
        documentInDsgvo.status == "IN_PROGRESS"

        and: "no values for original domain"
        documentInDsgvo.customAspects.details == null

        when: "updating and reloading the document from the viewpoint of the second domain"
        documentInDsgvo.description = "New description"
        documentInDsgvo.status = "ARCHIVED"
        documentInDsgvo.customAspects.document_revision = [
            document_revision_comment: "Well worded"
        ]
        put("/domians/$dsgvoTestDomainId/documents/$documentId", documentInDsgvo, [
            'If-Match': getETag(get("/domians/$dsgvoTestDomainId/documents/$documentId"))
        ], 200)
        documentInDsgvo = parseJson(get("/domians/$dsgvoTestDomainId/documents/$documentId"))

        then: "updated values are present"
        documentInDsgvo.description == "New description"
        documentInDsgvo.status == "ARCHIVED"
        documentInDsgvo.customAspects.document_revision.document_revision_comment == "Well worded"

        and: "values for original domain are still absent"
        documentInDsgvo.customAspects.details == null

        when: "fetching the document from the viewpoint of the original domain again"
        def documentInTestdomain = parseJson(get("/domians/$testDomainId/documents/$documentId"))

        then: "values for original domain are unchanged"
        documentInTestdomain.subType == "Manual"
        documentInTestdomain.status == "CURRENT"
        documentInTestdomain.customAspects.details.numberOfPages == 84

        and: "some basic values have been updated"
        documentInTestdomain.name == "ISMS manual"
        documentInTestdomain.description == "New description"

        and: "values for the second domain are absent"
        documentInTestdomain.customAspects.document_revision == null
    }

    def "get all documents in a domain"() {
        given: "15 documents in the domain & one unassociated document"
        (1..15).forEach {
            post("/documents", [
                name: "document $it",
                owner: [targetUri: "/units/$unitId"],
                domains: [
                    (testDomainId): [
                        subType: "Manual",
                        status: "CURRENT",
                    ]
                ]
            ])
        }
        post("/documents", [
            name: "unassociated document",
            owner: [targetUri: "/units/$unitId"]
        ])

        expect: "page 1 to be available"
        with(parseJson(get("/domians/$testDomainId/documents?size=10&sortBy=designator"))) {
            totalItemCount == 15
            page == 0
            pageCount == 2
            items*.name == (1..10).collect { "document $it" }
            items*.subType =~ ["Manual"]
        }

        and: "page 2 to be available"
        with(parseJson(get("/domians/$testDomainId/documents?size=10&page=1&sortBy=designator"))) {
            totalItemCount == 15
            page == 1
            pageCount == 2
            items*.name == (11..15).collect { "document $it" }
            items*.subType =~ ["Manual"]
        }
    }

    def "missing document is handled"() {
        given: "a non-existing document ID"
        def randomDocumentId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domians/$testDomainId/documents/$randomDocumentId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Document with ID $randomDocumentId not found"
    }

    def "missing domain is handled"() {
        given: "an document in a domain"
        def documentId = parseJson(post("/documents", [
            name: "Some document",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Manual",
                    status: "OUTDATED"
                ]
            ]
        ])).resourceId
        def randomDomainId = randomUUID()

        when: "trying to fetch the document in a non-existing domain"
        get("/domians/$randomDomainId/documents/$documentId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Domain with ID $randomDomainId not found"
    }

    def "unassociated document is handled"() {
        given: "an document without any domains"
        def documentId = parseJson(post("/documents", [
            name: "Unassociated document",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId

        when:
        get("/domians/$testDomainId/documents/$documentId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Document $documentId is not associated with domain $testDomainId"
    }
}
