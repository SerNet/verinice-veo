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

class DomainTemplateImportRestTest extends VeoRestTest {

    def setup() {
        postNewUnit("some unit to init rest test client")
    }

    def "import domain template"() {
        given: "a test template body"
        def template = getTemplateBody()

        when: "importing the domain template"
        def templateId = post("/domaintemplates", template, 201, UserType.CONTENT_CREATOR).body.resourceId

        and: "creating and fetching a new domain based on the template"
        post("/domaintemplates/$templateId/createdomains", null, 204, UserType.ADMIN)
        def domain = get("/domains").body.find { it.name == template.name }

        then: "the domain contains metadata from the template"
        domain.abbreviation == template.abbreviation
        domain.authority == template.authority
        domain.revision == template.revision
        domain.templateVersion == template.templateVersion

        and: "the domain contains the risk definition"
        with(domain.riskDefinitions.RD1) {
            it.probability.levels[1].name == "often"
            it.implementationStateDefinition.levels[0].name == "yes"
            it.riskValues[2].symbolicRisk == 'r-3'
            it.categories[0].valueMatrix[0][1].name == "medium"
        }

        when: "fetching the process schema in the new domain"
        def processSchema = get("/schemas/process?domains=${domain.id}").body

        then: "it contains a type definition from our template"
        processSchema.properties.customAspects.properties.process_processing.properties.attributes.properties.process_processing_asProcessor.type == "boolean"

        when: "fetching the catalog"
        String catalogUri = domain.catalogs.first().targetUri
        def catalog = get(catalogUri).body

        then: "it has the correct name"
        catalog.name == "ITT Controls"

        when: "fetching the catalog items"
        def catalogItems = catalog.catalogItems.collect {
            get(it.targetUri).body
        }

        then: "they are connected by a tailoring reference"
        catalogItems*.namespace.sort() == ["TOM.c-1", "VT.p-1"]
        def tom = catalogItems.find { it.namespace == "TOM.c-1" }
        def vt = catalogItems.find { it.namespace == "VT.p-1" }
        tom.tailoringReferences.first().catalogItem.targetUri == vt._self

        when: "fetching the catalog item elements"
        def catalogItemElements = catalogItems.collect {
            get(it.element.targetUri).body
        }

        then: "their names are set"
        catalogItemElements*.name.sort() == ["Control-1", "Test process-1"]

        expect: "updating to fail"
        post("/domaintemplates", template, 409)
    }

    def "cannot import template with identical name & version twice"() {
        given: "a unique template name for this test run"
        def name = "import test template ${UUID.randomUUID()}"

        expect: "posting different version numbers to succeed"
        post("/domaintemplates", [
            name: name,
            templateVersion: "2.0",
            revision: "finest",
            authority: "me"
        ], 201, UserType.CONTENT_CREATOR)
        post("/domaintemplates", [
            name: name,
            templateVersion: "2.1",
            revision: "finest",
            authority: "me"
        ], 201, UserType.CONTENT_CREATOR)

        and: "posting a previous version number again to cause a conflict"
        post("/domaintemplates/", [
            name: name,
            templateVersion: "2.1",
            revision: "finest",
            authority: "me"
        ], 409, UserType.CONTENT_CREATOR)
    }

