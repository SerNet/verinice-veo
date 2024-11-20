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

import static org.veo.rest.test.UserType.ADMIN
import static org.veo.rest.test.UserType.CONTENT_CREATOR

import java.util.function.Consumer

class DomainUpdateRestTest extends VeoRestTest {

    String currentDomainTemplateId
    String templateName
    String unitId
    String oldDomainId

    def setup() {
        templateName = "domain update test template ${UUID.randomUUID()}"
        currentDomainTemplateId = post("/content-creation/domain-templates", template, 201, CONTENT_CREATOR).body.resourceId
        post("/domain-templates/$currentDomainTemplateId/createdomains", null, 204, ADMIN)
        oldDomainId = domains.find { it.name == templateName }.id
        unitId = postNewUnit("U1", [oldDomainId, testDomainId]).resourceId
    }

    def "updates client to new domain template version and migrates elements"() {
        given: "a scope and a process linked to it in the old domain"
        def scopeId = post("/scopes", [
            name: "target scope",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            domains: [
                (oldDomainId): [
                    subType: "SCP_ResponsibleBody",
                    status: "NEW"
                ]
            ]
        ]).body.resourceId
        def processId = post("/processes", [
            name: "old process",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            domains: [
                (oldDomainId): [
                    subType: "PRO_DataProcessing",
                    status: "NEW"
                ]
            ],
            links: [
                processToScopeLink: [
                    [
                        target: [targetUri: "$baseUrl/scopes/$scopeId"]
                    ]
                ]
            ]
        ]).body.resourceId

        and: "a link back from the scope to the process"
        def scopeResponse = get("/scopes/$scopeId")
        def scope = scopeResponse.body
        scope.links.scopeToProcessLink = [
            [
                target: [targetUri: "$baseUrl/processes/$processId"]
            ]
        ]
        put("/scopes/$scopeId", scope, scopeResponse.getETag())

        and: "an incarnated catalog item"
        def c1SymId = get("/domains/$oldDomainId/catalog-items").body.items.first().id
        post("/units/${unitId}/incarnations", get("/units/${unitId}/domains/${oldDomainId}/incarnation-descriptions?itemIds=${c1SymId}").body)

        when: "migrating to the new domain"
        def newDomainId = createNewTemplateAndMigrate {
            it.templateVersion = "1.0.1"
        }
        def migratedScope = get("/scopes/$scopeId").body

        then: "the sub type and link are still present under the new domain"
        migratedScope.domains.keySet() =~ [newDomainId]
        migratedScope.domains[newDomainId].subType == "SCP_ResponsibleBody"
        with(migratedScope.links.scopeToProcessLink) {
            size() == 1
            first().target.targetUri == "$owner.baseUrl/processes/$processId"
        }

        when: "fetching the migrated process"
        def migratedProcess = get("/processes/$processId").body

        then: "the sub type & link are still present under the new domain"
        migratedProcess.domains.keySet() =~ [newDomainId]
        migratedProcess.domains[newDomainId].subType == "PRO_DataProcessing"
        with(migratedProcess.links.processToScopeLink) {
            size() == 1
            first().target.targetUri == "$owner.baseUrl/scopes/$scopeId"
        }

        and: "decision results are present on migrated element"
        migratedProcess.domains[newDomainId].decisionResults.riskAnalyzed.decisiveRule == 0

        when: "fetching incarnation descriptions for the catalog item in the new domain"
        def c1IncarnationDescriptions = get("/units/$unitId/domains/$newDomainId/incarnation-descriptions?itemIds=$c1SymId&useExistingIncarnations=ALWAYS").body

        then: "the old incarnation is detected"
        c1IncarnationDescriptions.parameters.empty

        when: "adding a link from a new process to an old scope"
        post("/processes", [
            name: "new process",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            domains: [
                (newDomainId): [
                    subType: "PRO_DataProcessing",
                    status: "NEW",
                ]
            ],
            links: [
                processToScopeLink: [
                    [
                        target: [targetUri: "$baseUrl/scopes/$scopeId"]
                    ]
                ]
            ]
        ])

        then:
        noExceptionThrown()
    }

