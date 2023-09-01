/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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
package org.veo.rest

import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.repository.ClientRepository

@WithUserDetails("admin")
class DomainTemplateControllerMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepository clientRepo

    def setup() {
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
    }

    def "list all domain templates"() {
        given:
        createTestDomainTemplate(TEST_DOMAIN_TEMPLATE_ID)

        when:
        def templates = parseJson(get("/domaintemplates"))

        then:
        templates.size() == 2
        with(templates.find {
            it.name == "DS-GVO"
        }) {
            id instanceof String
            templateVersion == "1.4.0"
            createdAt instanceof String
            _self.endsWith("/domaintemplates/$id")
        }
        with(templates.find {
            it.name == "test-domain"
        }) {
            id instanceof String
            templateVersion == "1.0.0"
            createdAt instanceof String
            _self.endsWith("/domaintemplates/$id")
        }
    }

    def "create DSGVO domain for a single client"() {
        given: "a client with some units and a document"
        def client1 = clientRepo.save(newClient {})
        def client2 = clientRepo.save(newClient {})

        when: "creating the DSGVO domain for the client"
        post("/domaintemplates/$DSGVO_DOMAINTEMPLATE_UUID/createdomains?clientids=${client1.id.uuidValue()}", [:], HttpStatus.SC_NO_CONTENT)
        client1 = loadClientAndInitializeDomains(client1.id)

        then: "the client gets the new domain"
        with(client1.domains) {
            size() == 1
            first().name == 'DS-GVO'
        }

        and: "the other client is not affected"
        with(client2.domains) {
            size() == 0
        }
    }

    def "create DSGVO domain for multiple clients"() {
        given: "a client with some units and a document"
        def client1 = clientRepo.save(newClient {})
        def client2 = clientRepo.save(newClient {})
        def client3 = clientRepo.save(newClient {})

        when: "creating the DSGVO domain for the client"
        post("/domaintemplates/$DSGVO_DOMAINTEMPLATE_UUID/createdomains?clientids=${client1.id.uuidValue()},${client2.id.uuidValue()}", [:], HttpStatus.SC_NO_CONTENT)
        client1 = loadClientAndInitializeDomains(client1.id)
        client2 = loadClientAndInitializeDomains(client2.id)
        client3 = loadClientAndInitializeDomains(client3.id)

        then: "the clients get the new domain"
        with(client1.domains) {
            size() == 1
            first().name == 'DS-GVO'
        }
        with(client2.domains) {
            size() == 1
            first().name == 'DS-GVO'
        }

        and: "the other client is not affected"
        with(client3.domains) {
            size() == 0
        }
    }

    def "create DSGVO domain for all clients"() {
        given: "a client with some units and a document"
        def client1 = clientRepo.save(newClient {})
        def client2 = clientRepo.save(newClient {})
        def client3 = clientRepo.save(newClient {})

        when: "creating the DSGVO domain for the client"
        post("/domaintemplates/$DSGVO_DOMAINTEMPLATE_UUID/createdomains", [:], HttpStatus.SC_NO_CONTENT)
        client1 = loadClientAndInitializeDomains(client1.id)
        client2 = loadClientAndInitializeDomains(client2.id)
        client3 = loadClientAndInitializeDomains(client3.id)

        then: "the clients get the new domain"
        client1.domains.size() == 1
        client2.domains.size() == 1
        client3.domains.size() == 1
    }

    @WithUserDetails("content-creator")
    def "import domain template exported from system with different context path"() {
        when:
        post("/domaintemplates", getTemplateBody())

        then:
        noExceptionThrown()
    }

    Client loadClientAndInitializeDomains(clientId) {
        txTemplate.execute{
            clientRepo.findById(clientId).get().tap {
                //initialize lazy associations
                domains.each {
                    it.name
                }
            }
        }
    }

    private Map getTemplateBody() {
        // Template ID should actually be generated by the application. This ID is only needed to create a unique name
        // for this test run and have some ID to use in URIs.
        def randomUuid = UUID.randomUUID().toString()
        return [
            'name': "Import test template $randomUuid",
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
                    'namespace': 'VT.p-1',
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
                    'namespace': 'TOM.c-1',
                    'tailoringReferences': [
                        [
                            'catalogItem': [
                                'targetUri': 'https://somehost.example/superveo/catalogitems/f55a860f-3bf0-4f63-9c8c-1c2a82762e40'
                            ],
                            'linkType': 'process_tom',
                            'referenceType': 'LINK_EXTERNAL']
                    ]]
            ],
            profiles:[
                example: [
                    "elements": [
                        [
                            "name": "Data GmbH",
                            "designator": "DMO-63",
                            "description": "Die Data GmbH ist ein IT-Dienstleister, spezialisiert auf Netzwerksicherheit und Informationssicherheit und in unseren Beispieldaten die Verantwortliche Stelle nach DS-GVO.",
                            "owner": [
                                "targetUri": "https://somehost.example/superveo/units/6e6dd1c2-111f-4ad2-9bc5-c2f6df8eaa5d"
                            ],
                            "links": [
                                "scope_informationSecurityOfficer": [
                                    [
                                        "domains": [],
                                        "attributes": [:],
                                        "target": [
                                            "targetUri": "https://somehost.example/superveo/persons/69ef9c7e-7306-404e-8907-a69e2dd02e07"
                                        ]
                                    ]
                                ]
                            ]
                        ]
                    ]
                ]
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
                            'targetSubType': 'PER_DataProtectionOfficer',
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
                            ]]]]
            ]
        ]
    }
}
