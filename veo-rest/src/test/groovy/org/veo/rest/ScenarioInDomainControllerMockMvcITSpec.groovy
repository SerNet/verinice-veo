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
class ScenarioInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository

    private String unitId
    private String testDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "get scenario in a domain"() {
        given: "an scenario with linked person and a part"
        def personId = parseJson(post("/persons", [
            name: "Mac Hack",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def partId = parseJson(post("/scenarios", [
            name: "Credential recycling",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Attack",
                    status: "NEW"
                ]
            ]
        ])).resourceId
        def scenarioId = parseJson(post("/scenarios", [
            name: "Brute-force attack",
            abbreviation: "BFA",
            description: "An attacker guesses a password by trying out many random strings",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Attack",
                    status: "NEW",
                    riskValues: [
                        riskyDef: [
                            potentialProbability: 1,
                            potentialProbabilityExplanation: "It happens"
                        ]
                    ]
                ]
            ],
            customAspects: [
                help: [
                    attributes: [
                        technicalArticle: "https://test.test/brute-force.html"
                    ]
                ]
            ],
            parts: [
                [ targetUri:"/scenarios/$partId" ]
            ],
            links: [
                expert: [
                    [
                        target: [targetUri: "/persons/$personId"],
                        attributes: [
                            experienceSince: "1988-08-08"
                        ]
                    ]
                ]
            ]
        ])).resourceId

        when: "fetching it in the domain context"
        def response = parseJson(get("/domians/$testDomainId/scenarios/$scenarioId"))

        then: "basic properties are contained"
        response.id == scenarioId
        response.type == "scenario"
        response._self == "http://localhost/domians/$testDomainId/scenarios/$scenarioId"
        response.name == "Brute-force attack"
        response.abbreviation == "BFA"
        response.description == "An attacker guesses a password by trying out many random strings"
        response.designator =~ /SCN-\d+/
        response.owner.targetUri == "http://localhost/units/$unitId"
        response.createdBy == "user@domain.example"
        response.createdAt != null
        response.updatedBy == "user@domain.example"
        response.updatedAt == response.createdAt

        and: "domain-specific properties"
        response.subType == "Attack"
        response.status == "NEW"
        response.customAspects.help.technicalArticle == "https://test.test/brute-force.html"
        response.links.expert[0].target.targetUri == "http://localhost/persons/$personId"
        response.links.expert[0].target.targetInDomainUri == "http://localhost/domians/$testDomainId/persons/$personId"
        response.links.expert[0].target.associatedWithDomain == false
        response.links.expert[0].target.subType == null
        response.links.expert[0].attributes.experienceSince == "1988-08-08"
        response.riskValues.riskyDef.potentialProbability == 1
        response.riskValues.riskyDef.potentialProbabilityExplanation == "It happens"

        and: "parts"
        response.parts[0].targetUri == "http://localhost/scenarios/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domians/$testDomainId/scenarios/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "Attack"
    }

    def "missing scenario is handled"() {
        given: "a non-existing scenario ID"
        def randomScenarioId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domians/$testDomainId/scenarios/$randomScenarioId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Scenario with ID $randomScenarioId not found"
    }

    def "missing domain is handled"() {
        given: "a scenario in a domain"
        def scenarioId = parseJson(post("/scenarios", [
            name: "Some scenario",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Attack",
                    status: "NEW"
                ]
            ]
        ])).resourceId
        def randomDomainId = randomUUID()

        when: "trying to fetch the scenario in a non-existing domain"
        get("/domians/$randomDomainId/scenarios/$scenarioId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Domain with ID $randomDomainId not found"
    }

    def "unassociated scenario is handled"() {
        given: "a scenario without any domains"
        def scenarioId = parseJson(post("/scenarios", [
            name: "Unassociated scenario",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId

        when:
        get("/domians/$testDomainId/scenarios/$scenarioId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Scenario $scenarioId is not associated with domain $testDomainId"
    }
}
