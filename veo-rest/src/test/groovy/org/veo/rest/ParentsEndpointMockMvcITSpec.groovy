/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Aziz Khalledi.
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
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DocumentRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@WithUserDetails("user@domain.example")
class ParentsEndpointMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private ScopeRepositoryImpl scopeRepository

    @Autowired
    private DocumentRepositoryImpl documentRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Domain testDomain
    private String testDomainId
    private String unitId
    private def client

    def setup() {
        txTemplate.execute {
            client = createTestClient()
            testDomain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            testDomainId = testDomain.idAsString

            unit = newUnit(client) {
                name = "Test unit"
            }

            unitRepository.save(unit)
            unitId = unit.idAsString
        }
    }

    def "retrieve parents of a scope that is a member of other scopes"() {
        given: "a scope that is a member of two other scopes"
        def (memberScope, parentScope1, parentScope2) = txTemplate.execute {
            def member = scopeRepository.save(newScope(unit) {
                name = 'Member Scope'
                abbreviation = 'MS'
                associateWithDomain(testDomain, "SCP_Scope", "NEW")
            })
            [
                member,
                scopeRepository.save(newScope(unit) {
                    name = 'Parent Scope 1'
                    abbreviation = 'PS1'
                    members << member
                    associateWithDomain(testDomain, "SCP_Scope", "NEW")
                }),
                scopeRepository.save(newScope(unit) {
                    name = 'Parent Scope 2'
                    abbreviation = 'PS2'
                    members << member
                    associateWithDomain(testDomain, "SCP_Scope", "NEW")
                })
            ]
        }

        when: "requesting parents of the member scope"
        def result = parseJson(get("/domains/$testDomainId/scopes/${memberScope.id}/parents"))

        then: "both parent scopes are returned"
        result.items.size() == 2
        result.items*.name.sort() == [
            'Parent Scope 1',
            'Parent Scope 2'
        ]
        result.items*.abbreviation.sort() == ['PS1', 'PS2']
        result.items*.subType == ['SCP_Scope', 'SCP_Scope']
        result.totalItemCount == 2
        result.page == 0
    }

    def "retrieve parents of a document as a composite - returns scopes"() {
        given: "a document that is a member of two scopes"
        def (document, scope1, scope2) = txTemplate.execute {
            def doc = documentRepository.save(newDocument(unit) {
                name = 'Test Document'
                abbreviation = 'TD'
                associateWithDomain(testDomain, "DOC_Document", "NEW")
            })
            def s1 = scopeRepository.save(newScope(unit) {
                name = 'Scope 1'
                abbreviation = 'S1'
                members << doc
                associateWithDomain(testDomain, "SCP_Scope", "NEW")
            })
            def s2 = scopeRepository.save(newScope(unit) {
                name = 'Scope 2'
                abbreviation = 'S2'
                members << doc
                associateWithDomain(testDomain, "SCP_Scope", "NEW")
            })
            [doc, s1, s2]
        }

        when: "requesting parents of the document"
        def result = parseJson(get("/domains/$testDomainId/documents/${document.idAsString}/parents"))

        then: "both scopes are returned as parents"
        result.items.size() == 2
        result.items*.name.sort() == ['Scope 1', 'Scope 2']
        result.items*.type == ['scope', 'scope']
        result.totalItemCount == 2
    }

    def "retrieve parents of a document as a composite - returns composites"() {
        given: "a document that is a part of two composite documents"
        def (partDocument, composite1, composite2) = txTemplate.execute {
            def part = documentRepository.save(newDocument(unit) {
                name = 'Part Document'
                abbreviation = 'PD'
                associateWithDomain(testDomain, "DOC_Document", "NEW")
            })
            def comp1 = documentRepository.save(newDocument(unit) {
                name = 'Composite Document 1'
                abbreviation = 'CD1'
                parts << part
                associateWithDomain(testDomain, "DOC_Document", "NEW")
            })
            def comp2 = documentRepository.save(newDocument(unit) {
                name = 'Composite Document 2'
                abbreviation = 'CD2'
                parts << part
                associateWithDomain(testDomain, "DOC_Document", "NEW")
            })
            [part, comp1, comp2]
        }

        when: "requesting parents of the part document"
        def result = parseJson(get("/domains/$testDomainId/documents/${partDocument.idAsString}/parents"))

        then: "both composite documents are returned as parents"
        result.items.size() == 2
        result.items*.name.sort() == [
            'Composite Document 1',
            'Composite Document 2'
        ]
        result.items*.type == ['document', 'document']
        result.totalItemCount == 2
    }

    def "retrieve parents of a document returns both scopes and composites"() {
        given: "a document that is a part of a composite and a member of a scope"
        def (document, compositeDoc, scope) = txTemplate.execute {
            def doc = documentRepository.save(newDocument(unit) {
                name = 'Mixed Document'
                abbreviation = 'MD'
                associateWithDomain(testDomain, "DOC_Document", "NEW")
            })
            def composite = documentRepository.save(newDocument(unit) {
                name = 'Composite Document'
                abbreviation = 'CD'
                parts << doc
                associateWithDomain(testDomain, "DOC_Document", "NEW")
            })
            def sc = scopeRepository.save(newScope(unit) {
                name = 'Parent Scope'
                abbreviation = 'PS'
                members << doc
                associateWithDomain(testDomain, "SCP_Scope", "NEW")
            })
            [doc, composite, sc]
        }

        when: "requesting parents of the document"
        def result = parseJson(get("/domains/$testDomainId/documents/${document.idAsString}/parents"))

        then: "both composite document and scope are returned"
        result.items.size() == 2
        result.items*.name.sort() == [
            'Composite Document',
            'Parent Scope'
        ]
        def types = result.items*.type.sort()
        types == ['document', 'scope']
        result.totalItemCount == 2
    }

    def "parents endpoint pagination works correctly"() {
        given: "a scope with 15 parent scopes"
        def memberScope = txTemplate.execute {
            def member = scopeRepository.save(newScope(unit) {
                name = 'Member Scope'
                abbreviation = 'MS'
                associateWithDomain(testDomain, "SCP_Scope", "NEW")
            })
            (1..15).each { i ->
                scopeRepository.save(newScope(unit) {
                    name = "Parent Scope $i"
                    abbreviation = "PS$i"
                    members << member
                    associateWithDomain(testDomain, "SCP_Scope", "NEW")
                })
            }
            member
        }

        when: "requesting first page with size 10"
        def page1 = parseJson(get("/domains/$testDomainId/scopes/${memberScope.idAsString}/parents?size=10&page=0&sortBy=NAME"))

        then: "first 10 items are returned"
        page1.items.size() == 10
        page1.items*.name == (1..10).collect{"Parent Scope $it"}
        page1.totalItemCount == 15
        page1.page == 0
        page1.pageCount == 2

        when: "requesting second page"
        def page2 = parseJson(get("/domains/$testDomainId/scopes/${memberScope.idAsString}/parents?size=10&page=1&sortBy=NAME"))

        then: "remaining 5 items are returned"
        page2.items.size() == 5
        page2.items*.name == (11..15).collect{"Parent Scope $it"}
        page2.totalItemCount == 15
        page2.page == 1
        page2.pageCount == 2
    }

    def "parents endpoint sorting works correctly"() {
        given:
        def document = txTemplate.execute {
            def doc = documentRepository.save(newDocument(unit) {
                name = 'Test Document'
                associateWithDomain(testDomain, "DOC_Document", "NEW")
            })
            def composite = documentRepository.save(newDocument(unit) {
                name = 'Composite Document'
                abbreviation = 'CD'
                designator = 'DOC-30'
                parts << doc
                associateWithDomain(testDomain, "DOC_Document", "NEW")
            })
            scopesToCreate.each { scopeData ->
                scopeRepository.save(newScope(unit) {
                    name = scopeData.name
                    abbreviation = scopeData.abbreviation
                    designator = scopeData.designator
                    members << doc
                    associateWithDomain(testDomain, "SCP_Scope", scopeData.status ?: "NEW")
                })
            }

            doc
        }

        when:
        def result = parseJson(
                get("/domains/$testDomainId/documents/${document.idAsString}/parents?sortBy=$sortBy&sortOrder=$sortOrder")
                )

        then:
        result.items*.get(sortField) == expectedSortedValues

        where:
        sortBy        | sortOrder | sortField     | scopesToCreate                                         | expectedSortedValues
        "DESIGNATOR"  | "asc"     | "designator"  | [
            [designator:'SCP-003'],
            [designator:'SCP-001'],
            [designator:'SCP-002']
        ] | [
            'DOC-30',
            'SCP-001',
            'SCP-002',
            'SCP-003'
        ]
        "NAME"        | "asc"     | "name"        | [
            [name:'Zebra Scope'],
            [name:'Alpha Scope'],
            [name:'Beta Scope']
        ]          | [
            'Alpha Scope',
            'Beta Scope',
            'Composite Document',
            'Zebra Scope'
        ]
        // Lowercase to test case-insensitive sorting
        "name"        | "asc"     | "name"        | [
            [name:'Zebra Scope'],
            [name:'Alpha Scope'],
            [name:'Beta Scope']
        ]          | [
            'Alpha Scope',
            'Beta Scope',
            'Composite Document',
            'Zebra Scope'
        ]
        "ABBREVIATION"| "asc"     | "abbreviation"| [
            [abbreviation:'ZZZ'],
            [abbreviation:'AAA'],
            [abbreviation:'MMM']
        ]       | ['AAA', 'CD', 'MMM', 'ZZZ']
        "TYPE" | "desc"    | "type"        | [
            [name:'Doc Parent', abbreviation:'DP', designator:'SCP-001'],
            [name:'Scope Parent', abbreviation:'SP', designator:'SCP-002']
        ] | [
            'scope',
            'scope',
            'document'
        ]
    }

    def "parents are sorted by type per default"() {
        given: "a document with both scope and document composite parents"
        def document = txTemplate.execute {
            def doc = documentRepository.save(newDocument(unit) {
                name = 'Test Document'
                associateWithDomain(testDomain, "DOC_Document", "NEW")
            })
            scopeRepository.save(newScope(unit) {
                name = 'Scope Parent'
                abbreviation = 'SP'
                members << doc
                associateWithDomain(testDomain, "SCP_Scope", "NEW")
            })
            documentRepository.save(newDocument(unit) {
                name = 'Composite Parent'
                abbreviation = 'CP'
                parts << doc
                associateWithDomain(testDomain, "DOC_Document", "NEW")
            })
            doc
        }

        when: "requesting parents with default sorting (by type)"
        def result = parseJson(get("/domains/$testDomainId/documents/${document.idAsString}/parents"))

        then: "parents are returned (type sorting is implicit in repository)"
        result.items.size() == 2
        result.items*.type == [
            'document',
            'scope'
        ]
        result.totalItemCount == 2
    }

    def "parents endpoint returns empty list for element with no parents"() {
        given: "a scope with no parents"
        def scope = txTemplate.execute {
            scopeRepository.save(newScope(unit) {
                name = 'Lonely Scope'
                associateWithDomain(testDomain, "SCP_Scope", "NEW")
            })
        }

        when: "requesting parents"
        def result = parseJson(get("/domains/$testDomainId/scopes/${scope.idAsString}/parents"))

        then: "empty list is returned"
        result.items.size() == 0
        result.totalItemCount == 0
    }

    def "parents endpoint returns 404 for non-existent element"() {
        given: "a non-existent scope ID"
        def nonExistentId = UUID.randomUUID()

        when: "requesting parents"
        get("/domains/$testDomainId/scopes/$nonExistentId/parents", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "scope $nonExistentId not found"
    }

    def "parents endpoint returns 404 for non-existent domain"() {
        given: "a scope and a non-existent domain ID"
        def scope = txTemplate.execute {
            scopeRepository.save(newScope(unit) {
                name = 'Test Scope'
                associateWithDomain(testDomain, "SCP_Scope", "NEW")
            })
        }
        def nonExistentDomainId = UUID.randomUUID()

        when: "requesting parents with wrong domain"
        get("/domains/$nonExistentDomainId/scopes/${scope.idAsString}/parents", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "domain $nonExistentDomainId not found"
    }

    def "parents endpoint returns only parents from the specified domain"() {
        given: "two domains and a document with parents in both"
        def (document, scope1, scope2, domain2Id) = txTemplate.execute {
            def domain2 = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID)
            def domain2Id = domain2.idAsString

            def doc = documentRepository.save(newDocument(unit) {
                name = 'Multi-Domain Document'
                associateWithDomain(testDomain, "DOC_Document", "NEW")
                associateWithDomain(domain2, "DOC_Document", "NEW")
            })
            def s1 = scopeRepository.save(newScope(unit) {
                name = 'Scope in Test Domain'
                members << doc
                associateWithDomain(testDomain, "SCP_Scope", "NEW")
            })
            def s2 = scopeRepository.save(newScope(unit) {
                name = 'Scope in Domain 2'
                members << doc
                associateWithDomain(domain2, "SCP_Scope", "NEW")
            })
            [doc, s1, s2, domain2Id]
        }

        when: "requesting parents from test domain"
        def result = parseJson(get("/domains/$testDomainId/documents/${document.idAsString}/parents"))

        then: "only scope from test domain is returned"
        result.items.size() == 1
        result.items[0].name == 'Scope in Test Domain'

        when: "requesting parents from the other domain"
        def resultOtherDomain = parseJson(get("/domains/$domain2Id/documents/${document.id}/parents"))

        then:
        resultOtherDomain.items.size() == 1
        resultOtherDomain.items*.name == [
            'Scope in Domain 2'
        ]
    }
}
