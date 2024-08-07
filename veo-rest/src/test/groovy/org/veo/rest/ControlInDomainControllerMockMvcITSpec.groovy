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
import org.veo.core.entity.Client
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.repository.ControlRepository
import org.veo.core.repository.UnitRepository

@WithUserDetails("user@domain.example")
class ControlInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository
    @Autowired
    private ControlRepository controlRepository

    private String unitId
    private String testDomainId
    private String dsgvoTestDomainId
    private Client client

    def setup() {
        client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        dsgvoTestDomainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
        client = clientRepository.getById(client.id)
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "CRUD control in domain contexts"() {
        given: "a control with linked document and a part"
        def documentId = parseJson(post("/domains/$testDomainId/documents", [
            name: "Encryption for dummies",
            owner: [targetUri: "/units/$unitId"],
            subType: "Manual",
            status: "OUTDATED",
        ])).resourceId
        def partId = parseJson(post("/domains/$testDomainId/controls", [
            name: "Encrypt user messages",
            owner: [targetUri: "/units/$unitId"],
            subType: "TOM",
            status: "NEW"
        ])).resourceId
        def controlId = parseJson(post("/domains/$testDomainId/controls", [
            name: "End-to-end encryption",
            abbreviation: "E2EE",
            description: "A security method that keeps messages secure",
            owner: [targetUri: "/units/$unitId"],
            subType: "TOM",
            status: "NEW",
            riskValues: [
                riskyDef: [
                    implementationStatus: 1
                ]
            ],
            customAspects: [
                implementation: [
                    explanation: "Data is encrypted / decrypted by the clients, not by the server"
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
        def response = parseJson(get("/domains/$testDomainId/controls/$controlId"))

        then: "basic properties are contained"
        response.id == controlId
        response.type == "control"
        response._self == "http://localhost/domains/$testDomainId/controls/$controlId"
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
        response.links.literature[0].target.targetInDomainUri == "http://localhost/domains/$testDomainId/documents/$documentId"
        response.links.literature[0].target.associatedWithDomain
        response.links.literature[0].target.subType == "Manual"
        response.links.literature[0].attributes.chapters == [2, 7, 8]
        response.riskValues.riskyDef.implementationStatus == 1

        and: "parts"
        response.parts[0].targetUri == "http://localhost/controls/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domains/$testDomainId/controls/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "TOM"

        and: "it conforms to the JSON schema"
        validate(response, get("/domains/$testDomainId/controls/json-schema")).empty

        when: "associating control with a second domain"
        post("/domains/$dsgvoTestDomainId/controls/$controlId", [
            subType: "CTL_TOM",
            status: "IN_PROGRESS"
        ], 200)

        and: "fetching control in second domain"
        def controlInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/controls/$controlId")) as Map

        then: "it contains basic values"
        controlInDsgvo.name == "End-to-end encryption"
        controlInDsgvo.description == "A security method that keeps messages secure"

        and: "values for second domain"
        controlInDsgvo.subType == "CTL_TOM"
        controlInDsgvo.status == "IN_PROGRESS"

        and: "no values for original domain"
        controlInDsgvo.customAspects.implementation == null

        when: "updating and reloading the control from the viewpoint of the second domain"
        controlInDsgvo.description = "New description"
        controlInDsgvo.status = "ARCHIVED"
        controlInDsgvo.customAspects.control_dataProtection = [
            control_dataProtection_objectives: [
                "control_dataProtection_objectives_integrity"
            ]
        ]
        put("/domains/$dsgvoTestDomainId/controls/$controlId", controlInDsgvo, [
            'If-Match': getETag(get("/domains/$dsgvoTestDomainId/controls/$controlId"))
        ], 200)
        controlInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/controls/$controlId"))

        then: "updated values are present"
        controlInDsgvo.description == "New description"
        controlInDsgvo.status == "ARCHIVED"
        controlInDsgvo.customAspects.control_dataProtection.control_dataProtection_objectives == [
            "control_dataProtection_objectives_integrity"
        ]

        and: "values for original domain are still absent"
        controlInDsgvo.customAspects.implementation == null

        when: "fetching the control from the viewpoint of the original domain again"
        def controlInTestdomain = parseJson(get("/domains/$testDomainId/controls/$controlId"))

        then: "values for original domain are unchanged"
        controlInTestdomain.subType == "TOM"
        controlInTestdomain.status == "NEW"
        controlInTestdomain.customAspects.implementation.explanation == "Data is encrypted / decrypted by the clients, not by the server"

        and: "some basic values have been updated"
        controlInTestdomain.name == "End-to-end encryption"
        controlInTestdomain.description == "New description"

        and: "values for the second domain are absent"
        controlInTestdomain.customAspects.control_dataProtection == null
    }

    def "get all control implementations of a control in a domain"() {
        given: "15 control and one scope in the domain"
        def controlId = parseJson(post("/domains/$testDomainId/controls", [
            name: "End-to-end encryption",
            abbreviation: "E2EE",
            description: "A security method that keeps messages secure",
            owner: [targetUri: "/units/$unitId"],
            subType: "TOM",
            status: "NEW",
        ])).resourceId

        (1..15).forEach {
            post("/domains/$testDomainId/scopes", [
                name: "Risky scope $it",
                owner: [targetUri: "/units/$unitId"],
                subType: "Company",
                status: "NEW",
                controlImplementations: [
                    [control: ["targetUri": "/controls/$controlId"]],
                ]
            ])
        }

        expect: "page 1 to be available"
        with(parseJson(get("/domains/$testDomainId/controls/$controlId/control-implementations?size=10&sortBy=owner.name"))) {
            totalItemCount == 15
            page == 0
            pageCount == 2
            items*.owner.name == (1..10).collect { "Risky scope $it" }
            items*.owner.subType =~ ["Company"]
        }

        and: "page 2 to be available"
        with(parseJson(get("/domains/$testDomainId/controls/$controlId/control-implementations?size=10&page=1&sortBy=owner.name"))) {
            totalItemCount == 15
            page == 1
            pageCount == 2
            items*.owner.name == (11..15).collect { "Risky scope $it" }
            items*.owner.subType =~ ["Company"]
        }

        when: "request with a control not associated with the domain returns 404"
        def nonAssociatedDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        get("/domains/$nonAssociatedDomainId/controls/$controlId/control-implementations?size=10&sortBy=owner.name", 404)

        then:
        thrown(NotFoundException)

        when: "request with non existing domain returns 404"
        def nonExistingDomainId = randomUUID().toString()
        get("/domains/$nonExistingDomainId/controls/$controlId/control-implementations?size=10&sortBy=owner.name", 404)

        then:
        thrown(NotFoundException)

        when: "request with non existing control returns 404"
        def nonExistingControlId = randomUUID().toString()
        get("/domains/$testDomainId/controls/$nonExistingControlId/control-implementations?size=10&sortBy=owner.name", 404)

        then:
        thrown(NotFoundException)
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
        with(parseJson(get("/domains/$testDomainId/controls?size=10&sortBy=designator"))) {
            totalItemCount == 15
            page == 0
            pageCount == 2
            items*.name == (1..10).collect { "control $it" }
            items*.subType =~ ["TOM"]
        }

        and: "page 2 to be available"
        with(parseJson(get("/domains/$testDomainId/controls?size=10&page=1&sortBy=designator"))) {
            totalItemCount == 15
            page == 1
            pageCount == 2
            items*.name == (11..15).collect { "control $it" }
            items*.subType =~ ["TOM"]
        }
    }

    def "risk values can be updated"() {
        given: "a control with risk values"
        def controlId = parseJson(post("/domains/$testDomainId/controls", [
            name: "Risky control",
            owner: [targetUri: "/units/$unitId"],
            subType: "TOM",
            status: "NEW",
            riskValues: [
                riskyDef: [
                    implementationStatus: 0
                ]
            ]
        ])).resourceId

        when: "updating risk values"
        get("/domains/$testDomainId/controls/$controlId").with{getResults ->
            def control = parseJson(getResults)
            control.riskValues.riskyDef.implementationStatus = 1
            put(control._self, control, ["If-Match": getETag(getResults)], 200)
        }

        then: "risk values have been altered"
        with(parseJson(get("/domains/$testDomainId/controls/$controlId"))) {
            riskValues.riskyDef.implementationStatus == 1
        }
    }

    def "missing control is handled"() {
        given: "a non-existing control ID"
        def randomControlId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domains/$testDomainId/controls/$randomControlId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "control $randomControlId not found"
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
        get("/domains/$randomDomainId/controls/$controlId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "domain $randomDomainId not found"
    }

    def "unassociated control is handled"() {
        given: "a control without any domains"
        def controlId = parseJson(post("/controls", [
            name: "Unassociated control",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId

        when:
        get("/domains/$testDomainId/controls/$controlId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Control $controlId is not associated with domain $testDomainId"
    }
}
