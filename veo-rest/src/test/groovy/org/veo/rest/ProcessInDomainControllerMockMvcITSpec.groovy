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
import org.veo.core.repository.ProcessRepository
import org.veo.core.repository.UnitRepository

@WithUserDetails("user@domain.example")
class ProcessInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository
    @Autowired
    private ProcessRepository processRepository

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

    def "CRUD process in domain contexts"() {
        given: "an process with linked process and a part"
        def assetId = parseJson(post("/assets", [
            name: "Market investigation results",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def partId = parseJson(post("/domains/$testDomainId/processes", [
            name: "Promotion",
            owner: [targetUri: "/units/$unitId"],
            subType: "BusinessProcess",
            status: "NEW"
        ])).resourceId
        def processId = parseJson(post("/domains/$testDomainId/processes", [
            name: "Marketing",
            abbreviation: "M",
            description: "Catering to the target market",
            owner: [targetUri: "/units/$unitId"],
            subType: "BusinessProcess",
            status: "NEW",
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        C: 1
                    ]
                ]
            ],
            customAspects: [
                general: [
                    complexity: "high"
                ]
            ],
            parts: [
                [ targetUri:"/processes/$partId" ]
            ],
            links: [
                necessaryData: [
                    [
                        target: [targetUri: "/assets/$assetId"],
                        attributes: [
                            essential: true
                        ]
                    ]
                ]
            ]
        ])).resourceId

        when: "fetching it in the domain context"
        def response = parseJson(get("/domains/$testDomainId/processes/$processId"))

        then: "basic properties are contained"
        response.id == processId
        response.type == "process"
        response._self == "http://localhost/domains/$testDomainId/processes/$processId"
        response.name == "Marketing"
        response.abbreviation == "M"
        response.description == "Catering to the target market"
        response.designator =~ /PRO-\d+/
        response.owner.targetUri == "http://localhost/units/$unitId"
        response.createdBy == "user@domain.example"
        response.createdAt != null
        response.updatedBy == "user@domain.example"
        response.updatedAt == response.createdAt

        and: "domain-specific properties"
        response.subType == "BusinessProcess"
        response.status == "NEW"
        response.customAspects.general.complexity == "high"
        response.links.necessaryData[0].target.targetUri == "http://localhost/assets/$assetId"
        response.links.necessaryData[0].target.targetInDomainUri == "http://localhost/domains/$testDomainId/assets/$assetId"
        response.links.necessaryData[0].target.associatedWithDomain == false
        response.links.necessaryData[0].target.subType == null
        response.links.necessaryData[0].attributes.essential
        response.riskValues.riskyDef.potentialImpacts.C == 1

        and: "parts"
        response.parts[0].targetUri == "http://localhost/processes/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domains/$testDomainId/processes/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "BusinessProcess"

        and: "it conforms to the JSON schema"
        validate(response, get("/domains/$testDomainId/processes/json-schema")).empty

        when: "associating process with a second domain"
        post("/domains/$dsgvoTestDomainId/processes/$processId", [
            subType: "PRO_DataProcessing",
            status: "IN_PROGRESS"
        ], 200)

        and: "fetching process in second domain"
        def processInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/processes/$processId")) as Map

        then: "it contains basic values"
        processInDsgvo.name == "Marketing"
        processInDsgvo.description == "Catering to the target market"

        and: "values for second domain"
        processInDsgvo.subType == "PRO_DataProcessing"
        processInDsgvo.status == "IN_PROGRESS"

        and: "no values for original domain"
        processInDsgvo.customAspects.general == null

        when: "updating and reloading the process from the viewpoint of the second domain"
        processInDsgvo.description = "New description"
        processInDsgvo.status = "ARCHIVED"
        processInDsgvo.customAspects.process_accessAuthorization = [
            process_accessAuthorization_description: "Uhm..."
        ]
        put("/domains/$dsgvoTestDomainId/processes/$processId", processInDsgvo, [
            'If-Match': getETag(get("/domains/$dsgvoTestDomainId/processes/$processId"))
        ], 200)
        processInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/processes/$processId"))

        then: "updated values are present"
        processInDsgvo.description == "New description"
        processInDsgvo.status == "ARCHIVED"
        processInDsgvo.customAspects.process_accessAuthorization.process_accessAuthorization_description == "Uhm..."

        and: "values for original domain are still absent"
        processInDsgvo.customAspects.general == null

        when: "fetching the process from the viewpoint of the original domain again"
        def processInTestdomain = parseJson(get("/domains/$testDomainId/processes/$processId"))

        then: "values for original domain are unchanged"
        processInTestdomain.subType == "BusinessProcess"
        processInTestdomain.status == "NEW"
        processInTestdomain.customAspects.general.complexity == "high"

        and: "some basic values have been updated"
        processInTestdomain.name == "Marketing"
        processInTestdomain.description == "New description"

        and: "values for the second domain are absent"
        processInTestdomain.customAspects.process_accessAuthorization == null
    }

    def "get all processes in a domain"() {
        given: "15 processes in the domain & one unassociated asset"
        (1..15).forEach {
            post("/processes", [
                name: "process $it",
                owner: [targetUri: "/units/$unitId"],
                domains: [
                    (testDomainId): [
                        subType: "BusinessProcess",
                        status: "NEW",
                    ]
                ]
            ])
        }
        post("/processes", [
            name: "unassociated process",
            owner: [targetUri: "/units/$unitId"]
        ])

        expect: "page 1 to be available"
        with(parseJson(get("/domains/$testDomainId/processes?size=10&sortBy=designator"))) {
            totalItemCount == 15
            page == 0
            pageCount == 2
            items*.name == (1..10).collect { "process $it" }
            items*.subType =~ ["BusinessProcess"]
        }

        and: "page 2 to be available"
        with(parseJson(get("/domains/$testDomainId/processes?size=10&page=1&sortBy=designator"))) {
            totalItemCount == 15
            page == 1
            pageCount == 2
            items*.name == (11..15).collect { "process $it" }
            items*.subType =~ ["BusinessProcess"]
        }
    }

    def "missing process is handled"() {
        given: "a non-existing process ID"
        def randomProcessId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domains/$testDomainId/processes/$randomProcessId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Process with ID $randomProcessId not found"
    }

    def "risk values can be updated"() {
        given: "a process with risk values"
        def processId = parseJson(post("/domains/$testDomainId/processes", [
            name: "Risky process",
            owner: [targetUri: "/units/$unitId"],
            subType: "BusinessProcess",
            status: "NEW",
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        C: 0
                    ]
                ]
            ]
        ])).resourceId

        when: "updating risk values"
        get("/domains/$testDomainId/processes/$processId").with{getResults ->
            def process = parseJson(getResults)
            process.riskValues.riskyDef.potentialImpacts.C = 1
            put(process._self, process, ["If-Match": getETag(getResults)], 200)
        }

        then: "risk values have been altered"
        with(parseJson(get("/domains/$testDomainId/processes/$processId"))) {
            riskValues.riskyDef.potentialImpacts.C == 1
        }
    }

    def "missing domain is handled"() {
        given: "a process in a domain"
        def processId = parseJson(post("/processes", [
            name: "Some process",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "BusinessProcess",
                    status: "NEW"
                ]
            ]
        ])).resourceId
        def randomDomainId = randomUUID()

        when: "trying to fetch the process in a non-existing domain"
        get("/domains/$randomDomainId/processes/$processId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Domain with ID $randomDomainId not found"
    }

    def "unassociated process is handled"() {
        given: "a process without any domains"
        def processId = parseJson(post("/processes", [
            name: "Unassociated process",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId

        when:
        get("/domains/$testDomainId/processes/$processId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Process $processId is not associated with domain $testDomainId"
    }
}
