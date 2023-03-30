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
import org.veo.core.repository.ScopeRepository
import org.veo.core.repository.UnitRepository

@WithUserDetails("user@domain.example")
class ScopeInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository
    @Autowired
    private ScopeRepository scopeRepository

    private String unitId
    private String testDomainId
    private String dsgvoTestDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        dsgvoTestDomainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "CRUD scope in domain contexts"() {
        given: "an scope with linked person and a part"
        def personId = parseJson(post("/persons", [
            name: "Lou Vice",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def memberId = parseJson(post("/domians/$testDomainId/scopes", [
            name: "Data Party Inc.",
            owner: [targetUri: "/units/$unitId"],
            subType: "Company",
            status: "NEW"
        ])).resourceId
        def scopeId = parseJson(post("/domians/$testDomainId/scopes", [
            name: "Data Inc.",
            abbreviation: "DT",
            description: "Some company dealing with IT",
            owner: [targetUri: "/units/$unitId"],
            subType: "Company",
            status: "NEW",
            riskDefinition: "riskyDef",
            customAspects: [
                staff: [
                    numberOfEmployees: 638
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

        when: "associating scope with a second domain"
        post("/domians/$dsgvoTestDomainId/scopes/$scopeId", [
            subType: "SCP_Processor",
            status: "IN_PROGRESS"
        ], 200)

        and: "fetching scope in second domain"
        def scopeInDsgvo = parseJson(get("/domians/$dsgvoTestDomainId/scopes/$scopeId")) as Map

        then: "it contains basic values"
        scopeInDsgvo.name == "Data Inc."
        scopeInDsgvo.description == "Some company dealing with IT"

        and: "values for second domain"
        scopeInDsgvo.subType == "SCP_Processor"
        scopeInDsgvo.status == "IN_PROGRESS"

        and: "no values for original domain"
        scopeInDsgvo.customAspects.staff == null

        when: "updating and reloading the scope from the viewpoint of the second domain"
        scopeInDsgvo.description = "New description"
        scopeInDsgvo.status = "ARCHIVED"
        scopeInDsgvo.customAspects.scope_thirdCountry = [
            scope_thirdCountry_country: "Pizzaland"
        ]
        put("/domians/$dsgvoTestDomainId/scopes/$scopeId", scopeInDsgvo, [
            'If-Match': getETag(get("/domians/$dsgvoTestDomainId/scopes/$scopeId"))
        ], 200)
        scopeInDsgvo = parseJson(get("/domians/$dsgvoTestDomainId/scopes/$scopeId"))

        then: "updated values are present"
        scopeInDsgvo.description == "New description"
        scopeInDsgvo.status == "ARCHIVED"
        scopeInDsgvo.customAspects.scope_thirdCountry.scope_thirdCountry_country == "Pizzaland"

        and: "values for original domain are still absent"
        scopeInDsgvo.customAspects.staff == null

        when: "fetching the scope from the viewpoint of the original domain again"
        def scopeInTestdomain = parseJson(get("/domians/$testDomainId/scopes/$scopeId"))

        then: "values for original domain are unchanged"
        scopeInTestdomain.subType == "Company"
        scopeInTestdomain.status == "NEW"
        scopeInTestdomain.customAspects.staff.numberOfEmployees == 638

        and: "some basic values have been updated"
        scopeInTestdomain.name == "Data Inc."
        scopeInTestdomain.description == "New description"

        and: "values for the second domain are absent"
        scopeInTestdomain.customAspects.scope_thirdCountry == null
    }

    def "get all scopes in a domain"() {
        given: "15 scopes in the domain & one unassociated scope"
        (1..15).forEach {
            post("/scopes", [
                name: "scope $it",
                owner: [targetUri: "/units/$unitId"],
                domains: [
                    (testDomainId): [
                        subType: "Company",
                        status: "NEW",
                    ]
                ]
            ])
        }
        post("/scopes", [
            name: "unassociated scope",
            owner: [targetUri: "/units/$unitId"]
        ])

        expect: "page 1 to be available"
        with(parseJson(get("/domians/$testDomainId/scopes?size=10&sortBy=designator"))) {
            totalItemCount == 15
            page == 0
            pageCount == 2
            items*.name == (1..10).collect { "scope $it" }
            items*.subType =~ ["Company"]
        }

        and: "page 2 to be available"
        with(parseJson(get("/domians/$testDomainId/scopes?size=10&page=1&sortBy=designator"))) {
            totalItemCount == 15
            page == 1
            pageCount == 2
            items*.name == (11..15).collect { "scope $it" }
            items*.subType =~ ["Company"]
        }
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
