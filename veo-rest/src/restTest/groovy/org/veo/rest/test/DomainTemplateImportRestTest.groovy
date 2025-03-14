/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import spock.lang.Ignore

class DomainTemplateImportRestTest extends VeoRestTest {

    def "import domain template"() {
        given: "a test template body"
        def template = getTemplateBody()

        when: "importing the domain template"
        def templateId = post("/content-creation/domain-templates", template, 201, UserType.CONTENT_CREATOR).body.resourceId

        and: "creating and fetching a new domain based on the template"
        post("/domain-templates/$templateId/createdomains?restrictToClientsWithExistingDomain=false", null, 204, UserType.ADMIN)
        def domain = get("/domains").body.find { it.name == template.name }

        then: "the domain contains metadata from the template"
        domain.abbreviation == template.abbreviation
        domain.authority == template.authority
        domain.templateVersion == template.templateVersion

        and: "the domain contains the decision and inspection"
        domain.decisions.negativeDecision.elementSubType == "AST_Application"
        get("/domains/${domain.id}/inspections/conceptWithoutDescription").body.elementType == "process"

        and: "the domain contains the risk definition"
        with(domain.riskDefinitions.RD1) {
            it.probability.levels[1].translations.en.name == "often"
            it.implementationStateDefinition.levels[0].translations.en.name == "yes"
            it.riskValues[2].symbolicRisk == 'r-3'
            it.categories[0].valueMatrix[0][1].translations.en.name == "medium"
        }

        when: "fetching the process schema in the new domain"
        def processSchema = get("/schemas/process?domains=${domain.id}").body

        then: "it contains a type definition from our template"
        processSchema.properties.customAspects.properties.process_processing.properties.attributes.properties.process_processing_asProcessor.type == "boolean"

        and: "catalog items are present"
        getCatalogItems(domain.id)*.name ==~ ["Control-1", "Test process-1"]

        expect: "updating to fail"
        post("/content-creation/domain-templates", template, 409)
    }

    def "import domain template with content type multipart"() {
        given: "a test template body"
        def template = getTemplateBody()

        when: "importing the domain template with content type multipart"
        def templateId = postMultipart("/content-creation/domain-templates", template, 201, UserType.CONTENT_CREATOR).body.resourceId

        and: "creating and fetching a new domain based on the template"
        post("/domain-templates/$templateId/createdomains?restrictToClientsWithExistingDomain=false", null, 204, UserType.ADMIN)
        def domain = get("/domains").body.find { it.name == template.name }

        then: "the domain contains metadata from the template"
        domain.abbreviation == template.abbreviation
        domain.authority == template.authority
        domain.templateVersion == template.templateVersion
    }

    def "cannot import template with identical name & version twice"() {
        given: "a unique template name for this test run"
        def name = "import test template ${UUID.randomUUID()}"

        expect: "posting different version numbers to succeed"
        post("/content-creation/domain-templates", [
            id: UUID.randomUUID(),
            name: name,
            templateVersion: "2.0.0",
            authority: "me"
        ], 201, UserType.CONTENT_CREATOR)
        post("/content-creation/domain-templates", [
            id: UUID.randomUUID(),
            name: name,
            templateVersion: "2.1.0",
            authority: "me"
        ], 201, UserType.CONTENT_CREATOR)

        and: "posting a previous version number again to cause a conflict"
        post("/content-creation/domain-templates", [
            id: UUID.randomUUID(),
            name: name,
            templateVersion: "2.1.0",
            authority: "me"
        ], 409, UserType.CONTENT_CREATOR)
    }

    def "cannot import template with invalid version"() {
        given: "a unique template name for this test run"
        def name = "import test template ${UUID.randomUUID()}"

        expect: "posting a valid version number to succeed"
        post("/content-creation/domain-templates", [
            id: UUID.randomUUID(),
            name: name,
            templateVersion: "1.0.0",
            authority: "me"
        ], 201, UserType.CONTENT_CREATOR)

        and: "posting an invalid version to fail"
        post("/content-creation/domain-templates", [
            id: UUID.randomUUID(),
            name: name,
            templateVersion: "1.1",
            authority: "me"
        ], 400, UserType.CONTENT_CREATOR)
    }