    def "updates client to new domain template version and migrates elements with incompatible changes"() {
        given: "a scope and a process linked to it in the old domain"
        def scopeId = post("/domains/$oldDomainId/scopes", [
            name: "target scope",
            owner: [targetUri: "/units/$unitId"],
            subType: "SCP_ResponsibleBody",
            status: "NEW"
        ]).body.resourceId
        def processId = post("/domains/$oldDomainId/processes", [
            name: "old process",
            owner: [targetUri: "/units/$unitId"],
            subType: "PRO_DataProcessing",
            status: "NEW",
            customAspects: [
                "test1":
                [
                    Attribute1: true,
                    Attribute2: "my test text",
                ],
                "test2": [
                    Attribute1: false,
                    Attribute2: true,
                ],
                "everlasting": [
                    perfectNumber: 42
                ],
            ],
            links: [
                processToScopeLink: [
                    [
                        target: [targetUri: "/scopes/$scopeId"]
                    ]
                ]
            ]
        ]).body.resourceId

        when: "migrating to a template with breaking changes"
        def newDomainId = createNewTemplateAndMigrate {
            it.templateVersion = "2.0.0"

            it.elementTypeDefinitions.process.customAspects.test1 = [
                'attributeDefinitions': [
                    'Attribute3': ['type': 'boolean']
                ]]

            it.elementTypeDefinitions.process.customAspects.test2 = [
                'attributeDefinitions': [
                    'Attribute1': ['type': 'boolean'],
                    'Attribute2': ['type': 'boolean'],
                    'Attribute3': ['type': 'text']
                ]]

            it.domainMigrationDefinition = [migrations: migrationDefinitionChangeKey()]
        }

        def migratedScope = get("/domains/$newDomainId/scopes/$scopeId").body

        then: "the sub type is still present under the new domain"
        migratedScope.subType == "SCP_ResponsibleBody"

        when:"We get the process in the new domain"
        def processInNewDomain = get("/domains/$newDomainId/processes/$processId").body

        then: "the process has been migrated"
        with(processInNewDomain) {
            name == "old process"
            customAspects.test1.Attribute1 == null
            customAspects.test1.Attribute2 == null
            customAspects.test1.Attribute3 == true
            customAspects.test2.Attribute1 == false
            customAspects.test2.Attribute2 == true
            customAspects.test2.Attribute3 == "my test text"
            customAspects.everlasting.perfectNumber == 42
            links.processToScopeLink.size() == 1
            links.processToScopeLink.first().target.name == "target scope"
            links.processToScopeLink.first().target.associatedWithDomain == true
        }

        when: "migrating to a new patch template"
        newDomainId = createNewTemplateAndMigrate {
            it.templateVersion = "2.0.1"
        }

        then: "the process has been migrated without any changes"
        with(get("/domains/$newDomainId/processes/$processId").body) {
            customAspects.test1.Attribute1 == null
            customAspects.test1.Attribute2 == null
            customAspects.test1.Attribute3 == true
            customAspects.test2.Attribute1 == false
            customAspects.test2.Attribute2 == true
            customAspects.test2.Attribute3 == "my test text"
            customAspects.everlasting.perfectNumber == 42
            links.processToScopeLink.size() == 1
            links.processToScopeLink.first().target.name == "target scope"
            links.processToScopeLink.first().target.associatedWithDomain == true
        }
    }

