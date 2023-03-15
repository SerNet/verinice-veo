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
class PersonInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository

    private String unitId
    private String testDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "get person in a domain"() {
        given: "a person with linked scope and a part"
        def scopeId = parseJson(post("/scopes", [
            name: "Hack Inc.",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def partId = parseJson(post("/persons", [
            name: "Harry's rubber duck",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Programmer",
                    status: "REVIEWING"
                ]
            ]
        ])).resourceId
        def personId = parseJson(post("/persons", [
            name: "Harry Larry",
            abbreviation: "HL",
            description: "Typing swiftly, thinking slowly",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Programmer",
                    status: "CODING"
                ]
            ],
            customAspects: [
                general: [
                    attributes: [
                        dateOfBirth: "1999-12-31"
                    ]
                ]
            ],
            parts: [
                [ targetUri:"/persons/$partId" ]
            ],
            links: [
                employer: [
                    [
                        target: [targetUri: "/scopes/$scopeId"],
                        attributes: [
                            employedSince: "2022-08-01"
                        ]
                    ]
                ]
            ]
        ])).resourceId

        when: "fetching it in the domain context"
        def response = parseJson(get("/domians/$testDomainId/persons/$personId"))

        then: "basic properties are contained"
        response.id == personId
        response.type == "person"
        response._self == "http://localhost/domians/$testDomainId/persons/$personId"
        response.name == "Harry Larry"
        response.abbreviation == "HL"
        response.description == "Typing swiftly, thinking slowly"
        response.designator =~ /PER-\d+/
        response.owner.targetUri == "http://localhost/units/$unitId"
        response.createdBy == "user@domain.example"
        response.createdAt != null
        response.updatedBy == "user@domain.example"
        response.updatedAt == response.createdAt

        and: "domain-specific properties"
        response.subType == "Programmer"
        response.status == "CODING"
        response.customAspects.general.dateOfBirth == "1999-12-31"
        response.links.employer[0].target.targetUri == "http://localhost/scopes/$scopeId"
        response.links.employer[0].target.targetInDomainUri == "http://localhost/domians/$testDomainId/scopes/$scopeId"
        response.links.employer[0].target.associatedWithDomain == false
        response.links.employer[0].target.subType == null
        response.links.employer[0].attributes.employedSince == "2022-08-01"

        and: "parts"
        response.parts[0].targetUri == "http://localhost/persons/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domians/$testDomainId/persons/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "Programmer"
    }

    def "missing person is handled"() {
        given: "a non-existing person ID"
        def randomPersonId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domians/$testDomainId/persons/$randomPersonId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Person with ID $randomPersonId not found"
    }

    def "missing domain is handled"() {
        given: "a person in a domain"
        def personId = parseJson(post("/persons", [
            name: "Some person",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Programmer",
                    status: "CODING"
                ]
            ]
        ])).resourceId
        def randomDomainId = randomUUID()

        when: "trying to fetch the person in a non-existing domain"
        get("/domians/$randomDomainId/persons/$personId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Domain with ID $randomDomainId not found"
    }

    def "unassociated person is handled"() {
        given: "a person without any domains"
        def personId = parseJson(post("/persons", [
            name: "Unassociated person",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId

        when:
        get("/domians/$testDomainId/persons/$personId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Person $personId is not associated with domain $testDomainId"
    }
}
