/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

import org.veo.core.entity.ControlImplementationConfiguration
import org.veo.core.entity.TranslationMap
import org.veo.core.entity.riskdefinition.CategoryLevel

class DomainCreationRestTest extends DomainRestTest {
    String unitId

    def setup() {
        unitId = postNewUnit().resourceId
    }

    def "create new domain"() {
        when: "creating a new domain"
        def domainName = "Domain creation test ${randomUUID()}"
        def domainId = post("/content-creation/domains", [
            name: domainName,
            abbreviation: "dct",
            description: "best one ever",
            authority: "JJ",
        ], 201, CONTENT_CREATOR).body.resourceId

        then: "it can be retrieved"
        with(get("/domains/$domainId").body) {
            name == domainName
            abbreviation == "dct"
            description == "best one ever"
            authority == "JJ"
            templateVersion == "0.1.0"
            domainTemplate == null
            controlImplementationConfiguration == [:]
        }

        when: "defining an element type, decision, inspection & incarnation config in the domain"
        postAssetObjectSchema(domainId)
        def domainUpdatedBeforeDecisionUpdate = get("/domains/$domainId").body.updatedAt

        put("/content-creation/domains/${domainId}/decisions/truthy", [
            name: [en: "Decision that always outputs true"],
            elementType: "asset",
            elementSubType: "server",
            defaultResultValue: true,
        ], null, 201, CONTENT_CREATOR)
        def domainUpdatedAfterDecisionUpdate = get("/domains/$domainId").body.updatedAt

        then:
        domainUpdatedAfterDecisionUpdate > domainUpdatedBeforeDecisionUpdate

        when:
        put("/content-creation/domains/${domainId}/inspections/alwaysAddPart", [
            description: [en: "You should always add a part, no matter what"],
            severity: "HINT",
            elementType: "asset",
            elementSubType: "server",
            condition: [
                type: "constant",
                value: true
            ],
            suggestions: [
                [type: "addPart"]
            ]
        ], null, 201, CONTENT_CREATOR)

        then:
        get("/domains/$domainId").body.updatedAt > domainUpdatedAfterDecisionUpdate

        when:
        get("/domains/$domainId/incarnation-configuration").with {
            body.exclude = ["COMPOSITE"]
            put("/content-creation/domains/$domainId/incarnation-configuration", body, getETag(), 204, CONTENT_CREATOR)
        }

        and: "adding the domain to the unit"
        get("/units/$unitId").with{
            body.domains.add([targetUri: "/domains/$domainId"])
            put("/units/$unitId", body, getETag())
        }

        then: "an element can be created in the domain"
        post("/assets", [
            name: "main server",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "server",
                    status: "on"
                ]
            ]
        ])

        when: "creating a template from the domain"
        def templateUri = post("/content-creation/domains/$domainId/template", [
            version: "1.0.0"
        ]).body.targetUri.replaceAll( /\/content-creation\//, "/")

        and: "applying the template in another client"
        post("$templateUri/createdomains", null, 204, ADMIN)

        and: "looking up secondary client's domain"
        def secondaryClientDomain = get("/domains", 200, SECONDARY_CLIENT_USER).body
                .find { it.name == domainName }

        then: "its metadata is correct"
        secondaryClientDomain.authority == "JJ"
        secondaryClientDomain.templateVersion == "1.0.0"

        and: "it contains the decision, inspection & incarnation config"
        secondaryClientDomain.decisions.truthy.elementSubType == "server"
        with(get("/domains/${secondaryClientDomain.id}/inspections/alwaysAddPart", 200, SECONDARY_CLIENT_USER).body) {
            condition.type == "constant"
        }
        with(get("/domains/$secondaryClientDomain.id/incarnation-configuration", 200, SECONDARY_CLIENT_USER).body) {
            exclude == ["COMPOSITE"]
        }

        and: "an element can be created in the domain"
        def secondaryClientUnitId = post("/units", [
            name: "secondary client unit",
            domains: [
                [targetUri: secondaryClientDomain._self]
            ]
        ], 201, SECONDARY_CLIENT_USER).body.resourceId
        post("/assets", [
            name: "gain server",
            owner: [targetUri: "http://localhost/units/$secondaryClientUnitId"],
            domains: [
                (secondaryClientDomain.id): [
                    subType: "server",
                    status: "off"
                ]
            ]
        ], 201, SECONDARY_CLIENT_USER)
    }

    def "update controlImplementationConfiguration"() {
        when: "creating a new domain"
        def domainName = "Domain creation test ${randomUUID()}"
        def domainId = post("/content-creation/domains", [
            name: domainName,
            abbreviation: "dct",
            description: "best one ever",
            authority: "uz",
        ], 201, CONTENT_CREATOR).body.resourceId

        put("/content-creation/domains/$domainId/element-type-definitions/control",  [
            subTypes:[
                c1:[
                    statuses:['c0', 'c1'],
                    sortKey : 1
                ]
            ]
        ], null, 204, CONTENT_CREATOR)

        and: "we update controlImplementationConfiguration"
        def domainUpdatedBefore = get("/domains/$domainId").body.updatedAt
        put("/content-creation/domains/$domainId/control-implementation-configuration", ["complianceControlSubType": "c1",
            "mitigationControlSubType": "c1"], null, 204, CONTENT_CREATOR)

        then:"the domain is updated"
        with(get("/domains/$domainId").body) {
            updatedAt > domainUpdatedBefore
            controlImplementationConfiguration ==  ["complianceControlSubType": "c1",
                "mitigationControlSubType": "c1"]
        }

        and: "the change is exported"
        exportDomain(domainId).controlImplementationConfiguration == [ complianceControlSubType: "c1",
            mitigationControlSubType: "c1"]

        when:
        put("/content-creation/domains/$domainId/control-implementation-configuration", ["mitigationControlSubType": "c1"], null, 204, CONTENT_CREATOR)

        then:
        with(get("/domains/$domainId").body) {
            controlImplementationConfiguration ==  ["mitigationControlSubType": "c1"]
        }

        when:
        get("/domains/$domainId").with {
            put("/content-creation/domains/$domainId/control-implementation-configuration", ["complianceControlSubType": "c1"], getETag(), 204, CONTENT_CREATOR)
        }

        then:
        with(get("/domains/$domainId").body) {
            controlImplementationConfiguration ==  ["complianceControlSubType": "c1"]
        }

        when: "the controlImplementationConfiguration is cleared"
        def etag = get("/domains/$domainId").getETag()
        put("/content-creation/domains/$domainId/control-implementation-configuration", [:], null, 204, CONTENT_CREATOR)

        then:
        with(get("/domains/$domainId")) {
            etag != getETag()
            body.controlImplementationConfiguration ==  [:]
        }

        and: "an undefined subtype cannot be used"
        put("/content-creation/domains/$domainId/control-implementation-configuration", ["complianceControlSubType": "c1",
            "mitigationControlSubType": "c2"], null, 400, CONTENT_CREATOR).with {
            body.message == "Sub type c2 is not defined"
        }
    }

    def "CRUD risk definition"() {
        given: "a new domain"
        def domainName = "Risk definition test ${randomUUID()}"
        def newDomainId = post("/content-creation/domains", [
            name: domainName,
            abbreviation: "rdt",
            description: "it's kind of risky",
            authority: "JJ",
        ], 201, CONTENT_CREATOR).body.resourceId
        put("/content-creation/domains/$newDomainId/element-type-definitions/scenario", [
            subTypes: [
                HypotheticalScenario: [
                    statuses: ["OLD", "NEW"]
                ]
            ],
            links: [
                requiredScenario: [
                    targetType: "scenario",
                    targetSubType: "HypotheticalScenario",
                ]
            ]
        ], null, 204, CONTENT_CREATOR)
        get("/units/$unitId").with {
            body.domains.add([targetUri: "/domains/$newDomainId"])
            put(body._self, body, getETag())
        }
        def domainCreationTime = get("/domains/$newDomainId").body.createdAt

        when: "adding a modified version of the test-domain risk def"
        def definition = get("/domains/$testDomainId").body.riskDefinitions.riskyDef
        definition.categories.removeIf{it.id == "I"}
        put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 201, CONTENT_CREATOR)

        then: "it can be retrieved"
        with(get("/domains/$newDomainId").body) {
            riskDefinitions.simpleDef.categories.find { it.id == "C" }.potentialImpacts.size() == 3
            riskDefinitions.simpleDef.categories.find { it.id == "I" } == null
            createdAt == domainCreationTime
            updatedAt > domainCreationTime
        }

        when: "using the risk definition on a scenario"
        def scenarioInDomainUri = post("/domains/$newDomainId/scenarios", [
            name: "risk test scenario",
            subType: "HypotheticalScenario",
            status: "OLD",
            owner: [targetUri: "/units/$unitId"],
            riskValues: [
                simpleDef: [
                    potentialProbability: 1
                ]
            ]
        ]).location

        then: "it has been applied"
        get(scenarioInDomainUri).body.riskValues.simpleDef.potentialProbability == 1

        when: "modifying impact inheriting links"
        definition.impactInheritingLinks.scenario = ["requiredScenario"]
        put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 200, CONTENT_CREATOR)

        then: "the change has been applied"
        exportDomain(newDomainId).riskDefinitions.simpleDef.impactInheritingLinks.scenario == ["requiredScenario"]

        when: "deleting the risk definition"
        delete("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", 204, CONTENT_CREATOR)

        then: "it is missing from the domain"
        get("/domains/$newDomainId").body.riskDefinitions == [:]

        and: "it has been removed from the scenario"
        with(get(scenarioInDomainUri).body) {
            riskValues == [:]
            updatedBy == owner.contentCreatorUserName
        }
    }

    def "ordinals are ignored when saving risk definition"() {
        given: "a new domain"
        def domainName = "Risk definition test ${randomUUID()}"
        def newDomainId = post("/content-creation/domains", [
            name: domainName,
            abbreviation: "rdt",
            description: "it's kind of risky",
            authority: "JJ",
        ], 201, CONTENT_CREATOR).body.resourceId

        when: "saving a risk definition with wacky ordinals"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.probability.levels[0].ordinalValue = 5
            definition.probability.levels[1].ordinalValue = -35000
            definition.probability.levels[2].ordinalValue = 0
            put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 201, CONTENT_CREATOR)
        }

        then: "correct ordinals have been applied"
        with(get("/domains/$newDomainId").body.riskDefinitions.simpleDef) {
            probability.levels[0].ordinalValue == 0
            probability.levels[1].ordinalValue == 1
            probability.levels[2].ordinalValue == 2
        }
    }

    def "risk definition is validated"() {
        given: "a new domain"
        def domainName = "Risk definition validation test ${randomUUID()}"
        def newDomainId = post("/content-creation/domains", [
            name: domainName,
            abbreviation: "rdvt",
            description: "let's test risk definition validation",
            authority: "JJ",
        ], 201, CONTENT_CREATOR).body.resourceId

        expect: "no risk method to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.riskMethod = null
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body["riskMethod"] == "must not be null"
            }
        }

        and: "no implementation state to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.implementationStateDefinition = null
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body["implementationStateDefinition"] == "must not be null"
            }
        }

        and: "no probability definition to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.probability = null
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body["probability"] == "must not be null"
            }
        }

        and: "no probability levels to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.probability.levels = []
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body["probability.levels"] == "must not be empty"
            }
        }

        and: "no categories to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.categories = []
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body["categories"] == "must not be empty"
            }
        }

        and: "redundant category IDs to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.categories.find { it.id == "I" }.id = "C"
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body.message == "Categories not unique."
            }
        }

        and: "redundant risk value IDs to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.riskValues.find { it.symbolicRisk == "symbolic_risk_3" }.symbolicRisk = "symbolic_risk_2"
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body.message == "SymbolicRisk not unique."
            }
        }

        and: "empty matrix to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.categories[0].valueMatrix = []
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body.message == "Value matrix for category C does not conform to impacts."
            }
        }

        and: "undefined symbolic risk in matrix to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.categories.find { it.id == "D" }.valueMatrix[0][0] = [
                ordinalValue: 0,
                symbolicRisk: "symbolic_risk_99",
            ]
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body.message == "Invalid risk values for category D: [RiskValue(symbolicRisk=symbolic_risk_99)]"
            }
        }

        and: "non-matching ordinal value and symbolic risk in matrix to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.categories.find { it.id == "D" }.valueMatrix[0][0] = [
                ordinalValue: 2,
                symbolicRisk: "symbolic_risk_1",
            ]
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body.message == "Invalid risk values for category D: [RiskValue(symbolicRisk=symbolic_risk_1)]"
            }
        }

        and: "missing impact in matrix to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.categories.find { it.id == "D" }.valueMatrix.removeLast()
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body.message == "Value matrix for category D does not conform to impacts."
            }
        }

        and: "missing probability in matrix to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.categories.find { it.id == "D" }.valueMatrix[1].removeLast()
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 400, CONTENT_CREATOR)) {
                body.message == "Value matrix for category D does not conform to probability."
            }
        }

        and: "invalid impact-inheriting link to be illegal"
        get("/domains/$testDomainId").body.riskDefinitions.riskyDef.with { definition ->
            definition.impactInheritingLinks.person = [
                "myImaginaryFriend"
            ]
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 422, CONTENT_CREATOR)) {
                body.message == "Link type 'myImaginaryFriend' does not exist for persons"
            }
        }
    }

    def "risk definition can have no riskMatrix"() {
        given: "the a new domain"
        def domainName = "Risk definition validation test ${randomUUID()}"
        def newDomainId = post("/content-creation/domains", [
            name: domainName,
            abbreviation: "rdvt",
            description: "let's test risk definition validation",
            authority: "JJ",
        ], 201, CONTENT_CREATOR).body.resourceId

        when: "adding a copy of DSRA without any risk matrices to the new domain"
        get("/domains/$dsgvoDomainId").body.riskDefinitions.DSRA.with { definition ->
            definition.impactInheritingLinks = [:]
            definition.categories.each { it.valueMatrix = null }
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 201, CONTENT_CREATOR)) {
                body.message == "Risk definition created"
            }
        }

        then: "it is perstisted"
        with(get("/domains/$newDomainId").body.riskDefinitions.simpleDef) {definition->
            definition.categories.each { it.valueMatrix == null }
        }
    }

    def "impact values are updated when category is removed or added"() {
        given: "some elements with risks"
        def domainId = copyDomain(dsgvoDomainId)

        get("/units/$unitId").with{
            body.domains.add([targetUri: "/domains/$domainId"])
            put("/units/$unitId", body, getETag())
        }

        def elementWithImpactId = post("/domains/$domainId/$type", [
            name: "asset-0",
            subType: (subtype),
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            riskValues: [
                DSRA : [
                    potentialImpacts: [
                        "C": 2,
                        "I": 2,
                        "A": 2,
                        "R": 2
                    ]
                ]
            ]
        ]).body.resourceId

        def scenarioId = post("/domains/$domainId/scenarios", [
            name: "$type risk test scenario",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            riskValues: [
                DSRA : [
                    potentialProbability: 2,
                    potentialProbabilityExplanation: "When it happens",
                ]
            ]
        ]).body.resourceId

        post("/$type/$elementWithImpactId/risks", [
            scenario: [ targetUri: "/scenarios/$scenarioId"],
            domains: [
                (domainId): [
                    reference: [targetUri: "/domains/$domainId"],
                    riskDefinitions: [
                        DSRA: [
                            impactValues: [
                                [
                                    category      : "C",
                                    specificImpact: 2,
                                ],
                                [
                                    category      : "A",
                                    specificImpact: 2,
                                ],
                                [
                                    category      : "I",
                                    specificImpact: 2,
                                ],
                                [
                                    category      : "R",
                                    specificImpact: 2,
                                ],
                            ],
                            riskValues  : [
                                [
                                    category               : "R",
                                    userDefinedResidualRisk: 0,
                                ],
                                [
                                    category               : "C",
                                    userDefinedResidualRisk: 1,
                                ],
                                [
                                    category               : "A",
                                    userDefinedResidualRisk: 1,
                                ],
                                [
                                    category               : "I",
                                    userDefinedResidualRisk: 2,
                                ],
                            ]
                        ]
                    ]
                ]]])

        when: "removing r"
        get("/domains/$domainId").body.riskDefinitions.DSRA.with { definition ->
            definition.categories.removeLast()
            with(put("/content-creation/domains/$domainId/risk-definitions/DSRA", definition, null, 200, CONTENT_CREATOR)) {
                body.message == "Risk definition updated"
            }
        }

        then: "r is gone in the ra"
        with(get("/domains/$domainId/$type/$elementWithImpactId").body.riskValues.DSRA.potentialImpacts) {
            C == 2
            I == 2
            A == 2
            R == null
        }

        and: "in the scenario nothing changes"
        with(get("/domains/$domainId/scenarios/$scenarioId").body.riskValues.DSRA) {
            potentialProbability == 2
            potentialProbabilityExplanation == "When it happens"
        }

        and:"r is missing in the risk"
        with(get("/$type/$elementWithImpactId/risks/$scenarioId").body.domains.(domainId).riskDefinitions.DSRA) {
            riskValues*.category ==~ ["C", "I", "A"]
            impactValues*.category ==~ ["C", "I", "A"]
        }

        when: "remove a"
        get("/domains/$domainId").body.riskDefinitions.DSRA.with { definition ->
            definition.categories.removeLast()
            with(put("/content-creation/domains/$domainId/risk-definitions/DSRA", definition, null, 200, CONTENT_CREATOR)) {
                body.message == "Risk definition updated"
            }
        }

        then: "a is gone"
        with(get("/domains/$domainId/$type/$elementWithImpactId").body.riskValues.DSRA.potentialImpacts) {
            C == 2
            I == 2
        }

        and:
        with(get("/$type/$elementWithImpactId/risks/$scenarioId").body.domains.(domainId).riskDefinitions.DSRA) {
            riskValues*.category ==~ ["C", "I"]
            impactValues*.category ==~ ["C", "I"]
        }

        when: "adding a category to the riskdefinition"
        def newCategory = get("/domains/$dsgvoDomainId").body.riskDefinitions.DSRA.categories.find { it.id == "C" }
        newCategory.id = "newCat"
        newCategory.translations.de.name = "my new Cat"

        get("/domains/$domainId").body.riskDefinitions.DSRA.with { definition ->
            definition.categories.add(newCategory)
            with(put("/content-creation/domains/$domainId/risk-definitions/DSRA", definition, null, 200, CONTENT_CREATOR)) {
                body.message == "Risk definition updated"
            }
        }

        then: "potential impacts haven't changed"
        with(get("/domains/$domainId/$type/$elementWithImpactId").body.riskValues.DSRA.potentialImpacts) {
            C == 2
            I == 2
        }

        and: "the new category is available on the risk"
        with(get("/$type/$elementWithImpactId/risks/$scenarioId").body.domains.(domainId).riskDefinitions.DSRA) {
            riskValues*.category ==~ ["C", "I", "newCat"]
            impactValues*.category ==~ ["C", "I", "newCat"]
        }

        when: "adding a new element"
        def newElementWithImpactId = post("/domains/$domainId/$type", [
            name: "asset-0",
            subType: (subtype),
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            riskValues: [
                DSRA : [
                    potentialImpacts: [
                        "C": 2,
                        "I": 2,
                        "newCat": 1
                    ]
                ]
            ]
        ]).body.resourceId

        then: "newCat-1 there"
        with(get("/domains/$domainId/$type/$newElementWithImpactId").body.riskValues.DSRA.potentialImpacts) {
            C == 2
            I == 2
            newCat == 1
        }

        where:
        type|subtype
        "assets"|"AST_Datatype"
        "scopes"|"SCP_Processor"
        "processes"|"PRO_DPIA"
    }

    def "risk definition can be updated"() {
        given: "a new domain"
        def domainName = "Risk definition validation test ${randomUUID()}"
        def newDomainId = post("/content-creation/domains", [
            name: domainName,
            abbreviation: "rdvt",
            description: "let's test risk definition validation",
            authority: "JJ",
        ], 201, CONTENT_CREATOR).body.resourceId

        put("/content-creation/domains/$newDomainId/element-type-definitions/asset",  [
            subTypes:[
                server:[
                    statuses:['on', 'off'],
                    sortKey : 1
                ]
            ],
            links:[
                serverLink :[
                    targetType: 'asset',
                    targetSubType: 'server',
                ]
            ],
            translations: [
                en: [
                    asset_server_singular: "Server",
                    asset_server_plural: "Servers",
                    asset_server_status_off: "off",
                    asset_server_status_on: "on",
                    serverLink: "link"
                ]
            ]
        ], "", 204, CONTENT_CREATOR)

        when: "adding a modified copy of DSRA to the new domain"
        get("/domains/$dsgvoDomainId").body.riskDefinitions.DSRA.with { definition ->
            definition.impactInheritingLinks = ["asset":  ['serverLink']]
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 201, CONTENT_CREATOR)) {
                body.message == "Risk definition created"
            }
        }

        and: "changing impactInheritingLinks"
        get("/domains/$newDomainId").body.riskDefinitions.simpleDef.with { definition ->
            definition.impactInheritingLinks = [:]
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 200, CONTENT_CREATOR)) {
                body.message == "Risk definition updated"
            }
        }

        then: "the change is persisted"
        with(get("/domains/$newDomainId").body.riskDefinitions.simpleDef) {
            it.impactInheritingLinks == [:]
        }

        when: "changing translations"
        get("/domains/$newDomainId").body.riskDefinitions.simpleDef.with { definition ->
            definition.riskValues[0].htmlColor = "#44444"
            definition.riskValues[0].translations.de.name = "Risk name 1"
            definition.riskValues[0].translations.de.description = "risk description"
            definition.probability.translations.de.name = "a new name"
            definition.probability.translations.de.description = "a new description"

            definition.implementationStateDefinition.translations.de.name = "a new name"
            definition.categories.find { it.id == "C" }.translations.de.name = "a new name"
            definition.categories.find { it.id == "C" }.potentialImpacts[0].translations.de.name = "a new name"
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 200, CONTENT_CREATOR)) {
                body.message == "Risk definition updated"
            }
        }

        then: "the change is persisted"
        with(get("/domains/$newDomainId").body.riskDefinitions.simpleDef) {
            it.riskValues[0].htmlColor == "#44444"
            it.riskValues[0].translations.de.name == "Risk name 1"
            it.probability.translations.de.name == "a new name"
            it.probability.translations.de.description == "a new description"
            it.implementationStateDefinition.translations.de.name == "a new name"
            it.categories.find { it.id == "C" }.translations.de.name == "a new name"
            it.categories.find { it.id == "C" }.potentialImpacts[0].translations.de.name == "a new name"
        }

        when: "removing a category"
        get("/domains/$newDomainId").body.riskDefinitions.simpleDef.with { definition ->
            definition.categories.removeLast()
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 200, CONTENT_CREATOR)) {
                body.message == "Risk definition updated"
            }
        }

        then: "the change is persisted"
        with(get("/domains/$newDomainId").body.riskDefinitions.simpleDef) {
            it.categories.size() == 3
            it.categories.find { it.id == "R"  } == null
        }

        when:
        def newCategory = get("/domains/$dsgvoDomainId").body.riskDefinitions.DSRA.categories.find { it.id == "C" }
        newCategory.id = "newCat-1"
        newCategory.translations.de.name = "my new Cat"

        then: "adding a category"
        get("/domains/$newDomainId").body.riskDefinitions.simpleDef.with { definition ->
            definition.categories.add(newCategory)
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 200, CONTENT_CREATOR)) {
                body.message == "Risk definition updated"
            }
        }

        and: "the change is persisted"
        with(get("/domains/$newDomainId").body.riskDefinitions.simpleDef) {
            it.categories.size() == 4
            it.categories.find { it.id == newCategory.id }.translations.de.name == newCategory.translations.de.name
        }

        expect: "a changed value in a risk matrix to be illegal"
        get("/domains/$newDomainId").body.riskDefinitions.simpleDef.with { definition ->
            definition.categories[0].valueMatrix[0][0] = definition.categories[0].valueMatrix[3][3]
            with(put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 422, CONTENT_CREATOR)) {
                body.message ==~ /Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed.*/
            }
        }
    }

    def "cannot create domain with name of existing template"() {
        expect:
        post("/content-creation/domains", [
            name: "DS-GVO",
            authority: "JJ",
        ], 409, CONTENT_CREATOR)
        .body.message == "Templates already exist for domain name 'DS-GVO'"
    }

    def "cannot create multiple domains with the same name"() {
        given: "a random name"
        def name = "conflict test domain ${randomUUID()}"

        expect: "initial creation to succeed"
        post("/content-creation/domains", [
            name: name,
            authority: "JJ",
        ], 201, CONTENT_CREATOR)

        and: "second creation to fail"
        post("/content-creation/domains", [
            name: name,
            authority: "JJ",
        ], 409, CONTENT_CREATOR)
        .body.message == "A domain with name '$name' already exists in this client"
    }

    def "create new domain and delete"() {
        when: "creating a new domain"
        def domainName = "Domain deletion test 1"
        def domainId = post("/content-creation/domains", [
            name: domainName,
            abbreviation: "11",
            description: "my",
            authority: "qq",
        ], 201, CONTENT_CREATOR).body.resourceId

        then: "it can be retrieved"
        with(get("/domains/$domainId").body) {
            name == domainName
            abbreviation == "11"
            description == "my"
            authority == "qq"
            templateVersion == "0.1.0"
            domainTemplate == null
        }

        when:
        delete("/content-creation/domains/${domainId}", 204)

        then:
        get("/domains/$domainId", 404)
    }

    def "delete only domains not in use"() {
        when: "we have a domain"
        def domainId = post("/content-creation/domains", [
            name: "Domain deletion test 2",
            abbreviation: "111",
            description: "my1",
            authority: "qq1",
        ], 201, CONTENT_CREATOR).body.resourceId

        and: "it is linked to a unit"
        def unitId = post("/units", [
            name: 'my unit used by domain',
            domains: [
                [targetUri: "/domains/${domainId}"]
            ]
        ]).body.resourceId

        then: "delete is not allowed"
        with(delete("/content-creation/domains/${domainId}", 409).body) {
            message ==~ /Domain in use.*/
        }

        when: "we remove the link"
        delete("/units/${unitId}")

        then: "delete is allowed and the domain is deleted"
        delete("/content-creation/domains/${domainId}",204)
        with(get("/domains/$domainId", 404).body) {
            message ==~ /domain .* not found/
        }
    }

    def "create new domain with schema and delete"() {
        given:
        def domainId = post("/content-creation/domains", [
            name: "Domain deletion test 3",
            abbreviation: "11",
            description: "my",
            authority: "qq",
        ], 201, CONTENT_CREATOR).body.resourceId

        when: "defining an element type in the domain"
        postAssetObjectSchema(domainId)

        and:
        delete("/content-creation/domains/${domainId}", 204)

        then:
        get("/domains/${domainId}", 404)
    }
}