    def "cannot import template with invalid sub type in decision"() {
        given: "a template with an invalid sub type in a decision"
        var template = getTemplateBody()
        template.decisions.negativeDecision.elementSubType = "sillySub"

        when: "trying to create the template"
        def response = post("/content-creation/domain-templates", template, 422, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message == "Validation error in decision 'negativeDecision': Sub type sillySub is not defined, availabe sub types: [AST_Application]"
    }

    def "cannot import template with invalid expression in inspection"() {
        given: "a template where an inspection uses a string attribute in an AND expression"
        var template = getTemplateBody()
        template.inspections.conceptWithoutDescription.condition.operands[0].attribute = "process_accessAuthorization_document"

        when: "trying to create the template"
        def response = post("/content-creation/domain-templates", template, 422, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message == "Validation error in inspection 'conceptWithoutDescription': Only boolean values can be used in an AND expression"
    }

    def "cannot import template with invalid catalog item attribute"() {
        given: "a template with an invalid catalog item attribute"
        var template = getTemplateBody()
        def vtElement = template.catalogItems.find { it.name == "Test process-1" }
        vtElement.customAspects.process_accessAuthorization = [
            process_accessAuthorization_description: 1
        ]

        when: "trying to create the template"
        def response = post("/content-creation/domain-templates", template, 400, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message.endsWith("Invalid value '1' for attribute 'process_accessAuthorization_description': must be a string")

        when: "trying to create the template with content type multipart"
        response = postMultipart("/content-creation/domain-templates", template, 400, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message.endsWith("Invalid value '1' for attribute 'process_accessAuthorization_description': must be a string")
    }

    def "cannot import template with invalid catalog link"() {
        given: "a template with an invalid catalog item attribute"
        var template = getTemplateBody()
        def vtItem = template.catalogItems.find { it.name == "Test process-1" }
        vtItem.tailoringReferences.add(
                [
                    target: [targetUri: "/domains/$template.id/catalog-items/f55a860f-3bf0-4f63-9c8c-1c2a82762e40"],
                    linkType: 'process_manager',
                    referenceType: 'LINK'
                ]
                )

        when: "trying to create the template"
        def response = post("/content-creation/domain-templates", template, 400, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message.endsWith("Invalid target type 'process' for link type 'process_manager'")
    }

    def "cannot import template with missing catalog item ID"() {
        given:
        var template = getTemplateBody()
        def vtItem = template.catalogItems.find { it.name == "Test process-1" }
        vtItem.id = null

        when: "trying to create the template"
        def response = post("/content-creation/domain-templates", template, 400, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message.endsWith("Missing symbolic ID for catalog-item in domain-template $template.id")
    }

    def "cannot import template with duplicate catalog item ID"() {
        given:
        var template = getTemplateBody()
        def item1 = template.catalogItems.find { it.name == "Test process-1" }
        def item2 = template.catalogItems.find { it.name == "Control-1" }
        item2.id = item1.id

        when: "trying to create the template"
        def response = post("/content-creation/domain-templates", template, 422, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message.endsWith("Duplicate key: catalog-item $item1.id in domain-template $template.id")
    }

    def "cannot import template with invalid catalog item sub type"() {
        given: "a template with an invalid sub type"
        var template = getTemplateBody()
        def vtElement = template.catalogItems.find { it.name == "Test process-1" }
        vtElement.subType = "PRO_fit"

        when: "trying to create the template"
        def response = post("/content-creation/domain-templates", template, 400, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message.endsWith("Sub type 'PRO_fit' is not defined for element type process")
    }

    def "cannot import template with sub-type-less catalog item"() {
        given: "a template with a catalog item that has no sup type"
        var template = getTemplateBody()
        def vtItem = template.catalogItems.find { it.name == "Test process-1" }
        vtItem.subType = null

        when: "trying to create the template"
        def response = post("/content-creation/domain-templates", template, 400, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message.endsWith("Cannot assign element to domain without specifying a sub type")
    }

    def "cannot import template where profile item has invalid risk values"() {
        given: "a template with a scenario profile item that uses an undefined probability level"
        var template = getTemplateBody()
        template.profiles_v2 = [
            [
                id: UUID.randomUUID(),
                items: [
                    [
                        id: UUID.randomUUID(),
                        name: "very likely",
                        elementType: "scenario",
                        subType: "SCN_Scenario",
                        status: "RELEASED",
                        aspects: [
                            scenarioRiskValues: [
                                RD1: [
                                    potentialProbability: 9001
                                ]
                            ]
                        ]
                    ]
                ]
            ]
        ]

        when: "trying to create the template"
        def response = post("/content-creation/domain-templates", template, 422, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message.endsWith("Risk definition RD1 contains no probability with ordinal value 9001")
    }

    @Ignore
    def "cannot import template with invalid catalog item risk definition"() {
        given: "a template with a catalog item using a non-existing risk definition"
        var template = getTemplateBody()
        def vtItem = template.catalogItems.find{it.name == "Test process-1"}
        vtItem.domains = [
            (UUID.randomUUID()): [
                riskValues: [
                    RDX: [
                        potentialImpacts: [
                            C: 2
                        ]
                    ]
                ]
            ]
        ]

        when: "trying to create the template"
        def response = post("/content-creation/domain-templates", template, 422, UserType.CONTENT_CREATOR).body

        then: "it fails"
        response.message.contains("Domain $template.name 1.0.0 contains no risk definition with ID RDX")
    }

    def "cannot import template with invalid translation in the risk definition"() {
        given: "a template with a catalog item using a non-existing risk definition"
        var template = getTemplateBody()
        template.riskDefinitions['RD1'].riskMethod.translations.de.remove('description')

        when: "trying to create the template"
        def response = post("/content-creation/domain-templates", template, 422, UserType.CONTENT_CREATOR).body

        then: "it fails"
        response.message.contains(" MISSING: riskDefinition RD1 risk method: description")
    }

    private Map getTemplateBody() {
        // Domain ID is temporary and should be replaced with an ID generated by the application. It is also used to
        // create a unique domain name here.
        def domainId = UUID.randomUUID().toString()
        return [
            'id': domainId,
            'name': "Import test template $domainId",
            'templateVersion': '1.0.0',
            'abbreviation': 'ITT',
            'authority': 'JJ',
            'catalogItems': [
                [
                    'customAspects': [:],
                    'subType': 'PRO_DataTransfer',
                    'status': 'NEW',
                    'name': 'Test process-1',
                    'elementType': 'process',
                    'id': 'f55a860f-3bf0-4f63-9c8c-1c2a82762e40',
                    'tailoringReferences': []],
                [
                    'abbreviation': 'c-1',
                    'customAspects': [:],
                    'description': 'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat.',
                    'name': 'Control-1',
                    'elementType': 'control',
                    'subType': 'CTL_TOM',
                    'status': 'IN_PROGRESS',
                    'id': 'dc46afdd-c957-4957-99da-f0a5f32dc457',
                    'tailoringReferences': [
                        [
                            'target': [
                                'targetUri': "https://somehost.example/superveo/domains/$domainId/catalog-items/f55a860f-3bf0-4f63-9c8c-1c2a82762e40"],
                            'linkType': 'process_tom',
                            'referenceType': 'LINK_EXTERNAL']
                    ]]
            ],
            'elementTypeDefinitions': [
                'asset': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'AST_Application': [
                            'statuses': [
                                'IN_PROGRESS',
                                'NEW',
                                'RELEASED',
                                'FOR_REVIEW',
                                'ARCHIVED'
                            ]]]],
                'control': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'CTL_TOM': [
                            'statuses': [
                                'IN_PROGRESS',
                                'NEW',
                                'RELEASED',
                                'FOR_REVIEW',
                                'ARCHIVED'
                            ]]]],
                'document': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'DOC_Document': [
                            'statuses': [
                                'IN_PROGRESS',
                                'NEW',
                                'RELEASED',
                                'FOR_REVIEW',
                                'ARCHIVED'
                            ]]]],
                'incident': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'INC_Incident': [
                            'statuses': [
                                'IN_PROGRESS',
                                'NEW',
                                'RELEASED',
                                'FOR_REVIEW',
                                'ARCHIVED'
                            ]]]],
                'person': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'PER_DataProtectionOfficer': [
                            'statuses': [
                                'IN_PROGRESS',
                                'NEW',
                                'RELEASED',
                                'FOR_REVIEW',
                                'ARCHIVED'
                            ]],
                        'PER_Person': [
                            'statuses': [
                                'IN_PROGRESS',
                                'NEW',
                                'RELEASED',
                                'FOR_REVIEW',
                                'ARCHIVED'
                            ]]]],
                'process': [
                    'customAspects': [
                        'process_accessAuthorization': [
                            'attributeDefinitions': [
                                'process_accessAuthorization_concept': [
                                    'type': 'boolean'],
                                'process_accessAuthorization_description': [
                                    'type': 'text'],
                                'process_accessAuthorization_document': [
                                    'type': 'externalDocument']]],
                        'process_processing': [
                            'attributeDefinitions': [
                                'process_processing_asProcessor': [
                                    'type': 'boolean']]]],
                    'links': [
                        'process_tom': [
                            'attributeDefinitions': [:],
                            'targetSubType': 'CTL_TOM',
                            'targetType': 'control'
                        ],
                        'process_manager': [
                            'attributeDefinitions': [:],
                            'targetType': 'person',
                            'targetSubType': 'PER_Person',
                        ],
                    ],
                    'subTypes': [
                        'PRO_DataProcessing': [
                            'statuses': [
                                'IN_PROGRESS',
                                'NEW',
                                'RELEASED',
                                'FOR_REVIEW',
                                'ARCHIVED'
                            ]],
                        'PRO_DataTransfer': [
                            'statuses': [
                                'IN_PROGRESS',
                                'NEW',
                                'RELEASED',
                                'FOR_REVIEW',
                                'ARCHIVED'
                            ]]]],
                'scenario': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'SCN_Scenario': [
                            'statuses': [
                                'IN_PROGRESS',
                                'NEW',
                                'RELEASED',
                                'FOR_REVIEW',
                                'ARCHIVED'
                            ]]]],
                'scope': [
                    'customAspects': [:],
                    'links': [:],
                    'subTypes': [
                        'SCP_Scope': [
                            'statuses': [
                                'IN_PROGRESS',
                                'NEW',
                                'RELEASED',
                                'FOR_REVIEW',
                                'ARCHIVED'
                            ]]]]],
            'decisions': [
                'negativeDecision': [
                    'name': ['en': 'silly asset decision that always returns false'],
                    'elementType': 'asset',
                    'elementSubType': 'AST_Application',
                    'rules': [],
                    'defaultResultValue': false
                ]
            ],
            'inspections': [
                'conceptWithoutDescription': [
                    'description': ['en': 'Each process with a concept should have a description'],
                    'severity': 'HINT',
                    'elementType': 'process',
                    'condition': [
                        'type': 'and',
                        'operands': [
                            [
                                'type': 'customAspectAttributeValue',
                                'customAspect': 'process_accessAuthorization',
                                'attribute': 'process_accessAuthorization_concept'
                            ],
                            [
                                'type': 'equals',
                                'left': [
                                    'type': 'customAspectAttributeValue',
                                    'customAspect': 'process_accessAuthorization',
                                    'attribute': 'process_accessAuthorization_description'
                                ],
                                'right': [
                                    'type': 'constant',
                                    'value': null
                                ]
                            ]
                        ]
                    ]
                ],
            ],
            'riskDefinitions': [
                'RD1': [
                    'id': 'RD1',
                    'riskMethod': [
                        'translations': [
                            'en': [
                                'description': 'description',
                                'impactMethod': 'highwatermark'
                            ],
                            'de': [
                                'description': 'Beschreibung',
                                'impactMethod': 'Hochwasser'
                            ]
                        ]
                    ],
                    'probability': [
                        'id': 'prob',
                        'translations': [
                            'en': [
                                'name': 'whatever',
                                'abbreviation': 'p',
                                'description': "the properbility",
                            ],
                        ],
                        'levels': [
                            [
                                'ordinalValue': 0,
                                'translations': [
                                    'en': [
                                        'name': 'selten',
                                        'abbreviation': '1',
                                        'description': "Almost never",
                                    ],
                                ],
                                'htmlColor': '#004643'
                            ],
                            [
                                'ordinalValue': 1,
                                'translations': [
                                    'en': [
                                        'name': 'often',
                                        'abbreviation': '2',
                                        'description': 'All the time',
                                    ],
                                ],
                                'htmlColor': '#004643'
                            ],
                        ],
                    ],
                    'implementationStateDefinition': [
                        'id': 'imp',
                        'translations': [
                            'en': [
                                'name': 'whatever',
                                'abbreviation': 'c',
                                'description': '',
                            ],
                        ],
                        'levels': [
                            [
                                'ordinalValue': 0,
                                'translations': [
                                    'en': [
                                        'name': 'yes',
                                        'abbreviation': 'Y',
                                        'description': 'Implemented',
                                    ],
                                ],
                                'htmlColor': '#12AE0F'
                            ],
                            [
                                'ordinalValue': 1,
                                'translations': [
                                    'en': [
                                        'name': 'no',
                                        'abbreviation': 'N',
                                        'description': 'Not implemented',
                                    ],
                                ],
                                'htmlColor': '#AE0D11'
                            ],
                        ]
                    ],
                    'categories': [
                        [
                            'id': 'C',
                            'translations': [
                                'en': [
                                    'name': 'Vertraulichkeit',
                                    'abbreviation': 'c',
                                    'description': '',
                                ],
                            ],
                            'valueMatrix': [
                                [
                                    [
                                        'ordinalValue': 0,
                                        'translations': [
                                            'en': [
                                                'name': 'low',
                                            ],
                                        ],
                                        'htmlColor': '#A0CF11',
                                        'symbolicRisk': 'r-1'
                                    ],
                                    [
                                        'ordinalValue': 1,
                                        'translations': [
                                            'en': [
                                                'name': 'medium',
                                            ],
                                        ],
                                        'htmlColor': '#A0CF11',
                                        'symbolicRisk': 'r-2'
                                    ],
                                ],
                                [
                                    [
                                        'ordinalValue': 0,
                                        'translations': [
                                            'en': [
                                                'name': 'medium',
                                            ],
                                        ],
                                        'htmlColor': '#A0CF11',
                                        'symbolicRisk': 'r-1'
                                    ],
                                    [
                                        'ordinalValue': 1,
                                        'translations': [
                                            'en': [
                                                'name': 'high',
                                            ],
                                        ],
                                        'htmlColor': '#A0CF11',
                                        'symbolicRisk': 'r-2'
                                    ],
                                ],
                            ],
                            'potentialImpacts': [
                                [
                                    'ordinalValue': 0,
                                    'translations': [
                                        'en': [
                                            'name': 'peanuts',
                                            'abbreviation': 'p',
                                            'description': 'Not a big deal',
                                        ],
                                    ],
                                    'htmlColor': '#004643'
                                ],
                                [
                                    'ordinalValue': 1,
                                    'translations': [
                                        'en': [
                                            'name': 'devastating',
                                            'abbreviation': 'd',
                                            'description': 'Just terrible',
                                        ],
                                    ],
                                    'htmlColor': '#004643'
                                ]
                            ]
                        ],
                    ],
                    'riskValues': [
                        [
                            'symbolicRisk': 'r-1',
                            'ordinalValue': 0,
                            'translations': [
                                'en': [
                                    'name': 'low',
                                    'abbreviation': 'l',
                                    'description': 'Pretty low',
                                ],
                            ],
                            'htmlColor': '#A0CF11',
                        ],
                        [
                            'symbolicRisk': 'r-2',
                            'ordinalValue': 1,
                            'translations': [
                                'en': [
                                    'name': 'medium',
                                    'abbreviation': 'm',
                                    'description': 'Kind of medium',
                                ],
                            ],
                            'htmlColor': '#FFFF13',
                        ],
                        [
                            'symbolicRisk': 'r-3',
                            'ordinalValue': 2,
                            'translations': [
                                'en': [
                                    'name': 'high',
                                    'abbreviation': 'h',
                                    'description': 'Pretty high',
                                ],
                            ],
                            'htmlColor': '#FFFF13',
                        ],
                    ]
                ],
            ],
        ]
    }
}