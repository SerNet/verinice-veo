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
import org.veo.core.repository.AssetRepository
import org.veo.core.repository.UnitRepository

@WithUserDetails("user@domain.example")
class AssetInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository
    @Autowired
    private AssetRepository assetRepository

    private String unitId
    private String testDomainId
    private String dsgvoTestDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        dsgvoTestDomainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
        client = clientRepository.getById(client.id)
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "CRUD asset in domain contexts"() {
        given: "an asset with linked person and a part"
        def personId = parseJson(post("/domains/$testDomainId/persons", [
            name: "Anne Admin",
            owner: [targetUri: "/units/$unitId"],
            subType: "MasterOfDisaster",
            status: "WATCHING_DISASTER_MOVIES",
        ])).resourceId
        def partId = parseJson(post("/domains/$testDomainId/assets", [
            name: "Git server",
            owner: [targetUri: "/units/$unitId"],
            subType: "Server",
            status: "DOWN"
        ])).resourceId
        def assetId = parseJson(post("/domains/$testDomainId/assets", [
            name: "My little server farm",
            abbreviation: "SF",
            description: "Bunch of servers",
            owner: [targetUri: "/units/$unitId"],
            subType: "Server",
            status: "RUNNING",
            customAspects: [
                storage: [
                    totalCapacityInTb: 32
                ]
            ],
            parts: [
                [ targetUri:"/assets/$partId" ]
            ],
            links: [
                admin: [
                    [
                        target: [targetUri: "/persons/$personId"],
                        attributes: [
                            accessProtocol: "ssh"
                        ]
                    ]
                ]
            ]
        ])).resourceId

        when: "fetching it in the domain context"
        def response = parseJson(get("/domains/$testDomainId/assets/$assetId"))

        then: "basic properties are contained"
        response.id == assetId
        response.type == "asset"
        response._self == "http://localhost/domains/$testDomainId/assets/$assetId"
        response.name == "My little server farm"
        response.abbreviation == "SF"
        response.description == "Bunch of servers"
        response.designator =~ /AST-\d+/
        response.owner.targetUri == "http://localhost/units/$unitId"
        response.createdBy == "user@domain.example"
        response.createdAt != null
        response.updatedBy == "user@domain.example"
        response.updatedAt == response.createdAt

        and: "domain-specific properties"
        response.subType == "Server"
        response.status == "RUNNING"
        response.customAspects.storage.totalCapacityInTb == 32
        response.links.admin[0].target.targetUri == "http://localhost/persons/$personId"
        response.links.admin[0].target.targetInDomainUri == "http://localhost/domains/$testDomainId/persons/$personId"
        response.links.admin[0].target.associatedWithDomain
        response.links.admin[0].target.subType == "MasterOfDisaster"
        response.links.admin[0].attributes.accessProtocol == "ssh"

        and: "parts"
        response.parts[0].targetUri == "http://localhost/assets/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domains/$testDomainId/assets/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "Server"

        and: "it conforms to the JSON schema"
        validate(response, get("/domains/$testDomainId/assets/json-schema")).empty

        when: "associating asset with a second domain"
        post("/domains/$dsgvoTestDomainId/assets/$assetId", [
            subType: "AST_IT-System",
            status: "RELEASED"
        ], 200)

        and: "fetching asset in second domain"
        def assetInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/assets/$assetId")) as Map

        then: "it contains basic values"
        assetInDsgvo.name == "My little server farm"
        assetInDsgvo.description == "Bunch of servers"

        and: "values for second domain"
        assetInDsgvo.subType == "AST_IT-System"
        assetInDsgvo.status == "RELEASED"

        and: "no values for original domain"
        assetInDsgvo.customAspects.storage == null

        when: "updating and reloading the asset from the viewpoint of the second domain"
        assetInDsgvo.description = "New description"
        assetInDsgvo.status = "ARCHIVED"
        assetInDsgvo.customAspects.asset_details = [
            asset_details_number: 3000
        ]
        put("/domains/$dsgvoTestDomainId/assets/$assetId", assetInDsgvo, [
            'If-Match': getETag(get("/domains/$dsgvoTestDomainId/assets/$assetId"))
        ], 200)
        assetInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/assets/$assetId"))

        then: "updated values are present"
        assetInDsgvo.description == "New description"
        assetInDsgvo.status == "ARCHIVED"
        assetInDsgvo.customAspects.asset_details.asset_details_number == 3000

        and: "values for original domain are still absent"
        assetInDsgvo.customAspects.storage == null

        when: "fetching the asset from the viewpoint of the original domain again"
        def assetInTestdomain = parseJson(get("/domains/$testDomainId/assets/$assetId"))

        then: "values for original domain are unchanged"
        assetInTestdomain.subType == "Server"
        assetInTestdomain.status == "RUNNING"
        assetInTestdomain.customAspects.storage.totalCapacityInTb == 32

        and: "some basic values have been updated"
        assetInTestdomain.name == "My little server farm"
        assetInTestdomain.description == "New description"

        and: "values for the second domain are absent"
        assetInTestdomain.customAspects.asset_details == null
    }

    def "get all assets in a domain"() {
        given: "15 assets in the domain & one unassociated asset"
        (1..15).forEach {
            post("/domains/$testDomainId/assets", [
                name: "asset $it",
                owner: [targetUri: "/units/$unitId"],
                subType: "Server",
                status: "RUNNING",
            ])
        }
        post("/assets", [
            name: "unassociated asset",
            owner: [targetUri: "/units/$unitId"]
        ])

        expect: "page 1 to be available"
        with(parseJson(get("/domains/$testDomainId/assets?size=10&sortBy=designator"))) {
            totalItemCount == 15
            page == 0
            pageCount == 2
            items*.name == (1..10).collect { "asset $it" }
            items*.subType =~ ["Server"]
        }

        and: "page 2 to be available"
        with(parseJson(get("/domains/$testDomainId/assets?size=10&page=1&sortBy=designator"))) {
            totalItemCount == 15
            page == 1
            pageCount == 2
            items*.name == (11..15).collect { "asset $it" }
            items*.subType =~ ["Server"]
        }
    }

    def "missing asset is handled"() {
        given: "a non-existing asset ID"
        def randomAssetId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domains/$testDomainId/assets/$randomAssetId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "asset $randomAssetId not found"
    }

    def "missing domain is handled"() {
        given: "an asset in a domain"
        def assetId = parseJson(post("/domains/$testDomainId/assets", [
            name: "Some asset",
            owner: [targetUri: "/units/$unitId"],
            subType: "Server",
            status: "DOWN"
        ])).resourceId
        def randomDomainId = randomUUID()

        when: "trying to fetch the asset in a non-existing domain"
        get("/domains/$randomDomainId/assets/$assetId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "domain $randomDomainId not found"
    }

    def "unassociated asset is handled"() {
        given: "an asset without any domains"
        def assetId = parseJson(post("/assets", [
            name: "Unassociated asset",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId

        when:
        get("/domains/$testDomainId/assets/$assetId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Asset $assetId is not associated with domain $testDomainId"
    }

    def "retrieving parts for missing asset returns 404"() {
        given: "a non-existing asset ID"
        def randomAssetId = randomUUID()

        when: "trying to fetch its parts in the domain"
        get("/domains/$testDomainId/assets/$randomAssetId/parts", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "asset $randomAssetId not found"
    }

    def "risk values can be updated"() {
        given: "an asset with risk values"
        def assetId = parseJson(post("/domains/$testDomainId/assets", [
            name: "Risky asset",
            owner: [targetUri: "/units/$unitId"],
            subType: "Server",
            status: "RUNNING",
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        C: 0
                    ]
                ]
            ]
        ])).resourceId

        when: "updating risk values"
        get("/domains/$testDomainId/assets/$assetId").with{getResults ->
            def asset = parseJson(getResults)
            asset.riskValues.riskyDef.potentialImpacts.C = 1
            put(asset._self, asset, ["If-Match": getETag(getResults)], 200)
        }

        then: "risk values have been altered"
        with(parseJson(get("/domains/$testDomainId/assets/$assetId"))) {
            riskValues.riskyDef.potentialImpacts.C == 1
        }
    }
}
