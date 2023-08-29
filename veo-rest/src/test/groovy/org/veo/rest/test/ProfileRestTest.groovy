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
package org.veo.rest.test

import static java.util.UUID.randomUUID
import static org.veo.rest.test.UserType.ADMIN
import static org.veo.rest.test.UserType.CONTENT_CREATOR
import static org.veo.rest.test.UserType.SECONDARY_CLIENT_USER

class ProfileRestTest extends VeoRestTest {
    String newDomainName
    String newDomainId
    String unitId

    def setup() {
        newDomainName = "profile creation test ${randomUUID()}"
        newDomainId = post("/content-creation/domains", [
            name: newDomainName,
            abbreviation: "pct",
            description: "...",
            authority: "jj",
        ], 201, CONTENT_CREATOR).body.resourceId
        putElementTypeDefinitions(newDomainId)
        unitId = postNewUnit().resourceId
    }

    def "create profile from multi-domain unit"() {
        given: "assets associated with different domains"
        get("/units/$unitId").with {
            body.domains = [
                [targetUri: "/domains/$newDomainId"],
                [targetUri: "/domains/$testDomainId"],
            ]
            put(body._self, body, getETag())
        }
        def unassociatedScenarioId = post("/scenarios", [
            name: "unassociated scenario",
            owner: [targetUri: "/units/$unitId"],
        ]).body.resourceId
        def newDomainScenarioId = post("/domains/$newDomainId/scenarios", [
            name: "new domain scenario",
            owner: [targetUri: "/units/$unitId"],
            subType: "danger",
            status: "real",
        ]).body.resourceId
        def testDomainAssetId = post("/domains/$testDomainId/assets", [
            name: "test domain asset",
            subType: "Server",
            status: "DOWN",
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId
        def newDomainAssetId = post("/domains/$newDomainId/assets", [
            name: "new domain asset",
            subType: "server",
            status: "off",
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId
        def multiDomainAssetId = post("/domains/$newDomainId/assets", [
            name: "multi domain asset",
            subType: "server",
            status: "on",
            owner: [targetUri: "/units/$unitId"],
            customAspects: [
                energy: [
                    powerConsumptionWatt: 9000
                ]
            ],
            parts: [
                [targetUri: "/assets/$newDomainAssetId"],
                [targetUri: "/assets/$testDomainAssetId"],
            ]
        ]).body.resourceId
        "/domains/$testDomainId/assets/$multiDomainAssetId".with { uri ->
            post(uri, [
                subType: "Server",
                status: "RUNNING"
            ], 200)
            get(uri).with {
                body.customAspects.storage = [
                    totalCapacityInTb: 64
                ]
                put(uri, body, getETag())
            }
        }
        post("/assets/$multiDomainAssetId/risks", [
            scenario: [targetUri: "/scenarios/$unassociatedScenarioId"],
            domains: [
                (newDomainId): [reference: [targetUri: "/domains/$newDomainId"]],
                (testDomainId): [reference: [targetUri: "/domains/$testDomainId"]],
            ]
        ])
        post("/assets/$multiDomainAssetId/risks", [
            scenario: [targetUri: "/scenarios/$newDomainScenarioId"],
            domains: [
                (newDomainId): [reference: [targetUri: "/domains/$newDomainId"]],
                (testDomainId): [reference: [targetUri: "/domains/$testDomainId"]],
            ]
        ])

        when: "creating a domain template with a profile based on the unit"
        def templateUri = post("/content-creation/domains/$newDomainId/template", [
            version: "1.0.0",
            profiles: [
                servers: [
                    unitId: unitId
                ]
            ]
        ], 201, CONTENT_CREATOR).body.targetUri
        def templateId = uriToId(templateUri)
        println(templateId)
        post("/domaintemplates/$templateId/createdomains", null, 204, ADMIN)

        and: "applying the profile in secondary client"
        def secondaryClientDomainId = get("/domains", 200, SECONDARY_CLIENT_USER).body.find {
            it.name == newDomainName
        }.id
        def targetUnitId = post("/units", [
            name: "profile target unit"
        ], 201, SECONDARY_CLIENT_USER).body.resourceId
        post("/domains/$secondaryClientDomainId/profiles/servers/units/$targetUnitId", null, 204, SECONDARY_CLIENT_USER)
        def appliedAssets = get("/assets?unit=$targetUnitId", 200,
                SECONDARY_CLIENT_USER).body.items

        then: "only content for the correct domain has been applied"
        appliedAssets.size() == 2
        with(appliedAssets.find { it.name == "new domain asset" }) {
            domains.keySet() ==~ [secondaryClientDomainId]
            with(get("/domains/$secondaryClientDomainId/assets/$id", 200, SECONDARY_CLIENT_USER).body) {
                subType == "server"
                status == "off"
            }
        }
        with(appliedAssets.find { it.name == "multi domain asset" }) {
            domains.keySet() ==~ [secondaryClientDomainId]
            parts.size() == 1
            parts.first().displayName =~ /.+new domain asset/
            with(get("/domains/$secondaryClientDomainId/assets/$id", 200, SECONDARY_CLIENT_USER).body) {
                subType == "server"
                status == "on"
                customAspects.energy.powerConsumptionWatt == 9000
                customAspects.storage == null
            }
            with(get("/assets/$id/risks", 200, SECONDARY_CLIENT_USER).body) {
                size() == 1
                first().scenario.displayName =~ /.+ new domain scenario/
            }
        }
        with(get("/scenarios?unit=$targetUnitId", 200, SECONDARY_CLIENT_USER).body.items) {
            size() == 1
            first().name == "new domain scenario"
        }

        and: "the original element is still assigned to both domains"
        get("/assets/$multiDomainAssetId").body.domains.keySet() ==~ [newDomainId, testDomainId]
    }

    def "applying absent profile yields error"() {
        expect:
        post("/domains/$testDomainId/profiles/absent/units/$unitId", null, 404)
                .body.message == "Profile 'absent' not found"
    }

    def putElementTypeDefinitions(String domainId) {
        put("/content-creation/domains/$domainId/element-type-definitions/asset",
                [
                    subTypes: [
                        server: [
                            statuses: ["off", "on"]
                        ]
                    ],
                    customAspects: [
                        energy: [
                            attributeDefinitions: [
                                powerConsumptionWatt: [
                                    type: "integer"
                                ]
                            ]
                        ]
                    ],
                ], null, 204, CONTENT_CREATOR)
        put("/content-creation/domains/$domainId/element-type-definitions/scenario",
                [
                    subTypes: [
                        danger: [
                            statuses: ["hypothetical", "real"]
                        ]
                    ],
                ], null, 204, CONTENT_CREATOR)
    }

    private uriToId(String targetUri) {
        targetUri.split('/').last()
    }
}
