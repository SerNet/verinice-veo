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
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion

import org.veo.core.service.EntitySchemaService

import io.swagger.v3.core.util.Json
import spock.lang.Specification

class ClassPathMetaSchemaConformitySpec extends Specification {
    static EntitySchemaService entitySchemaService = new EntitySchemaServiceClassPathImpl()

    def customAspectMetaSchema = getMetaSchema("custom-aspect-meta-schema.json")
    def customLinkMetaSchema = getMetaSchema("custom-link-meta-schema.json")


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


    def "entity schema #schema.title is a valid schema"() {
        given:
        def schema07 = getMetaSchema("draft-07.json")
        expect:
        schema07.validate(schema).empty
        where:
        schema << entitySchemas
    }

    private JsonSchema getMetaSchema(String file) throws IOException {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(getClass().getClassLoader().getResource("schemas/meta/"+file).openStream())
    }

    private static List<JsonNode> getEntitySchemas() {
        entitySchemaService.listValidSchemaNames()
                .collect { entitySchemaService.findSchema(it, null) }
                .collect {  Json.mapper().readTree(it) }
    }
}
