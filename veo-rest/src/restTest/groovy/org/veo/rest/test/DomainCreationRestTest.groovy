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

        when: "making other risk definition modifications"
        definition.categories[1].with{
            potentialImpacts.removeLast()
            valueMatrix.removeLast()
        }

        then: "the update should fail (for now)"
        put("/content-creation/domains/$newDomainId/risk-definitions/simpleDef", definition, null, 422, CONTENT_CREATOR)
                .body.message ==~ /Your modifications on this existing risk definition are not supported yet.+/

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
    def "cannot delete risk definition from a template"() {
        expect: "trying to delete the DS-GVO risk definition to be illegal"
        delete("/content-creation/domains/$dsgvoDomainId/risk-definitions/DSRA", 409, CONTENT_CREATOR)
                .body.message == "Deleting a risk definition that is part of a domain template is currently not supported."
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
                body.message == "Risk matrix for category C is empty."
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
