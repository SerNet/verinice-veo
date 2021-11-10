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
import com.networknt.schema.SpecVersion.VersionFlag

import org.veo.core.entity.EntityType
import org.veo.core.service.EntitySchemaService

import io.swagger.v3.core.util.Json
import spock.lang.Shared
import spock.lang.Specification

class ClassPathSchemaSpec extends Specification {
    private static final String SCHEMA_FILES_PATH = "/schemas/entity/"

    static EntitySchemaService entitySchemaService = new EntitySchemaServiceClassPathImpl(SCHEMA_FILES_PATH)

    @Shared def customAspectMetaSchema = getMetaSchemaV7("custom-aspect-meta-schema.json")
    @Shared def customLinkMetaSchema = getMetaSchemaV7("custom-link-meta-schema.json")
    @Shared def domainsMetaSchema = getMetaSchemaV7("domains-meta-schema.json")


    def "Custom aspect #aspect.id of #aspect.schema schema conform to meta schema"() {
        expect:
        customAspectMetaSchema.validate(aspect.data).empty
        where:
        aspect << entitySchemas.collect{entitySchema->
            entitySchema.get("properties").get("customAspects").get("properties").fields().collect {field ->
                [id: field.key, schema:entitySchema.title, data: field.value ]
            }
        }.flatten()
    }


    def "Custom link #link.id of #link.schema schema conform to meta schema"() {
        expect:
        customLinkMetaSchema.validate(link.data).empty
        where:
        link << entitySchemas.collect{entitySchema->
            entitySchema.get("properties").get("links").get("properties").fields().collect {field ->
                [id: field.key, schema:entitySchema.title, data: field.value.items ]
            }
        }.flatten()
    }

    def "domains node in #schema.title conforms to meta schema"() {
        expect:
        def domainsNode = schema.get("properties").get("domains")
        domainsMetaSchema.validate(domainsNode).empty
        where:
        schema << entitySchemas
    }


    def "entity schema #schema.title is a valid schema"() {
        given:
        def schema201909 = getMetaSchemaV2019_09()
        expect:
        schema201909.validate(schema).empty
        where:
        schema << entitySchemas
    }

    def "designator is marked read-only in entity schema #schema.title"() {
        expect:
        schema.get("properties").get("designator").get("readOnly").booleanValue()
        where:
        schema << entitySchemas
    }

    def "_self is marked read-only in entity schema #schema.title"() {
        expect:
        schema.get("properties").get("_self").get("readOnly").booleanValue()
        where:
        schema << entitySchemas
    }

    private JsonSchema getMetaSchemaV7(String file) throws IOException {
        return JsonSchemaFactory.getInstance(VersionFlag.V7)
                .getSchema(getClass().getClassLoader().getResource("schemas/meta/"+file).openStream())
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

    private static List<JsonNode> getEntitySchemas() {
        EntityType.ELEMENT_TYPES
                .collect { it.singularTerm }
                .collect { entitySchemaService.findSchema(it, null) }
                .collect {  Json.mapper().readTree(it) }
    }
}