    def "add and remove CAs during major update"() {
        given:
        def processId = post("/domains/$oldDomainId/processes", [
            name: "old process",
            owner: [targetUri: "/units/$unitId"],
            subType: "PRO_DataProcessing",
            status: "NEW",
            customAspects: [
                "test1":
                [
                    Attribute1: false,
                    Attribute2: "old comment",
                ],
                test2:[
                    Attribute1: true,
                    Attribute2: true,
                ],
                everlasting: [
                    perfectNumber: 42
                ],
            ],
        ]).body.resourceId

        when: "migrating to the new domain"
        def newDomainId = createNewTemplateAndMigrate {
            it.templateVersion = "2.0.0"

            it.elementTypeDefinitions.process.customAspects.remove('test1')
            it.elementTypeDefinitions.process.customAspects.test2.attributeDefinitions.newText = [
                type: 'text'
            ]
            it.elementTypeDefinitions.process.customAspects.newCA = [
                attributeDefinitions: [
                    newBool: [
                        type: 'boolean'
                    ]
                ]
            ]

            it.domainMigrationDefinition = [
                migrations: [
                    [
                        description: [en: "move text attribute from removed CA to an extended CA"],
                        id: "move-text",
                        oldDefinitions: [
                            [
                                type: "customAspectAttribute",
                                elementType: "process",
                                customAspect: "test1",
                                attribute: "Attribute2"
                            ]
                        ],
                        newDefinitions: [
                            [
                                type: "customAspectAttribute",
                                elementType: "process",
                                customAspect: "test2",
                                attribute: "newText",
                                migrationExpression: [
                                    type: 'customAspectAttributeValue',
                                    customAspect: 'test1',
                                    attribute: 'Attribute2'
                                ]
                            ],
                        ],
                    ],
                    [
                        description: [en: "move boolean attribute from removed CA to a new CA"],
                        id: "move-bool",
                        oldDefinitions: [
                            [
                                type: "customAspectAttribute",
                                elementType: "process",
                                customAspect: "test1",
                                attribute: "Attribute1"
                            ],
                        ],
                        newDefinitions: [
                            [
                                type: "customAspectAttribute",
                                elementType: "process",
                                customAspect: "newCA",
                                attribute: "newBool",
                                migrationExpression: [
                                    type: 'customAspectAttributeValue',
                                    customAspect: 'test1',
                                    attribute: 'Attribute1'
                                ]
                            ],
                        ],
                    ],
                ],]
        }
        def processInNewDomain = get("/domains/$newDomainId/processes/$processId").body

        then: "the process has the migrated values in the new domain"
        with(processInNewDomain) {
            name == "old process"
            customAspects.test1 == null
            customAspects.test2.Attribute1 == true
            customAspects.test2.Attribute2 == true
            customAspects.test2.newText == "old comment"
            customAspects.newCA.newBool == false
            customAspects.everlasting.perfectNumber == 42
        }
    }

    def "missing attribute values are handled correctly"() {
        given:
        def processWithoutTextId = post("/domains/$oldDomainId/processes", [
            name: "old process",
            owner: [targetUri: "/units/$unitId"],
            subType: "PRO_DataProcessing",
            status: "NEW",
            customAspects: [
                "test1":
                [
                    Attribute1: false,
                ],
            ],
        ]).body.resourceId
        def processWithTextId = post("/domains/$oldDomainId/processes", [
            name: "old process",
            owner: [targetUri: "/units/$unitId"],
            subType: "PRO_DataProcessing",
            status: "NEW",
            customAspects: [
                "test1":
                [
                    Attribute1: true,
                    Attribute2: "Famous last words",
                ],
            ],
        ]).body.resourceId

        when: "migrating to the new domain"
        def newDomainId = createNewTemplateAndMigrate {
            it.templateVersion = "2.0.0"

            it.elementTypeDefinitions.process.customAspects.test1.attributeDefinitions.with {
                it.remove('Attribute2')
                it.Attribute3 = [
                    type: 'text'
                ]
            }
            it.domainMigrationDefinition = [
                migrations: [
                    [
                        description: [en: "rename attr 2 to attr 3"],
                        id: "move-text",
                        oldDefinitions: [
                            [
                                type: "customAspectAttribute",
                                elementType: "process",
                                customAspect: "test1",
                                attribute: "Attribute2"
                            ]
                        ],
                        newDefinitions: [
                            [
                                type: "customAspectAttribute",
                                elementType: "process",
                                customAspect: "test1",
                                attribute: "Attribute3",
                                migrationExpression: [
                                    type: 'customAspectAttributeValue',
                                    customAspect: 'test1',
                                    attribute: 'Attribute2'
                                ]
                            ],
                        ],
                    ],
                ],
            ]
        }

        then: "the process has the migrated values in the new domain"
        with(get("/domains/$newDomainId/processes/$processWithoutTextId").body) {
            name == "old process"
            customAspects.test1 == [
                Attribute1: false
            ]
        }
        with(get("/domains/$newDomainId/processes/$processWithTextId").body) {
            name == "old process"
            customAspects.test1 == [
                Attribute1: true,
                Attribute3: "Famous last words"
            ]
        }
    }

