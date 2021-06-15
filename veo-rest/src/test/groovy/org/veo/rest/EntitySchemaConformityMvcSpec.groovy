/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.web.servlet.ResultActions

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion

import org.veo.core.VeoMvcSpec
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.DomainRepository
import org.veo.core.repository.UnitRepository
import org.veo.core.service.EntitySchemaService
import org.veo.rest.configuration.WebMvcSecurityConfiguration

/**
 * Tests if resources returned by the API conform to the entity schema.
 */
@WithUserDetails("user@domain.example")
class EntitySchemaConformityMvcSpec extends VeoMvcSpec {
    @Autowired
    EntitySchemaService entitySchemaService

    @Autowired
    UnitRepository unitRepository

    @Autowired
    ClientRepository clientRepository

    @Autowired
    DomainRepository domainRepository

    ObjectMapper om = new ObjectMapper()
    String domainId
    String unitId

    def setup() {
        def client = clientRepository.save(newClient {
            dbId = WebMvcSecurityConfiguration.TESTCLIENT_UUID
        })
        domainId = domainRepository.save(newDomain {
            owner = client
        }).id.uuidValue()
        unitId = unitRepository.save(newUnit(client)).dbId
    }

    def "created asset with custom aspect conforms to schema"() {
        given: "the asset schema and a newly created asset"
        def schema = getSchema("asset")
        def targetPersonId = parseJson(post("/persons", [
            name: "target",
            owner: [
                targetUri: "/units/"+unitId,
            ]])).resourceId
        def assetId = (String)parseJson(post("/assets", [
            customAspects: [
                asset_details: [
                    attributes: [
                        asset_details_operatingStage: "asset_details_operatingStage_operation"
                    ]
                ]
            ],
            name: "asset",
            owner: [
                targetUri: "/units/"+unitId,
            ]])).resourceId
        def createdAssetJson = parseNode(get("/assets/$assetId"))

        when: "validating the asset JSON"
        def validationMessages = schema.validate(createdAssetJson)

        then:
        validationMessages.empty
    }

    def "created control with custom aspect conforms to schema"() {
        given: "the control schema and a newly created control"
        def schema = getSchema("control")
        def controlId = (String)parseJson(post("/controls", [
            customAspects: [
                control_generalInformation: [
                    attributes: [
                        control_generalInformation_document: "https://example.org/controls/this_one"
                    ]
                ]
            ],
            name: "control",
            owner: [
                targetUri: "/units/"+unitId,
            ]])).resourceId
        def createdControlJson = parseNode(get("/controls/$controlId"))

        when: "validating the control JSON"
        def validationMessages = schema.validate(createdControlJson)

        then:
        validationMessages.empty
    }

    def "created document with custom aspect conforms to schema"() {
        given: "the document schema and a newly created document"
        def documentSchema = getSchema("document")
        def documentId = (String)parseJson(post("/documents", [
            name: "doc",
            owner: [
                targetUri: "/units/"+unitId
            ],
            customAspects: [
                document_details: [
                    attributes: [
                        document_details_approvalDate: "2020-01-01"
                    ]
                ]
            ]
        ])).resourceId
        def createdDocumentJson = parseNode(get("/documents/$documentId"))

        when: "validating the document JSON"
        def validationMessages = documentSchema.validate(createdDocumentJson)

        then:
        validationMessages.empty
    }

    def "created incident conforms to schema"() {
        given: "the incident schema and a newly created incident"
        def incidentSchema = getSchema("incident")
        // TODO VEO-320 add custom aspect & link.
        def incidentId = (String)parseJson(post("/incidents", [
            name: "incident",
            owner: [
                targetUri: "/units/"+unitId
            ]
        ])).resourceId
        def createdIncidentJson = parseNode(get("/incidents/$incidentId"))

        when: "validating the incident JSON"
        def validationMessages = incidentSchema.validate(createdIncidentJson)

        then:
        validationMessages.empty
    }

