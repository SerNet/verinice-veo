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
class AssetInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository

    private String unitId
    private String testDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "get asset in a domain"() {
        given: "an asset with linked person and a part"
        def personId = parseJson(post("/persons", [
            name: "Anne Admin",
            owner: [targetUri: "/units/$unitId"],
        ])).resourceId
        def partId = parseJson(post("/assets", [
            name: "Git server",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Server",
                    status: "DOWN"
                ]
            ]
        ])).resourceId
        def assetId = parseJson(post("/assets", [
            name: "My little server farm",
            abbreviation: "SF",
            description: "Bunch of servers",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Server",
                    status: "RUNNING"
                ]
            ],
            customAspects: [
                storage: [
                    attributes: [
                        totalCapacityInTb: 32
                    ]
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
        def response = parseJson(get("/domians/$testDomainId/assets/$assetId"))

        then: "basic properties are contained"
        response.id == assetId
        response.type == "asset"
        response._self == "http://localhost/domians/$testDomainId/assets/$assetId"
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
        response.links.admin[0].target.targetInDomainUri == "http://localhost/domians/$testDomainId/persons/$personId"
        response.links.admin[0].target.associatedWithDomain == false
        response.links.admin[0].target.subType == null
        response.links.admin[0].attributes.accessProtocol == "ssh"

        and: "parts"
        response.parts[0].targetUri == "http://localhost/assets/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domians/$testDomainId/assets/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "Server"
    }

    def "missing asset is handled"() {
        given: "a non-existing asset ID"
        def randomAssetId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domians/$testDomainId/assets/$randomAssetId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Asset with ID $randomAssetId not found"
    }

    def "missing domain is handled"() {
        given: "an asset in a domain"
        def assetId = parseJson(post("/assets", [
            name: "Some asset",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "Server",
                    status: "DOWN"
                ]
            ]
        ])).resourceId
        def randomDomainId = randomUUID()

        when: "trying to fetch the asset in a non-existing domain"
        get("/domians/$randomDomainId/assets/$assetId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Domain with ID $randomDomainId not found"
    }

    def "unassociated asset is handled"() {
        given: "an asset without any domains"
        def assetId = parseJson(post("/assets", [
            name: "Unassociated asset",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId

        when:
        get("/domians/$testDomainId/assets/$assetId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "Asset $assetId is not associated with domain $testDomainId"
    }
}
