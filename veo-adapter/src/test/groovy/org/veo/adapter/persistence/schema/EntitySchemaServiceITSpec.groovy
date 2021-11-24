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
package org.veo.adapter.persistence.schema

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonMetaSchema
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory

import org.veo.core.entity.Domain
import org.veo.core.entity.EntityType
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.ElementTypeDefinition
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.service.EntitySchemaService

import io.swagger.v3.core.util.Json
import spock.lang.Specification

/**
 * Tests {@link EntitySchemaService} , {@link EntitySchemaGenerator} & {@link SchemaExtender} by letting them produce
 * schemas for two test domains and verifying the produced schemas.
 */
class EntitySchemaServiceITSpec extends Specification {
    static EntitySchemaService entitySchemaService = new EntitySchemaServiceImpl(new EntitySchemaGenerator(new SchemaExtender()))
    public static final String PROPS = "properties"

    def "entity schema is a valid schema"() {
        given:
        def schema201909 = getMetaSchemaV2019_09()
        def schema = getSchema(Set.of(getTestDomain()))
        expect:
        schema201909.validate(schema).empty
    }

    def "designator is marked read-only in entity schema #schema.title"() {
        expect:
        def schema = getSchema(Set.of(getTestDomain()))
        schema.get(PROPS).get("designator").get("readOnly").booleanValue()
    }

    def "_self is marked read-only in entity schema #schema.title"() {
        given:
        def schema = getSchema(Set.of(getTestDomain()))
        expect:
        schema.get(PROPS).get("_self").get("readOnly").booleanValue()
    }

    def "definitions from multiple domains are composed"() {
        given:
        def type = "whateverType"

        def testDomain = getTestDomain()
        def extraTestDomain = getExtraTestDomain()
        def schema = getSchema(Set.of(testDomain, extraTestDomain))

        expect:
        with(schema.get(PROPS).get("customAspects").get(PROPS)) {
            get("test").get(PROPS).get("attributes").get(PROPS).get("testAttr").get("type").textValue() == "string"
            get("extraTest").get(PROPS).get("attributes").get(PROPS).get("extraTestAttr").get("type").textValue() == "boolean"
        }
        with(schema.get(PROPS).get("links").get(PROPS)) {
            get("test").get("items").get(PROPS).get("attributes").get(PROPS).get("linkTestAttr").get("type").textValue() == "string"
            get("test").get("items").get(PROPS).get("target").get(PROPS).get("type").get("enum")*.textValue() == ["otherType"]
            get("test").get("items").get(PROPS).get("target").get(PROPS).get("subType").get("enum")*.textValue() == ["otherSubType"]

            get("extraTest").get("items").get(PROPS).get("attributes").get(PROPS).get("extraLinkTestAttr").get("type").textValue() == "integer"
            get("extraTest").get("items").get(PROPS).get("target").get(PROPS).get("type").get("enum")*.textValue() == ["extraType"]
            get("extraTest").get("items").get(PROPS).get("target").get(PROPS).get("subType").get("enum")*.textValue() == ["extraSubType"]
        }
        with(schema.get(PROPS).get("domains").get(PROPS)) {
            get(testDomain.idAsString).get(PROPS).get("subType").get("enum")*.textValue() == ["testSubType"]
            get(testDomain.idAsString).get("allOf").first().get("if").get(PROPS).get("subType").get("const").textValue() == "testSubType"
            get(testDomain.idAsString).get("allOf").first().get("then").get(PROPS).get("status").get("enum")*.textValue() == ["NEW", "OLD"]

            get(extraTestDomain.idAsString).get(PROPS).get("subType").get("enum")*.textValue() == ["extraTestSubType"]
            get(extraTestDomain.idAsString).get("allOf").first().get("if").get(PROPS).get("subType").get("const").textValue() == "extraTestSubType"
            get(extraTestDomain.idAsString).get("allOf").first().get("then").get(PROPS).get("status").get("enum")*.textValue() == ["EXTRA_NEW", "EXTRA_OLD"]
        }
    }

    private JsonSchema getMetaSchemaV2019_09() throws IOException {
        def cl = getClass().getClassLoader()
        JsonMetaSchema metaSchema = JsonMetaSchema.v201909
        return JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(metaSchema.getUri())
                .uriFetcher({ uri->
                    String name = uri.toString().split('/').last()
                    cl.getResourceAsStream("schemas/meta/v2019_09/"+name)
                }, "https")
                .addMetaSchema(metaSchema)
                .build()
                .getSchema(cl.getResourceAsStream("schemas/meta/draft-2019-09.json"))
    }

    private List<JsonNode> getEntitySchemas() {
        EntityType.ELEMENT_TYPES
                .collect { it.singularTerm }
                .collect { getSchema(Set.of(getTestDomain())) }
    }

    private JsonNode getSchema(Set<Domain> domains) {
        Json.mapper().readTree(entitySchemaService.findSchema("asset", domains))
    }

    private Domain getTestDomain() {
        return Mock(Domain) {
            idAsString >> UUID.randomUUID()
            getElementTypeDefinition("asset") >> [
                Mock(ElementTypeDefinition) {
                    customAspects >> [
                        test: Mock(CustomAspectDefinition) {
                            attributeSchemas >> [
                                testAttr: [
                                    type: "string"
                                ]
                            ]
                        }
                    ]
                    links >> [
                        test: Mock(LinkDefinition) {
                            attributeSchemas >> [
                                linkTestAttr: [
                                    type: "string"
                                ]
                            ]
                            targetType >> "otherType"
                            targetSubType >> "otherSubType"
                        }
                    ]
                    subTypes >> [
                        testSubType: Mock(SubTypeDefinition) {
                            statuses >> ["NEW", "OLD"]
                        }
                    ]
                }
            ]
        }
    }

    private Domain getExtraTestDomain() {
        return Mock(Domain) {
            idAsString >> UUID.randomUUID()
            getElementTypeDefinition("asset") >> [
                Mock(ElementTypeDefinition) {
                    customAspects >> [
                        extraTest: Mock(CustomAspectDefinition) {
                            attributeSchemas >> [
                                extraTestAttr: [
                                    type: "boolean"
                                ]
                            ]
                        }
                    ]
                    links >> [
                        extraTest: Mock(LinkDefinition) {
                            attributeSchemas >> [
                                extraLinkTestAttr: [
                                    type: "integer"
                                ]
                            ]
                            targetType >> "extraType"
                            targetSubType >> "extraSubType"
                        }
                    ]
                    subTypes >> [
                        extraTestSubType: Mock(SubTypeDefinition) {
                            statuses >> ["EXTRA_NEW", "EXTRA_OLD"]
                        }
                    ]
                }
            ]
        }
    }
}