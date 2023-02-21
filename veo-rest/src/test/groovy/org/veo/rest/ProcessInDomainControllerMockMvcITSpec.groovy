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
class ProcessInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository

    private String unitId
    private String testDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "get process in a domain"() {
        given: "an process with linked process and a part"
        def assetId = parseJson(post("/assets", [
            name: "Market investigation results",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def partId = parseJson(post("/processes", [
            name: "Promotion",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "BusinessProcess",
                    status: "NEW"
                ]
            ]
        ])).resourceId
        def processId = parseJson(post("/processes", [
            name: "Marketing",
            abbreviation: "M",
            description: "Catering to the target market",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "BusinessProcess",
                    status: "NEW",
                    riskValues: [
                        riskyDef: [
                            potentialImpacts: [
                                C: 1
                            ]
                        ]
                    ]
                ]
            ],
            customAspects: [
                general: [
                    attributes: [
                        complexity: "high"
                    ]
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
        def response = parseJson(get("/domians/$testDomainId/processes/$processId"))

        then: "basic properties are contained"
        response.id == processId
        response.type == "process"
        response._self == "http://localhost/domians/$testDomainId/processes/$processId"
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
        response.links.necessaryData[0].target.targetInDomainUri == "http://localhost/domians/$testDomainId/assets/$assetId"
        response.links.necessaryData[0].target.associatedWithDomain == false
        response.links.necessaryData[0].target.subType == null
        response.links.necessaryData[0].attributes.essential
        response.riskValues.riskyDef.potentialImpacts.C == 1

        and: "parts"
        response.parts[0].targetUri == "http://localhost/processes/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domians/$testDomainId/processes/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "BusinessProcess"
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
        with(parseJson(get("/domians/$testDomainId/processes?size=10&sortBy=designator"))) {
            totalItemCount == 15
            page == 0
            pageCount == 2
            items*.name == (1..10).collect { "process $it" }
            items*.subType =~ ["BusinessProcess"]
        }

        and: "page 2 to be available"
        with(parseJson(get("/domians/$testDomainId/processes?size=10&page=1&sortBy=designator"))) {
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
        get("/domians/$testDomainId/processes/$randomProcessId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Process with ID $randomProcessId not found"
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
        get("/domians/$randomDomainId/processes/$processId", 404)

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
        get("/domians/$testDomainId/processes/$processId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Process $processId is not associated with domain $testDomainId"
    }
}
