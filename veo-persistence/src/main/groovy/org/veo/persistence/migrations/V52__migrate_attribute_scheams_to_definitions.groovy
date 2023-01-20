/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.persistence.migrations

import java.sql.ResultSet

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

import com.fasterxml.jackson.databind.ObjectMapper

import groovy.sql.Sql

class V52__migrate_attribute_scheams_to_definitions extends BaseJavaMigration {
    private om = new ObjectMapper()

    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with{
            var cache = [:]
            query("select db_id, custom_aspects, links from element_type_definition;") { ResultSet row ->
                while (row.next()) {
                    execute([
                        id: row.getString("db_id"),
                        customAspects: cache.computeIfAbsent(row.getString("custom_aspects")) { migrateCAsOrLinks(it) },
                        links: cache.computeIfAbsent(row.getString("links")) { migrateCAsOrLinks(it) },
                    ], """
                        update element_type_definition
                            set custom_aspects = :customAspects::jsonb, links = :links::jsonb
                            where db_id = :id
                            """)
                }
            }
        }
    }

    String migrateCAsOrLinks(String json) {
        return json
                .with { om.readValue(it, Map) }
                .each {entry ->
                    def caOrLinkDefinition = entry.value
                    caOrLinkDefinition.attributeDefinitions = caOrLinkDefinition
                            .attributeSchemas
                            .collectEntries { attrKey, attrSchema ->
                                [
                                    attrKey,
                                    schemaToDefinition(attrSchema)
                                ]
                            }
                    caOrLinkDefinition.remove("attributeSchemas")
                }
                .with{this.om.writeValueAsString(it)}
    }

    Map schemaToDefinition(Map schema) {
        if (schema.containsKey("items")) {
            return [
                type: "list",
                itemDefinition: schemaToDefinition(schema.items)
            ]
        }
        if (schema.containsKey("enum")) {
            return [
                type: "enum",
                allowedValues: schema.enum
            ]
        }
        if (schema.containsKey("format")) {
            switch (schema.format) {
                case "date": return [type: "date"]
                case "date-time": return [type: "dateTime"]
                case "uri": return [type: "externalDocument"]
                default: throw new IllegalArgumentException("Unsupported format")
            }
        }
        if (schema.containsKey("type")) {
            switch (schema.type) {
                case "integer": return [type: "integer"]
                case "boolean": return [type: "boolean"]
                case "string": return [type: "text"]
                default: throw new IllegalArgumentException("Unsupported type")
            }
        }
        throw new IllegalArgumentException("Unsupported attribute schema")
    }
}
