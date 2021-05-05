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

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion

import org.veo.core.VeoMvcSpec
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.UnitRepository
import org.veo.core.service.EntitySchemaService
import org.veo.rest.configuration.WebMvcSecurityConfiguration

/**
 * Tests if resources returned by the API conform to the entity schema.
 */
class EntitySchemaConformityMvcSpec extends VeoMvcSpec {
    @Autowired
    EntitySchemaService entitySchemaService

    @Autowired
    UnitRepository unitRepository

    @Autowired
    ClientRepository clientRepository

    String unitId

    def setup() {
        def client = clientRepository.save(newClient {
            dbId = WebMvcSecurityConfiguration.TESTCLIENT_UUID
        })
        unitId = unitRepository.save(newUnit(client)).dbId
    }

    @WithUserDetails("user@domain.example")
    def "created minimalist asset conforms to schema"() {
        given: "the asset schema and a newly created asset"
        def schema = getSchema("asset")
        def assetId = (String)parseJson(post("/assets", [
            name: "asset",
            owner: [
                targetUri: "/units/"+unitId,
            ]])).resourceId
        def createdAssetJson = new ObjectMapper().readTree(get("/assets/$assetId").andReturn().response.contentAsString)

        when: "validating the asset JSON"
        def validationMessages = schema.validate(createdAssetJson)

        then:
        validationMessages.empty
    }

    @WithUserDetails("user@domain.example")
    def "created process with props & links conforms to schema"() {
        given: "the process schema and a newly created process"
        def processSchema = getSchema("process")
        def personId = (String)parseJson(post("/persons", [
            name: "person",
            owner: [
                targetUri: "/units/"+unitId
            ]])).resourceId
        def processId = (String)parseJson(post("/processes", [
            name: "process",
            owner: [
                targetUri: "/units/"+unitId
            ],
            links: [
                process_AffectedParties: [
                    [
                        attributes: [
                            process_AffectedParties_supplementaryInformation: "strongly affected"
                        ],
                        name: "first affected party",
                        target: [
                            targetUri: "/persons/$personId"
                        ]
                    ]
                ]
            ],
            customAspects: [
                process_InternalRecipient: [
                    attributes: [
                        process_InternalRecipient_InternalRecipient: true
                    ]
                ]
            ]
        ])).resourceId
        def createdProcessJson = new ObjectMapper().readTree(get("/processes/$processId").andReturn().response.contentAsString)

        when: "validating the process JSON"
        def validationMessages = processSchema.validate(createdProcessJson)

        then:
        validationMessages.empty
    }

    @WithUserDetails("user@domain.example")
    def "created scope with member conforms to schema"() {
        given: "the scope schema and a scope with one member"
        def schema = getSchema("scope")
        def memberAssetId = parseJson(post("/assets", [
            name: "member",
            owner: [
                targetUri: "/units/"+unitId,
            ]])).resourceId
        def scopeId = parseJson(post("/scopes", [
            name: "scope",
            owner: [
                targetUri: "/units/"+unitId,
            ],
            members: [
                [
                    targetUri: "/assets/$memberAssetId"
                ]
            ]
        ])).resourceId
        def scope = new ObjectMapper().readTree(get("/scopes/$scopeId").andReturn().response.contentAsString)

        when: "validating the scope JSON"
        def validationMessages = schema.validate(scope)

        then:
        validationMessages.empty
    }

    private JsonSchema getSchema(String type) {
        def schemaString = entitySchemaService.findSchema(type, Collections.emptyList())
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaString)
    }
}