    def "information in other domains is untouched during migration"() {
        given: "a document associated with two domains"
        def targetPersonId = post("/domains/$testDomainId/persons", [
            name: "Manuel el autor",
            subType: "MasterOfDisaster",
            status: "CAUSING_REAL_DISASTERS",
            owner: [targetUri: "/units/$unitId"],
        ]).body.resourceId
        def documentId = post("/domains/$testDomainId/documents", [
            name: "protagonist document",
            subType: "Manual",
            status: "OUTDATED",
            owner: [targetUri: "/units/$unitId"],
            customAspects: [
                details: [
                    numberOfPages: 83
                ]
            ],
            links: [
                author: [
                    [
                        target: [targetUri: "/persons/$targetPersonId"],
                        attributes: [
                            writingFinished: "2024-04-01"
                        ]
                    ]
                ]
            ]
        ]).body.resourceId
        post("/domains/$oldDomainId/documents/$documentId", [
            subType: "DOC_Document",
            status: "NEW",
        ], 200)

        when: "migrating to a new template version"
        def newDomainId = createNewTemplateAndMigrate {
            it.templateVersion = "1.0.1"
        }

        then: "the document has been migrated"
        get("/domains/$newDomainId/documents/$documentId").body.subType == "DOC_Document"

        and: "the information in the other domain hasn't been altered"
        with(get("/domains/$testDomainId/documents/$documentId").body) {
            subType == "Manual"
            status == "OUTDATED"
            customAspects.details.numberOfPages == 83
            links.author[0].target.name == "Manuel el autor"
            links.author[0].attributes.writingFinished == "2024-04-01"
        }
    }

    def "removes risk values for deleted risk definition"() {
        given:
        def scopeId = post("/domains/$oldDomainId/scopes", [
            name: "scp",
            subType: "SCP_ResponsibleBody",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            riskDefinition: "definitelyRisky",
            riskValues: [
                definitelyRisky: [
                    potentialImpacts: [
                        nyanCat: 0
                    ]
                ]
            ],
        ]).body.resourceId
        def scenarioId = post("/domains/$oldDomainId/scenarios", [
            name: "scn",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            riskValues: [
                definitelyRisky: [
                    potentialProbability: 0
                ]
            ],
        ]).body.resourceId
        post("/scopes/$scopeId/risks", [
            scenario: [targetUri: "/scenarios/$scenarioId"],
            domains: [
                (oldDomainId): [
                    reference: [targetUri: "/domains/$oldDomainId"],
                    riskDefinitions: [
                        definitelyRisky: [
                            probability: [
                                specificProbability: 0
                            ]
                        ]
                    ]
                ]
            ]
        ])

        when: "migrating to a new template with no risk definitions"
        def newDomainId = createNewTemplateAndMigrate {
            it.templateVersion = "2.0.0"
            it.riskDefinitions = [:]
        }

        then:
        with(get("/domains/$newDomainId/scopes/$scopeId").body) {
            riskDefinition == null
            riskValues == [:]
        }
        get("/domains/$newDomainId/scenarios/$scenarioId").body.riskValues == [:]
        get("/scopes/$scopeId/risks/$scenarioId").body.domains[newDomainId].riskDefinitions == [:]
    }

