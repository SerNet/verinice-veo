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
import org.veo.core.repository.PersonRepository
import org.veo.core.repository.UnitRepository

@WithUserDetails("user@domain.example")
class PersonInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository
    @Autowired
    private PersonRepository personRepository

    private String unitId
    private String testDomainId
    private String dsgvoTestDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        dsgvoTestDomainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
        client = clientDataRepository.findById(client.idAsString).get()
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "CRUD person in domain contexts"() {
        given: "a person with linked scope and a part"
        def scopeId = parseJson(post("/scopes", [
            name: "Hack Inc.",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def partId = parseJson(post("/domains/$testDomainId/persons", [
            name: "Harry's rubber duck",
            owner: [targetUri: "/units/$unitId"],
            subType: "Programmer",
            status: "REVIEWING",
        ])).resourceId
        def personId = parseJson(post("/domains/$testDomainId/persons", [
            name: "Harry Larry",
            abbreviation: "HL",
            description: "Typing swiftly, thinking slowly",
            owner: [targetUri: "/units/$unitId"],
            subType: "Programmer",
            status: "CODING",
            customAspects: [
                general: [
                    dateOfBirth: "1999-12-31"
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
        def response = parseJson(get("/domains/$testDomainId/persons/$personId"))

        then: "basic properties are contained"
        response.id == personId
        response.type == "person"
        response._self == "http://localhost/domains/$testDomainId/persons/$personId"
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
        response.links.employer[0].target.targetInDomainUri == "http://localhost/domains/$testDomainId/scopes/$scopeId"
        response.links.employer[0].target.associatedWithDomain == false
        response.links.employer[0].target.subType == null
        response.links.employer[0].attributes.employedSince == "2022-08-01"

        and: "parts"
        response.parts[0].targetUri == "http://localhost/persons/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domains/$testDomainId/persons/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "Programmer"

        when: "associating person with a second domain"
        post("/domains/$dsgvoTestDomainId/persons/$personId", [
            subType: "PER_Person",
            status: "IN_PROGRESS"
        ], 200)

        and: "fetching person in second domain"
        def personInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/persons/$personId")) as Map

        then: "it contains basic values"
        personInDsgvo.name == "Harry Larry"
        personInDsgvo.description == "Typing swiftly, thinking slowly"

        and: "values for second domain"
        personInDsgvo.subType == "PER_Person"
        personInDsgvo.status == "IN_PROGRESS"

        and: "no values for original domain"
        personInDsgvo.customAspects.general == null

        when: "updating and reloading the person from the viewpoint of the second domain"
        personInDsgvo.description = "New description"
        personInDsgvo.status = "ARCHIVED"
        personInDsgvo.customAspects.person_generalInformation = [
            person_generalInformation_givenName: "Harry"
        ]
        put("/domains/$dsgvoTestDomainId/persons/$personId", personInDsgvo, [
            'If-Match': getETag(get("/domains/$dsgvoTestDomainId/persons/$personId"))
        ], 200)
        personInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/persons/$personId"))

        then: "updated values are present"
        personInDsgvo.description == "New description"
        personInDsgvo.status == "ARCHIVED"
        personInDsgvo.customAspects.person_generalInformation.person_generalInformation_givenName == "Harry"

        and: "values for original domain are still absent"
        personInDsgvo.customAspects.general == null

        when: "fetching the person from the viewpoint of the original domain again"
        def personInTestdomain = parseJson(get("/domains/$testDomainId/persons/$personId"))

        then: "values for original domain are unchanged"
        personInTestdomain.subType == "Programmer"
        personInTestdomain.status == "CODING"
        personInTestdomain.customAspects.general.dateOfBirth == "1999-12-31"

        and: "some basic values have been updated"
        personInTestdomain.name == "Harry Larry"
        personInTestdomain.description == "New description"

        and: "values for the second domain are absent"
        personInTestdomain.customAspects.person_generalInformation == null
    }

    def "missing person is handled"() {
        given: "a non-existing person ID"
        def randomPersonId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domains/$testDomainId/persons/$randomPersonId", 404)

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
        get("/domains/$randomDomainId/persons/$personId", 404)

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
        get("/domains/$testDomainId/persons/$personId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Person $personId is not associated with domain $testDomainId"
    }
}
