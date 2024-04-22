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

class DomainUpdateRestTest extends VeoRestTest {

    String oldDomainTemplateId
    String newDomainTemplateId
    String templateName
    String unitId
    String oldDomainId

    def setup() {
        templateName = "domain update test template ${UUID.randomUUID()}"

        def template = getTemplate()
        oldDomainTemplateId = post("/content-creation/domain-templates", template, 201, CONTENT_CREATOR).body.resourceId

        template.templateVersion = "1.1.0"
        newDomainTemplateId = post("/content-creation/domain-templates", template, 201, CONTENT_CREATOR).body.resourceId

        post("/domain-templates/$oldDomainTemplateId/createdomains", null, 204, ADMIN)
        unitId = postNewUnit().resourceId
        oldDomainId = get("/domains").body.find { it.name == templateName }.id
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

        when: "migrating to the new domain"
        def newDomainId = migrateToNewDomain()
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
        def newDomainId = migrateToNewDomain()

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

    private LinkedHashMap<String, Serializable> getTemplate() {
        [
            id: UUID.randomUUID(),
            name: templateName,
            templateVersion: "1.0.0",
            authority: "jj",
            catalogItems: [],
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
                    'customAspects': [:],
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
            ]
        ]
    }

    String migrateToNewDomain() {
        post("/domain-templates/$newDomainTemplateId/createdomains", null, 204, ADMIN)
        post("/admin/domain-templates/$newDomainTemplateId/allclientsupdate", null, 204, ADMIN)
        defaultPolling.eventually {
            get("/domains").body.findAll{it.name == templateName}*.templateVersion == ["1.1.0"]
        }
        get("/domains").body.findAll{it.name == templateName}.first().id
    }
}
