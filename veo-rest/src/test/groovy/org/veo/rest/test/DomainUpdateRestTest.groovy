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

import spock.util.concurrent.PollingConditions

class DomainUpdateRestTest extends VeoRestTest {

    String oldDomainTemplateId
    String newDomainTemplateId
    String templateName
    String unitId

    def setup() {
        unitId = postNewUnit("you knit").resourceId
        templateName = "domain update test template ${UUID.randomUUID()}"

        def template = getTemplate()
        oldDomainTemplateId = post("/domaintemplates", template, 201, CONTENT_CREATOR).body.resourceId

        template.templateVersion = "1.1.0"
        newDomainTemplateId = post("/domaintemplates", template, 201, CONTENT_CREATOR).body.resourceId
    }

    def "updates client to new domain template version and migrates elements"() {
        given: "a scope and a process linked to it in the old domain"
        post("/domaintemplates/$oldDomainTemplateId/createdomains", null, 204, ADMIN)
        def oldDomainId = get("/domains").body.find { it.name == templateName }.id
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

        when: "incarnating the new domain template version"
        post("/domaintemplates/$newDomainTemplateId/createdomains", null, 204, ADMIN)

        and: "triggering migration to the new domain"
        post("/admin/domaintemplates/$newDomainTemplateId/allclientsupdate", null, 204, ADMIN)

        then: "only the new domain is active"
        new PollingConditions().within(5, {
            get("/domains").body.findAll{it.name == templateName}*.templateVersion == ["1.1.0"]
        })

        when: "fetching the migrated scope"
        def migratedScope = get("/scopes/$scopeId").body
        def newDomainId = get("/domains").body.findAll{it.name == templateName}.first().id

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

    private LinkedHashMap<String, Serializable> getTemplate() {
        [
            name: templateName,
            templateVersion: "1.0.0",
            authority: "jj",
            catalogs: [],
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
}
