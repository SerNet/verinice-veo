/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.persistence.schema

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion

import org.veo.adapter.persistence.schema.EntitySchemaServiceClassPathImpl
import org.veo.core.service.EntitySchemaService

import io.swagger.v3.core.util.Json
import spock.lang.Specification

class ClassPathMetaSchemaConformitySpec extends Specification {
    static EntitySchemaService entitySchemaService = new EntitySchemaServiceClassPathImpl()
    List<JsonNode> entitySchemas

    def setup() {
        entitySchemas = entitySchemaService.listValidSchemaNames().getKnownSchemas()
                .collect { entitySchemaService.findSchema(it, null) }
                .collect {  Json.mapper().readTree(it) }
    }

    def "all custom aspect schemas conform to meta schema"() {
        expect:
        def customAspectMetaSchema = getMetaSchema("custom-aspect-meta-schema.json")
        entitySchemas.forEach{
            it.get("properties").get("customAspects").get("properties").forEach {
                assert customAspectMetaSchema.validate(it).empty
            }
        }
    }

    def "all custom link schemas conform to meta schema"() {
        expect:
        def customLinkMetaSchema = getMetaSchema("custom-link-meta-schema.json")
        entitySchemas.forEach{
            it.get("properties").get("links").get("properties").forEach {
                assert customLinkMetaSchema.validate(it.get("items")).empty
            }
        }
    }

    private JsonSchema getMetaSchema(String file) throws IOException {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(getClass().getClassLoader().getResource("schemas/meta/"+file).openStream());
    }
}