    def "cannot import template with invalid catalog item attribute"() {
        given: "a template with an invalid catalog item attribute"
        var template = getTemplateBody()
        def vtElement = template.catalogs[0].catalogItems.find { it.namespace == "VT.p-1" }.element
        vtElement.customAspects.process_accessAuthorization = [
            attributes: [
                process_accessAuthorization_description: 1
            ]
        ]

        when: "trying to create the template"
        def response = post("/domaintemplates", template, 400, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message.endsWith("Invalid value for attribute 'process_accessAuthorization_description': \$: integer found, string expected")
    }

    def "cannot import template with invalid catalog link"() {
        given: "a template with an invalid catalog item attribute"
        var template = getTemplateBody()
        def vtItem = template.catalogs[0].catalogItems.find { it.namespace == "VT.p-1" }
        vtItem.tailoringReferences.add(
                [
                    catalogItem: [targetUri: '/catalogitems/f55a860f-3bf0-4f63-9c8c-1c2a82762e40'],
                    linkType: 'process_manager',
                    referenceType: 'LINK'
                ]
                )

        when: "trying to create the template"
        def response = post("/domaintemplates", template, 400, UserType.CONTENT_CREATOR).body

        then: "it fails with a helpful message"
        response.message.endsWith("Invalid target type 'process' for link type 'process_manager'")
    }

    def "cannot import template with invalid catalog item risk definition"() {
        given: "a template with a catalog item using a non-existing risk definition"
        var template = getTemplateBody()
        def vtElement = template.catalogs[0].catalogItems.find{it.namespace == "VT.p-1"}.element
        vtElement.domains = [
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
        def response = post("/domaintemplates", template, 400, UserType.CONTENT_CREATOR).body

        then: "it fails"
        response.message.contains("Undefined risk definition: RDX")
    }

    private Map getTemplateBody() {
        // Template ID should actually be generated by the application. This ID is only needed to create a unique name
        // for this test run and have some ID to use in URIs.
        def randomUuid = UUID.randomUUID().toString()
        return [
            'name': "Import test template $randomUuid",
            'revision': 'latest',
            'templateVersion': '1.0',
            'abbreviation': 'ITT',
            'authority': 'JJ',
            'catalogs': [
                [
                    'catalogItems': [
                        [
                            'catalog': [
                                'targetUri': '/catalogs/fb70bd43-7da7-4df1-b378-020ace491443'],
                            'element': [
                                'customAspects': [:],
                                'id': '1b55d69a-977b-49b6-93d8-247d1a064126',
                                'domains': [
                                    (randomUuid): [
                                        subType: 'PRO_DataTransfer',
                                        status: 'NEW'
                                    ]
                                ],
                                'links': [:],
                                'name': 'Test process-1',
                                'owner': [
                                    'targetUri': '/catalogitems/f55a860f-3bf0-4f63-9c8c-1c2a82762e40'
                                ],
                                'parts': [],
                                'type': 'process'],
                            'id': 'f55a860f-3bf0-4f63-9c8c-1c2a82762e40',
                            'namespace': 'VT.p-1',
                            'tailoringReferences': []],
                        [
                            'catalog': [
                                'targetUri': '/catalogs/fb70bd43-7da7-4df1-b378-020ace491443'],
                            'element': [
                                'abbreviation': 'c-1',
                                'customAspects': [:],
                                'description': 'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat.',
                                'domains': [
                                    (randomUuid): [
                                        subType: 'CTL_TOM',
                                        status: 'IN_PROGRESS'
                                    ]
                                ],
                                'id': '37ea81df-fcd9-46b2-a148-bbcb9298669d',
                                'links': [:],
                                'name': 'Control-1',
                                'owner': [
                                    'targetUri': '/catalogitems/dc46afdd-c957-4957-99da-f0a5f32dc457'],
                                'parts': [],
                                'type': 'control'],
                            'id': 'dc46afdd-c957-4957-99da-f0a5f32dc457',
                            'namespace': 'TOM.c-1',
                            'tailoringReferences': [
                                [
                                    'catalogItem': [
                                        'targetUri': '/catalogitems/f55a860f-3bf0-4f63-9c8c-1c2a82762e40   '],
                                    'linkType': 'process_tom',
                                    'referenceType': 'LINK_EXTERNAL']
                            ]]
                    ],
                    'domainTemplate': [
                        'targetUri': "/domaintemplates/$randomUuid"],
                    'id': 'fb70bd43-7da7-4df1-b378-020ace491443',
                    'name': 'ITT Controls']
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
                            'attributeSchemas': [
                                'process_accessAuthorization_concept': [
                                    'type': 'boolean'],
                                'process_accessAuthorization_description': [
                                    'type': 'string'],
                                'process_accessAuthorization_document': [
                                    'format': 'uri',
                                    'pattern': '^(https?|ftp)://',
                                    'type': 'string']]],
                        'process_processing': [
                            'attributeSchemas': [
                                'process_processing_asProcessor': [
                                    'type': 'boolean']]]],
                    'links': [
                        'process_tom': [
                            'attributeSchemas': [:],
                            'targetSubType': 'CTL_TOM',
                            'targetType': 'control'
                        ],
                        'process_manager': [
                            'attributeSchemas': [:],
                            'targetType': 'person'
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
            'riskDefinitions': [
                'RD1': [
                    'id': 'RD1',
                    'probability': [
                        'id': 'whatever',
                        'name': 'whatever',
                        'levels': [
                            [
                                'ordinalValue': 0,
                                'name': 'selten',
                                'abbreviation': '1',
                                'description': "Almost never",
                                'htmlColor': '#004643'
                            ],
                            [
                                'ordinalValue': 1,
                                'name': 'often',
                                'abbreviation': '2',
                                'description': 'All the time',
                                'htmlColor': '#004643'
                            ],
                        ],
                    ],
                    'implementationStateDefinition': [
                        'id': 'whatever',
                        'name': 'whatever',
                        'levels': [
                            [
                                'ordinalValue': 0,
                                'name': 'yes',
                                'abbreviation': 'Y',
                                'description': 'Implemented',
                                'htmlColor': '#12AE0F'
                            ],
                            [
                                'ordinalValue': 1,
                                'name': 'no',
                                'abbreviation': 'N',
                                'description': 'Not implemented',
                                'htmlColor': '#AE0D11'
                            ],
                        ]
                    ],
                    'categories': [
                        [
                            'id': 'C',
                            'name': 'Vertraulichkeit',
                            'abbreviation': 'c',
                            'description': '',
                            'valueMatrix': [
                                [
                                    [
                                        'ordinalValue': 0,
                                        'name': 'low',
                                        'htmlColor': '#A0CF11',
                                        'symbolicRisk': 'r-1'
                                    ],
                                    [
                                        'ordinalValue': 1,
                                        'name': 'medium',
                                        'htmlColor': '#A0CF11',
                                        'symbolicRisk': 'r-2'
                                    ],
                                ],
                                [
                                    [
                                        'ordinalValue': 0,
                                        'name': 'medium',
                                        'htmlColor': '#A0CF11',
                                        'symbolicRisk': 'r-2'
                                    ],
                                    [
                                        'ordinalValue': 1,
                                        'name': 'high',
                                        'htmlColor': '#A0CF11',
                                        'symbolicRisk': 'r-3'
                                    ],
                                ],
                            ],
                            'potentialImpacts': [
                                [
                                    'ordinalValue': 0,
                                    'name': 'peanuts',
                                    'abbreviation': 'p',
                                    'description': 'Not a big deal',
                                    'htmlColor': '#004643'
                                ],
                                [
                                    'ordinalValue': 1,
                                    'name': 'devastating',
                                    'abbreviation': 'd',
                                    'description': 'Just terrible',
                                    'htmlColor': '#004643'
                                ]
                            ]
                        ],
                    ],
                    'riskValues': [
                        [
                            'symbolicRisk': 'r-1',
                            'ordinalValue': 0,
                            'name': 'low',
                            'abbreviation': 'l',
                            'description': 'Pretty low',
                            'htmlColor': '#A0CF11',
                        ],
                        [
                            'symbolicRisk': 'r-2',
                            'ordinalValue': 1,
                            'name': 'medium',
                            'abbreviation': 'm',
                            'description': 'Kind of medium',
                            'htmlColor': '#FFFF13',
                        ],
                        [
                            'symbolicRisk': 'r-3',
                            'ordinalValue': 1,
                            'name': 'high',
                            'abbreviation': 'h',
                            'description': 'Pretty high',
                            'htmlColor': '#FFFF13',
                        ],
                    ]
                ],
            ],
        ]
    }
}