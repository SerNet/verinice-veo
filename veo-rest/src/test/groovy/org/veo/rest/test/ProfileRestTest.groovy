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

    def "apply simple profile"() {
        given: "dsgvo with a profile"
        def profiles = get("/domains/${dsgvoDomainId}/profiles").body
        def profileId = profiles.find { it.name == "Beispielorganisation" }.id

        post("/domains/$dsgvoDomainId/profiles/$profileId/incarnation?unit=$unitId", null, 204)

        expect:
        with(get("/domains/$dsgvoDomainId/element-status-count?unit=$unitId").body) {
            person.PER_Person.NEW == 5
            control.CTL_TOM.NEW == 1
            scenario.SCN_Scenario.NEW == 1
            scope.SCP_Scope.IN_PROGRESS == 1
            process.PRO_DataProcessing.NEW == 1
        }

        and: "elements part are correct created"
        with(get("/domains/$dsgvoDomainId/persons?unit=$unitId").body) {
            items.size() == 5
            with (items.find { it.name == "IT-Team" }) {
                links.size() == 0
                parts.size() == 1
                parts[0].name == "Hans Meiser"
            }
            with(items.find { it.name == "Personal" }) {
                links.size() == 0
                parts.size() == 3
                parts.name ==~ [
                    "Hans Meiser",
                    "Jürgen Toast",
                    "Harald Wald"
                ]
            }
            with(items.find { it.name == "Hans Meiser" }) {
                links.size() == 0
                parts.size() == 1
                parts[0].name == "Harald Wald"
            }
            with(items.find { it.name == "Jürgen Toast" }) {
                links.size() == 1
                links.person_favoriteScope.target.name == ["Data GmbH"]
                parts.size() == 0
            }
            with(items.find { it.name == "Harald Wald" }) {
                links.size() == 0
                parts.size() == 0
            }
        }

        and: "scope members are correct created also links"
        with(get("/domains/$dsgvoDomainId/scopes?unit=$unitId").body) {
            items.size() == 1
            with (items.find { it.name == "Data GmbH" }) {
                members.size() == 1
                members[0].name == "Durchführung Befragungen"
                links.size() == 2
                links.scope_informationSecurityOfficer.target.name == ["Jürgen Toast"]
                links.scope_headOfDataProcessing.target.name == ["Hans Meiser"]
            }
        }

        and: "process members are correct created also links"
        with(get("/domains/$dsgvoDomainId/processes?unit=$unitId").body) {
            items.size() == 1
            with(items.find { it.name == "Durchführung Befragungen" }) {
                links.size() == 2
                links.process_PIAProcessOwner.target.name == ["Hans Meiser"]
                links.process_PIAOOtherOrganisationsInvolved.target.name == ["Data GmbH"]
            }
        }

        and:
        with(get("/domains/$dsgvoDomainId/controls?unit=$unitId").body) {
            items.size() == 1
            with(items.find { it.name == "TOM 1" }) {
                links.size() == 0
                parts.size() == 0
                riskValues.DSRA.implementationStatus == 2
            }
        }
        with(get("/domains/$dsgvoDomainId/scenarios?unit=$unitId").body) {
            items.size() == 1
            with(items.find { it.name == "GefÃ¤hrdung 1" }) {
                links.size() == 0
                parts.size() == 0
                riskValues.DSRA.potentialProbability == 1
            }
        }
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
        post("/content-creation/domains/$newDomainId/profiles?unit=$unitId", [
            name: "To serve man",
            description: "It's a cookbook!",
            language: "en_us",
        ])
        def templateUri = post("/content-creation/domains/$newDomainId/template", [
            version: "1.0.0",
        ], 201, CONTENT_CREATOR).body.targetUri
        post("/domain-templates/"+uriToId(templateUri)+"/createdomains", null, 204, ADMIN)

        and: "applying the profile in secondary client"
        def secondaryClientDomainId = get("/domains", 200, SECONDARY_CLIENT_USER).body.find {
            it.name == newDomainName
        }.id
        def profileId = get("/domains/$secondaryClientDomainId/profiles", 200, SECONDARY_CLIENT_USER).body[0].id
        def targetUnitId = post("/units", [
            name: "profile target unit",
            domains: [
                [targetUri: "/domains/$secondaryClientDomainId"]
            ],
        ], 201, SECONDARY_CLIENT_USER).body.resourceId
        post("/domains/$secondaryClientDomainId/profiles/$profileId/incarnation?unit=$targetUnitId", null, 204, SECONDARY_CLIENT_USER)
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
        given:
        def randomId = randomUUID().toString()

        expect:
        post("/domains/$testDomainId/profiles/$randomId/incarnation?unit=$unitId", null, 404)
                .body.message == "profile $randomId not found"
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
}
