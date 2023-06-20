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
        client = clientDataRepository.findById(client.idAsString).get()
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "CRUD scope in domain contexts"() {
        given: "an scope with linked person and a part"
        def personId = parseJson(post("/persons", [
            name: "Lou Vice",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def memberId = parseJson(post("/domains/$testDomainId/scopes", [
            name: "Data Party Inc.",
            owner: [targetUri: "/units/$unitId"],
            subType: "Company",
            status: "NEW"
        ])).resourceId
        def scopeId = parseJson(post("/domains/$testDomainId/scopes", [
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
        def response = parseJson(get("/domains/$testDomainId/scopes/$scopeId"))

        then: "basic properties are contained"
        response.id == scopeId
        response.type == "scope"
        response._self == "http://localhost/domains/$testDomainId/scopes/$scopeId"
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
        response.links.dataProtectionOfficer[0].target.targetInDomainUri == "http://localhost/domains/$testDomainId/persons/$personId"
        response.links.dataProtectionOfficer[0].target.associatedWithDomain == false
        response.links.dataProtectionOfficer[0].target.subType == null
        response.links.dataProtectionOfficer[0].attributes.experienceSince == "1988-08-08"
        response.riskDefinition == "riskyDef"

        and: "members"
        response.members[0].targetUri == "http://localhost/scopes/$memberId"
        response.members[0].targetInDomainUri == "http://localhost/domains/$testDomainId/scopes/$memberId"
        response.members[0].associatedWithDomain
        response.members[0].subType == "Company"

        when: "associating scope with a second domain"
        post("/domains/$dsgvoTestDomainId/scopes/$scopeId", [
            subType: "SCP_Processor",
            status: "IN_PROGRESS"
        ], 200)

        and: "fetching scope in second domain"
        def scopeInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/scopes/$scopeId")) as Map

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
        put("/domains/$dsgvoTestDomainId/scopes/$scopeId", scopeInDsgvo, [
            'If-Match': getETag(get("/domains/$dsgvoTestDomainId/scopes/$scopeId"))
        ], 200)
        scopeInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/scopes/$scopeId"))

        then: "updated values are present"
        scopeInDsgvo.description == "New description"
        scopeInDsgvo.status == "ARCHIVED"
        scopeInDsgvo.customAspects.scope_thirdCountry.scope_thirdCountry_country == "Pizzaland"

        and: "values for original domain are still absent"
        scopeInDsgvo.customAspects.staff == null

        when: "fetching the scope from the viewpoint of the original domain again"
        def scopeInTestdomain = parseJson(get("/domains/$testDomainId/scopes/$scopeId"))

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
        with(parseJson(get("/domains/$testDomainId/scopes?size=10&sortBy=designator"))) {
            totalItemCount == 15
            page == 0
            pageCount == 2
            items*.name == (1..10).collect { "scope $it" }
            items*.subType =~ ["Company"]
        }

        and: "page 2 to be available"
        with(parseJson(get("/domains/$testDomainId/scopes?size=10&page=1&sortBy=designator"))) {
            totalItemCount == 15
            page == 1
            pageCount == 2
            items*.name == (11..15).collect { "scope $it" }
            items*.subType =~ ["Company"]
        }
    }

    def "member list can be updated"() {
        given: "a scope with an asset as a member"
        def assetId = parseJson(post("/domains/$testDomainId/assets", [
            name: "Member one",
            owner: [targetUri: "/units/$unitId"],
            subType: "Server",
            status: "RUNNING",
        ])).resourceId
        def scopeId = parseJson(post("/domains/$testDomainId/scopes", [
            name: "Data Inc.",
            owner: [targetUri: "/units/$unitId"],
            subType: "Company",
            status: "NEW",
            members: [
                [targetUri: "/assets/$assetId"]
            ]
        ])).resourceId

        when: "adding a person as a second member"
        def personId = parseJson(post("/domains/$testDomainId/persons", [
            name: "Member two",
            owner: [targetUri: "/units/$unitId"],
            subType: "Programmer",
            status: "CODING",
        ])).resourceId
        get("/domains/$testDomainId/scopes/$scopeId").with {
            def scope = parseJson(it)
            scope.members.add([targetUri: "/persons/$personId"])
            put(scope._self, scope, ['If-Match': getETag(it)])
        }

        then: "both members are present"
        with(parseJson(get("/domains/$testDomainId/scopes/$scopeId"))) {
            members*.targetUri =~ [
                "http://localhost/assets/$assetId",
                "http://localhost/persons/$personId",
            ]
        }

        when: "removing the asset from the scope"
        get("/domains/$testDomainId/scopes/$scopeId").with {
            def scope = parseJson(it)
            scope.members.removeIf { it.targetUri.contains("asset") }
            put(scope._self, scope, ['If-Match': getETag(it)])
        }

        then: "only the person remains"
        with(parseJson(get("/domains/$testDomainId/scopes/$scopeId"))) {
            members*.targetUri =~ [
                "http://localhost/persons/$personId",
            ]
        }
    }

    def "members can be paginated"() {
        given: "three elements in test-domain"
        def assetUri = post("/domains/$testDomainId/assets", [
            name: "A for Asset",
            owner: [targetUri: "/units/$unitId"],
            subType: "Server",
            status: "DOWN"
        ]).andReturn().response.getHeader("Location")
        def documentUri = post("/domains/$testDomainId/documents", [
            name: "D for Document",
            owner: [targetUri: "/units/$unitId"],
            subType: "Manual",
            status: "OUTDATED"
        ]).andReturn().response.getHeader("Location")
        def subScopeUri = post("/domains/$testDomainId/scopes", [
            name: "S for Scope",
            owner: [targetUri: "/units/$unitId"],
            subType: "Company",
            status: "NEW"
        ]).andReturn().response.getHeader("Location")

        and: "one element in DS-GVO"
        def dsgvoSubScopeUri = post("/domains/$dsgvoTestDomainId/scopes", [
            name: "A for Absent",
            owner: [targetUri: "/units/$unitId"],
            subType: "SCP_Processor",
            status: "NEW"
        ]).andReturn().response.getHeader("Location")

        and: "a scope containing those elements"
        def scopeUri = post("/domains/$testDomainId/scopes", [
            name: "scope of hope",
            owner: [targetUri: "/units/$unitId"],
            subType: "Company",
            status: "NEW",
            members: [
                [targetInDomainUri: assetUri],
                [targetInDomainUri: documentUri],
                [targetInDomainUri: subScopeUri],
                [targetInDomainUri: dsgvoSubScopeUri],
            ]
        ]).andReturn().response.getHeader("Location")

        expect: "scope members in test-domain to be retrieved"
        with(parseJson(get("$scopeUri/members?size=2&page=0"))) {
            totalItemCount == 3
            page == 0
            pageCount == 2
            items.size() == 2
            with(items[0]) {
                name == "A for Asset"
                type == "asset"
                subType == "Server"
            }
            with(items[1]) {
                name == "D for Document"
                type == "document"
                subType == "Manual"
            }
        }
        with(parseJson(get("$scopeUri/members?size=2&page=1"))) {
            totalItemCount == 3
            page == 1
            pageCount == 2
            items.size() == 1
            with(items[0]) {
                name == "S for Scope"
                type == "scope"
                subType == "Company"
            }
        }
    }

    def "missing scope is handled"() {
        given: "a non-existing scope ID"
        def randomScopeId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domains/$testDomainId/scopes/$randomScopeId", 404)

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
        get("/domains/$randomDomainId/scopes/$scopeId", 404)

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
        get("/domains/$testDomainId/scopes/$scopeId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Scope $scopeId is not associated with domain $testDomainId"
    }

    def "risk values can be updated"() {
        given: "a scope with risk values"
        def assetId = parseJson(post("/domains/$testDomainId/scopes", [
            name: "Risky asset",
            owner: [targetUri: "/units/$unitId"],
            subType: "Company",
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
        get("/domains/$testDomainId/scopes/$assetId").with{getResults ->
            def asset = parseJson(getResults)
            asset.riskValues.riskyDef.potentialImpacts.C = 1
            put(asset._self, asset, ["If-Match": getETag(getResults)], 200)
        }

        then: "risk values have been altered"
        with(parseJson(get("/domains/$testDomainId/scopes/$assetId"))) {
            riskValues.riskyDef.potentialImpacts.C == 1
        }
    }
}
