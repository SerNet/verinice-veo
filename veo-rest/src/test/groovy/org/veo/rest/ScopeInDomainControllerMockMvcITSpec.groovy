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
class ScopeInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository

    private String unitId
    private String testDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "get scope in a domain"() {
        given: "an scope with linked person and a part"
        def personId = parseJson(post("/persons", [
            name: "Lou Vice",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def memberId = parseJson(post("/scopes", [
            name: "Data Party Inc.",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Company",
                    status: "NEW"
                ]
            ]
        ])).resourceId
        def scopeId = parseJson(post("/scopes", [
            name: "Data Inc.",
            abbreviation: "DT",
            description: "Some company dealing with IT",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Company",
                    status: "NEW",
                    riskDefinition: "riskyDef"
                ]
            ],
            customAspects: [
                staff: [
                    attributes: [
                        numberOfEmployees: 638
                    ]
                ]
            ],
            members: [
                [ targetUri:"/scopes/$memberId" ]
            ],
            links: [
                dataProtectionOfficer: [
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
        def response = parseJson(get("/domians/$testDomainId/scopes/$scopeId"))

        then: "basic properties are contained"
        response.id == scopeId
        response.type == "scope"
        response._self == "http://localhost/domians/$testDomainId/scopes/$scopeId"
        response.name == "Data Inc."
        response.abbreviation == "DT"
        response.description == "Some company dealing with IT"
        response.designator =~ /SCP-\d+/
        response.owner.targetUri == "http://localhost/units/$unitId"
        response.createdBy == "user@domain.example"
        response.createdAt != null
        response.updatedBy == "user@domain.example"
        response.updatedAt == response.createdAt

        and: "domain-specific properties"
        response.subType == "Company"
        response.status == "NEW"
        response.customAspects.staff.numberOfEmployees == 638
        response.links.dataProtectionOfficer[0].target.targetUri == "http://localhost/persons/$personId"
        response.links.dataProtectionOfficer[0].target.targetInDomainUri == "http://localhost/domians/$testDomainId/persons/$personId"
        response.links.dataProtectionOfficer[0].target.associatedWithDomain == false
        response.links.dataProtectionOfficer[0].target.subType == null
        response.links.dataProtectionOfficer[0].attributes.experienceSince == "1988-08-08"
        response.riskDefinition == "riskyDef"

        and: "members"
        response.members[0].targetUri == "http://localhost/scopes/$memberId"
        response.members[0].targetInDomainUri == "http://localhost/domians/$testDomainId/scopes/$memberId"
        response.members[0].associatedWithDomain
        response.members[0].subType == "Company"
    }

    def "missing scope is handled"() {
        given: "a non-existing scope ID"
        def randomScopeId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domians/$testDomainId/scopes/$randomScopeId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Scope with ID $randomScopeId not found"
    }

    def "missing domain is handled"() {
        given: "a scope in a domain"
        def scopeId = parseJson(post("/scopes", [
            name: "Some scope",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Company",
                    status: "NEW"
                ]
            ]
        ])).resourceId
        def randomDomainId = randomUUID()

        when: "trying to fetch the scope in a non-existing domain"
        get("/domians/$randomDomainId/scopes/$scopeId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Domain with ID $randomDomainId not found"
    }

    def "unassociated scope is handled"() {
        given: "a scope without any domains"
        def scopeId = parseJson(post("/scopes", [
            name: "Unassociated scope",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId

        when:
        get("/domians/$testDomainId/scopes/$scopeId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Scope $scopeId is not associated with domain $testDomainId"
    }
}