    def "created person with custom aspect conforms to schema"() {
        given: "the person schema and a newly created person"
        def personSchema = getSchema("person")
        def personId = (String)parseJson(post("/persons", [
            name: "person",
            owner: [
                targetUri: "/units/"+unitId
            ],
            customAspects: [
                person_address: [
                    attributes: [
                        person_address_city: "Goettingen"
                    ]
                ]
            ]
        ])).resourceId
        def createdPersonJson = parseNode(get("/persons/$personId"))

        when: "validating the process JSON"
        def validationMessages = personSchema.validate(createdPersonJson)

        then:
        validationMessages.empty
    }

    def "created process with custom aspect & links conforms to schema"() {
        given: "the process schema and a newly created process"
        def processSchema = getSchema("process")
        def personId = (String)parseJson(post("/persons", [
            domains: [
                [targetUri: "/domains/$domainId"]
            ],
            name: "person",
            owner: [
                targetUri: "/units/"+unitId
            ],
            subType: [
                (domainId): "PER_Controller"
            ]
        ])).resourceId
        def processId = (String)parseJson(post("/processes", [
            name: "process",
            owner: [
                targetUri: "/units/"+unitId
            ],
            links: [
                process_controller: [
                    [
                        attributes: [
                            process_controller_document: "http://example.org/doc"
                        ],
                        name: "first controller",
                        target: [
                            targetUri: "/persons/$personId"
                        ]
                    ]
                ]
            ],
            customAspects: [
                process_accessAuthorization: [
                    attributes: [
                        process_accessAuthorization_concept: true
                    ]
                ]
            ]
        ])).resourceId
        def createdProcessJson = parseNode(get("/processes/$processId"))

        when: "validating the process JSON"
        def validationMessages = processSchema.validate(createdProcessJson)

        then:
        validationMessages.empty
    }

    def "created scenario with custom aspect conforms to schema"() {
        given: "the scenario schema and a newly created scenario"
        def scenarioSchema = getSchema("scenario")
        def scenarioId = (String)parseJson(post("/scenarios", [
            name: "scenario",
            owner: [
                targetUri: "/units/"+unitId
            ],
            customAspects: [
                scenario_probability: [
                    attributes: [
                        scenario_probability_ofOccurrence: 'scenario_probability_ofOccurrence_high'
                    ]
                ]
            ]
        ])).resourceId
        def createdScenarioJson = parseNode(get("/scenarios/$scenarioId"))

        when: "validating the scenario JSON"
        def validationMessages = scenarioSchema.validate(createdScenarioJson)

        then:
        validationMessages.empty
    }

    def "created scope with custom aspect, link & member conforms to schema"() {
        given: "the scope schema and a scope with one member"
        def schema = getSchema("scope")
        def memberAssetId = parseJson(post("/assets", [
            name: "member",
            owner: [
                targetUri: "/units/"+unitId,
            ]])).resourceId
        def targetPersonId = parseJson(post("/persons", [
            name: "target",
            owner: [
                targetUri: "/units/"+unitId,
            ]])).resourceId
        def scopeId = parseJson(post("/scopes", [
            name: "scope",
            owner: [
                targetUri: "/units/"+unitId,
            ],
            customAspects: [
                scope_address: [
                    attributes: [
                        scope_address_city: "Goettingen"
                    ]
                ]
            ],
            links: [
                scope_dataProtectionOfficer: [
                    [
                        name: "doesn't matter",
                        target: [
                            targetUri: "/persons/$targetPersonId"
                        ]
                    ]
                ]
            ],
            members: [
                [
                    targetUri: "/assets/$memberAssetId"
                ]
            ]
        ])).resourceId
        def scope = parseNode(get("/scopes/$scopeId"))

        when: "validating the scope JSON"
        def validationMessages = schema.validate(scope)

        then:
        validationMessages.empty
    }

    private JsonSchema getSchema(String type) {
        def schemaString = entitySchemaService.findSchema(type, Collections.emptyList())
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaString)
    }

    JsonNode parseNode(ResultActions resultActions) {
        om.readTree(resultActions.andReturn().response.contentAsString)
    }
}