    def "overwrites vanilla risk definition"() {
        expect:
        get("/domains/$oldDomainId").body.riskDefinitions.definitelyRisky.riskValues*.symbolicRisk == ["one"]

        when:
        def newDomainId = createNewTemplateAndMigrate {
            it.templateVersion = "1.1.0"
            it.riskDefinitions.definitelyRisky.riskValues.add([symbolicRisk: "two"])
        }

        then: "the changes from the new domain template have been applied"
        with(get("/domains/$newDomainId").body.riskDefinitions.definitelyRisky) {
            riskValues*.symbolicRisk ==~ ["one", "two"]
        }
    }

    def "customized risk definition is not overwritten"() {
        given: "a customized domain with an added translation"
        get("/domains/$oldDomainId").body.riskDefinitions.definitelyRisky.with { riskDef ->
            riskDef.riskValues[0].translations = [
                en: [
                    name: "customized risk"
                ]
            ]
            put("/content-customizing/domains/$owner.oldDomainId/risk-definitions/definitelyRisky", riskDef, null)
        }

        when: "migrating to a new template version with an added risk value"
        def newDomainId = createNewTemplateAndMigrate {
            it.templateVersion = "1.1.0"
            it.riskDefinitions.definitelyRisky.riskValues.add([symbolicRisk: "two"])
        }

        then: "the customized version has been applied"
        with(get("/domains/$newDomainId").body.riskDefinitions.definitelyRisky) {
            riskValues*.symbolicRisk ==~ ["one"]
            riskValues[0].translations.en.name == "customized risk"
        }
    }

    def "invalid customized risk definition is migrated to new domain"() {
        given: "a customized domain where a risk definition uses the doomed link"
        get("/domains/$oldDomainId").body.riskDefinitions.definitelyRisky.with { riskDef ->
            riskDef.categories[0].id = "caterpillar"
            riskDef.impactInheritingLinks.scope = ['scopeToProcessLink']
            put("/content-creation/domains/$owner.oldDomainId/risk-definitions/definitelyRisky", riskDef, null)
        }

        when: "migrating to new template version with a removed link\""
        def newDomainId = createNewTemplateAndMigrate {
            it.templateVersion = "2.0.0"
            it.elementTypeDefinitions.scope.links.remove("scopeToProcessLink")
        }

        then: "the customized version has been applied, minus the invalid link"
        with(get("/domains/$newDomainId").body.riskDefinitions.definitelyRisky) {
            categories[0].id == "caterpillar"
            impactInheritingLinks == [:]
        }
    }

