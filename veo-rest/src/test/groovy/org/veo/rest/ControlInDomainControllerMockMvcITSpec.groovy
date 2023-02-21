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
import org.veo.core.repository.UnitRepository

@WithUserDetails("user@domain.example")
class ControlInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository

    private String unitId
    private String testDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "get control in a domain"() {
        given: "a control with linked document and a part"
        def documentId = parseJson(post("/documents", [
            name: "Encryption for dummies",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def partId = parseJson(post("/controls", [
            name: "Encrypt user messages",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "TOM",
                    status: "NEW"
                ]
            ]
        ])).resourceId
        def controlId = parseJson(post("/controls", [
            name: "End-to-end encryption",
            abbreviation: "E2EE",
            description: "A security method that keeps messages secure",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "TOM",
                    status: "NEW",
                    riskValues: [
                        riskyDef: [
                            implementationStatus: 1
                        ]
                    ]
                ]
            ],
            customAspects: [
                implementation: [
                    attributes: [
                        explanation: "Data is encrypted / decrypted by the clients, not by the server"
                    ]
                ]
            ],
            parts: [
                [ targetUri:"/controls/$partId" ]
            ],
            links: [
                literature: [
                    [
                        target: [targetUri: "/documents/$documentId"],
                        attributes: [
                            chapters: [2, 7, 8]
                        ]
                    ]
                ]
            ]
        ])).resourceId

        when: "fetching it in the domain context"
        def response = parseJson(get("/domians/$testDomainId/controls/$controlId"))

        then: "basic properties are contained"
        response.id == controlId
        response.type == "control"
        response._self == "http://localhost/domians/$testDomainId/controls/$controlId"
        response.name == "End-to-end encryption"
        response.abbreviation == "E2EE"
        response.description == "A security method that keeps messages secure"
        response.designator =~ /CTL-\d+/
        response.owner.targetUri == "http://localhost/units/$unitId"
        response.createdBy == "user@domain.example"
        response.createdAt != null
        response.updatedBy == "user@domain.example"
        response.updatedAt == response.createdAt

        and: "domain-specific properties"
        response.subType == "TOM"
        response.status == "NEW"
        response.customAspects.implementation.explanation == "Data is encrypted / decrypted by the clients, not by the server"
        response.links.literature[0].target.targetUri == "http://localhost/documents/$documentId"
        response.links.literature[0].target.targetInDomainUri == "http://localhost/domians/$testDomainId/documents/$documentId"
        response.links.literature[0].target.associatedWithDomain == false
        response.links.literature[0].target.subType == null
        response.links.literature[0].attributes.chapters == [2, 7, 8]
        response.riskValues.riskyDef.implementationStatus == 1

        and: "parts"
        response.parts[0].targetUri == "http://localhost/controls/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domians/$testDomainId/controls/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "TOM"
    }

    def "get all controls in a domain"() {
        given: "15 controls in the domain & one unassociated control"
        (1..15).forEach {
            post("/controls", [
                name: "control $it",
                owner: [targetUri: "/units/$unitId"],
                domains: [
                    (testDomainId): [
                        subType: "TOM",
                        status: "NEW",
                    ]
                ]
            ])
        }
        post("/controls", [
            name: "unassociated control",
            owner: [targetUri: "/units/$unitId"]
        ])

        expect: "page 1 to be available"
        with(parseJson(get("/domians/$testDomainId/controls?size=10&sortBy=designator"))) {
            totalItemCount == 15
            page == 0
            pageCount == 2
            items*.name == (1..10).collect { "control $it" }
            items*.subType =~ ["TOM"]
        }

        and: "page 2 to be available"
        with(parseJson(get("/domians/$testDomainId/controls?size=10&page=1&sortBy=designator"))) {
            totalItemCount == 15
            page == 1
            pageCount == 2
            items*.name == (11..15).collect { "control $it" }
            items*.subType =~ ["TOM"]
        }
    }

    def "missing control is handled"() {
        given: "a non-existing control ID"
        def randomControlId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domians/$testDomainId/controls/$randomControlId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Control with ID $randomControlId not found"
    }

    def "missing domain is handled"() {
        given: "a control in a domain"
        def controlId = parseJson(post("/controls", [
            name: "Some control",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "TOM",
                    status: "OLD"
                ]
            ]
        ])).resourceId
        def randomDomainId = randomUUID()

        when: "trying to fetch the control in a non-existing domain"
        get("/domians/$randomDomainId/controls/$controlId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Domain with ID $randomDomainId not found"
    }

    def "unassociated control is handled"() {
        given: "a control without any domains"
        def controlId = parseJson(post("/controls", [
            name: "Unassociated control",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId

        when:
        get("/domians/$testDomainId/controls/$controlId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Control $controlId is not associated with domain $testDomainId"
    }
}
