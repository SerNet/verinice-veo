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
import org.veo.core.repository.IncidentRepository
import org.veo.core.repository.UnitRepository

@WithUserDetails("user@domain.example")
class IncidentInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository
    @Autowired
    private IncidentRepository incidentRepository

    private String unitId
    private String testDomainId
    private String dsgvoTestDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        dsgvoTestDomainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "CRUD incident in domain contexts"() {
        given: "an incident with linked person and a part"
        def personId = parseJson(post("/persons", [
            name: "Master of disaster",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def partId = parseJson(post("/domains/$testDomainId/incidents", [
            name: "part of the disaster",
            owner: [targetUri: "/units/$unitId"],
            subType: "DISASTER",
            status: "INVESTIGATED",
        ])).resourceId
        def incidentId = parseJson(post("/domains/$testDomainId/incidents", [
            name: "Big disaster",
            abbreviation: "BD",
            description: "Something really bad happened.",
            owner: [targetUri: "/units/$unitId"],
            subType: "DISASTER",
            status: "DETECTED",
            customAspects: [
                general: [
                    timeOfOccurrence: "2023-02-10T12:00:00.000Z"
                ]
            ],
            parts: [
                [ targetUri:"/incidents/$partId" ]
            ],
            links: [
                responsiblePerson: [
                    [
                        target: [targetUri: "/persons/$personId"],
                        attributes: [
                            takesAllTheBlame: true
                        ]
                    ]
                ]
            ]
        ])).resourceId

        when: "fetching it in the domain context"
        def response = parseJson(get("/domains/$testDomainId/incidents/$incidentId"))

        then: "basic properties are contained"
        response.id == incidentId
        response.type == "incident"
        response._self == "http://localhost/domains/$testDomainId/incidents/$incidentId"
        response.name == "Big disaster"
        response.abbreviation == "BD"
        response.description == "Something really bad happened."
        response.designator =~ /INC-\d+/
        response.owner.targetUri == "http://localhost/units/$unitId"
        response.createdBy == "user@domain.example"
        response.createdAt != null
        response.updatedBy == "user@domain.example"
        response.updatedAt == response.createdAt

        and: "domain-specific properties"
        response.subType == "DISASTER"
        response.status == "DETECTED"
        response.customAspects.general.timeOfOccurrence == "2023-02-10T12:00:00.000Z"
        response.links.responsiblePerson[0].target.targetUri == "http://localhost/persons/$personId"
        response.links.responsiblePerson[0].target.targetInDomainUri == "http://localhost/domains/$testDomainId/persons/$personId"
        response.links.responsiblePerson[0].target.associatedWithDomain == false
        response.links.responsiblePerson[0].target.subType == null
        response.links.responsiblePerson[0].attributes.takesAllTheBlame

        and: "parts"
        response.parts[0].targetUri == "http://localhost/incidents/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domains/$testDomainId/incidents/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "DISASTER"

        when: "associating incident with a second domain"
        post("/domains/$dsgvoTestDomainId/incidents/$incidentId", [
            subType: "INC_Incident",
            status: "IN_PROGRESS"
        ], 200)

        and: "fetching incident in second domain"
        def incidentInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/incidents/$incidentId")) as Map

        then: "it contains basic values"
        incidentInDsgvo.name == "Big disaster"
        incidentInDsgvo.description == "Something really bad happened."

        and: "values for second domain"
        incidentInDsgvo.subType == "INC_Incident"
        incidentInDsgvo.status == "IN_PROGRESS"

        and: "no values for original domain"
        incidentInDsgvo.customAspects.general == null

        when: "updating and reloading the incident from the viewpoint of the second domain"
        incidentInDsgvo.description = "New description"
        incidentInDsgvo.status = "ARCHIVED"
        incidentInDsgvo.customAspects.incident_cause = [
            incident_cause_details: "Somebody made a big mistake"
        ]
        put("/domains/$dsgvoTestDomainId/incidents/$incidentId", incidentInDsgvo, [
            'If-Match': getETag(get("/domains/$dsgvoTestDomainId/incidents/$incidentId"))
        ], 200)
        incidentInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/incidents/$incidentId"))

        then: "updated values are present"
        incidentInDsgvo.description == "New description"
        incidentInDsgvo.status == "ARCHIVED"
        incidentInDsgvo.customAspects.incident_cause.incident_cause_details == "Somebody made a big mistake"

        and: "values for original domain are still absent"
        incidentInDsgvo.customAspects.details == null

        when: "fetching the incident from the viewpoint of the original domain again"
        def incidentInTestdomain = parseJson(get("/domains/$testDomainId/incidents/$incidentId"))

        then: "values for original domain are unchanged"
        incidentInTestdomain.subType == "DISASTER"
        incidentInTestdomain.status == "DETECTED"
        incidentInTestdomain.customAspects.general.timeOfOccurrence == "2023-02-10T12:00:00.000Z"

        and: "some basic values have been updated"
        incidentInTestdomain.name == "Big disaster"
        incidentInTestdomain.description == "New description"

        and: "values for the second domain are absent"
        incidentInTestdomain.customAspects.incident_cause == null
    }

    def "get all incidents in a domain"() {
        given: "15 incidents in the domain & one unassociated incident"
        (1..15).forEach {
            post("/incidents", [
                name: "incident $it",
                owner: [targetUri: "/units/$unitId"],
                domains: [
                    (testDomainId): [
                        subType: "DISASTER",
                        status: "DETECTED",
                    ]
                ]
            ])
        }
        post("/incidents", [
            name: "unassociated incident",
            owner: [targetUri: "/units/$unitId"]
        ])

        expect: "page 1 to be available"
        with(parseJson(get("/domains/$testDomainId/incidents?size=10&sortBy=designator"))) {
            totalItemCount == 15
            page == 0
            pageCount == 2
            items*.name == (1..10).collect { "incident $it" }
            items*.subType =~ ["DISASTER"]
        }

        and: "page 2 to be available"
        with(parseJson(get("/domains/$testDomainId/incidents?size=10&page=1&sortBy=designator"))) {
            totalItemCount == 15
            page == 1
            pageCount == 2
            items*.name == (11..15).collect { "incident $it" }
            items*.subType =~ ["DISASTER"]
        }
    }

    def "missing incident is handled"() {
        given: "a non-existing incident ID"
        def randomIncidentId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domains/$testDomainId/incidents/$randomIncidentId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Incident with ID $randomIncidentId not found"
    }

    def "missing domain is handled"() {
        given: "an incident in a domain"
        def incidentId = parseJson(post("/incidents", [
            name: "Some incident",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "DISASTER",
                    status: "INVESTIGATED"
                ]
            ]
        ])).resourceId
        def randomDomainId = randomUUID()

        when: "trying to fetch the incident in a non-existing domain"
        get("/domains/$randomDomainId/incidents/$incidentId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Domain with ID $randomDomainId not found"
    }

    def "unassociated incident is handled"() {
        given: "an incident without any domains"
        def incidentId = parseJson(post("/incidents", [
            name: "Unassociated incident",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId

        when:
        get("/domains/$testDomainId/incidents/$incidentId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Incident $incidentId is not associated with domain $testDomainId"
    }
}