    private LinkedHashMap<String, Serializable> getTemplate() {
        [
            id: UUID.randomUUID(),
            name: templateName,
            templateVersion: "1.0.0",
            authority: "jj",
            catalogItems: [
                [
                    id: UUID.randomUUID(),
                    name: "c1",
                    elementType: "asset",
                    subType: "AST_Application",
                    status: "NEW",
                ]
            ],
            elementTypeDefinitions: [
                'asset': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'AST_Application': [
                            'statuses': [
                                'NEW',
                            ]
                        ]
                    ]
                ],
                'control': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'CTL_TOM': [
                            'statuses': [
                                'NEW',
                            ]
                        ]
                    ]
                ],
                'document': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'DOC_Document': [
                            'statuses': [
                                'NEW',
                            ]
                        ]
                    ]
                ],
                'incident': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'INC_Incident': [
                            'statuses': [
                                'NEW',
                            ]
                        ]
                    ]
                ],
                'person': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'PER_DataProtectionOfficer': [
                            'statuses': [
                                'NEW',
                            ]
                        ],
                    ]
                ],
                'process': [
                    'customAspects': [
                        'test1' :[
                            'attributeDefinitions': [
                                'Attribute1': ['type': 'boolean'],
                                'Attribute2': ['type': 'text']
                            ]],
                        'test2' :[
                            'attributeDefinitions': [
                                'Attribute1': ['type': 'boolean'],
                                'Attribute2': ['type': 'boolean']
                            ]
                        ],
                        'everlasting': [
                            attributeDefinitions: [
                                'perfectNumber': ['type': 'integer']
                            ],
                        ]
                    ],
                    'links': [
                        'processToScopeLink': [
                            'attributeDefinitions': [:],
                            'targetSubType': 'SCP_ResponsibleBody',
                            'targetType': 'scope'
                        ]
                    ],
                    'subTypes': [
                        'PRO_DataProcessing': [
                            'statuses': [
                                'NEW',
                            ]
                        ]
                    ]
                ],
                'scenario': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'SCN_Scenario': [
                            'statuses': [
                                'NEW',
                            ]
                        ]
                    ]
                ],
                'scope': [
                    'customAspects': [:],
                    'links': [
                        'scopeToProcessLink': [
                            attributeDefinitions: [:],
                            targetSubType: 'PRO_DataProcessing',
                            targetType: 'process',
                        ]
                    ],
                    'subTypes': [
                        'SCP_ResponsibleBody': [
                            'statuses': [
                                'NEW',
                            ]
                        ]
                    ]
                ]
            ],
            decisions: [
                riskAnalyzed: [
                    name: [en: "Risk analysed?"],
                    elementType: "process",
                    elementSubType: "PRO_DataProcessing",
                    rules: [
                        [
                            description: [en: "no risk values present"],
                            conditions: [
                                [
                                    inputProvider: [
                                        type: "maxRisk"
                                    ],
                                    inputMatcher: [
                                        type: "isNull"
                                    ]
                                ]
                            ],
                            output: false
                        ]
                    ],
                    defaultResultValue: true
                ]
            ],
            riskDefinitions: [
                definitelyRisky: [
                    id: "definitelyRisky",
                    riskValues: [[symbolicRisk: "one"]],
                    probability: [levels: [[:]]],
                    categories: [
                        [
                            id: "nyanCat",
                            potentialImpacts: [[:]],
                            valueMatrix: [[[symbolicRisk: "one"]]]
                        ]
                    ],
                    riskMethod: [:]
                ]
            ]
        ]
    }

    String createNewTemplateAndMigrate(Consumer<Map> updateTemplate) {
        def template = get("/content-creation/domain-templates/$currentDomainTemplateId").body
        updateTemplate(template)
        currentDomainTemplateId = post("/content-creation/domain-templates", template, 201, CONTENT_CREATOR).body.resourceId
        post("/domain-templates/$currentDomainTemplateId/createdomains", null, 204, ADMIN)
        post("/admin/domain-templates/$currentDomainTemplateId/allclientsupdate", null, 204, ADMIN)
        defaultPolling.eventually {
            def versions = get("/domains").body.findAll { it.name == templateName }*.templateVersion
            versions == [template.templateVersion]
        }
        get("/domains").body.find{it.name == templateName}.id
    }

    private List<Map> migrationDefinitionChangeKey() {
        def m = [
            [description : [en: "a key change and and a transfer of value"],
                id: "t1",
                oldDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "process",
                        customAspect: "test1",
                        attribute: "Attribute1"
                    ],
                    [
                        type: "customAspectAttribute",
                        elementType: "process",
                        customAspect: "test1",
                        attribute: "Attribute2"
                    ]
                ],
                newDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "process",
                        customAspect: "test1",
                        attribute: "Attribute3",
                        migrationExpression: [
                            type : 'customAspectAttributeValue',
                            customAspect: 'test1',
                            attribute: 'Attribute1'
                        ]
                    ],
                    [
                        type: "customAspectAttribute",
                        elementType: "process",
                        customAspect: "test2",
                        attribute: "Attribute3",
                        migrationExpression: [
                            type : 'customAspectAttributeValue',
                            customAspect: 'test1',
                            attribute: 'Attribute2'
                        ]
                    ],
                ],

            ],
        ]
        return m
    }
}
