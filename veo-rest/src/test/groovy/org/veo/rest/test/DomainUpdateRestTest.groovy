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

    def setup() {
        templateName = "domain update test template ${UUID.randomUUID()}"

        def template = getTemplate()
        oldDomainTemplateId = post("/domaintemplates", template, 201, CONTENT_CREATOR).body.resourceId

        template.templateVersion = "1.1.0"
        newDomainTemplateId = post("/domaintemplates", template, 201, CONTENT_CREATOR).body.resourceId
    }

    def "updates client to new domain template version and migrates elements"() {
        given:
        post("/domaintemplates/$oldDomainTemplateId/createdomains", null, 204, ADMIN)
        def oldDomainId = get("/domains").body.find { it.name == templateName }.id
        def unitId = postNewUnit("you knit").resourceId
        def scopeId = post("/scopes", [
            name: "target scope",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (oldDomainId): [
                    subType: "SCP_ResponsibleBody",
                    status: "NEW"
                ]
            ]
        ]).body.resourceId

        when: "incarnating the new domain template version and migrating to the new domain"
        post("/domaintemplates/$newDomainTemplateId/createdomains", null, 204, ADMIN)
        post("/admin/domaintemplates/$newDomainTemplateId/allclientsupdate", null, 204, ADMIN)
        def domains = get("/domains").body.findAll{it.name == templateName}

        then: "only the new domain is active"
        domains*.templateVersion == ["1.1.0"]

        when: "fetching the migrated scope"
        def migratedScope = get("/scopes/$scopeId").body
        def newDomainId = domains.first().id

        then: "the sub type is still present under the new domain"
        migratedScope.domains.keySet() =~ [newDomainId]
        migratedScope.domains[newDomainId].subType == "SCP_ResponsibleBody"
    }

    private LinkedHashMap<String, Serializable> getTemplate() {
        [
            name: templateName,
            templateVersion: "1.0.0",
            revision: "whatever",
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
                    'links': [:],
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
                    'links': [:],
                    'subTypes': [
                        'SCP_ResponsibleBody': [
                            'statuses': [
                                'NEW',
                            ]
                        ]
                    ]
                ]
            ],
        ]
    }
}
