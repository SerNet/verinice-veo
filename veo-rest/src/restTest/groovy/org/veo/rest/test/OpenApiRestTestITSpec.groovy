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

import org.veo.categories.MapGetProperties

import spock.util.mop.Use

@Use(MapGetProperties)
class OpenApiRestTestITSpec extends VeoRestTest {
    def "all schema properties have sufficient validation rules"() {
        expect:
        def invalidProperties = apiDocs.components.schemas
                .findAll { k, v -> !v.readOnly }
                .collectMany({schemaName, schemaDefinition ->
                    schemaDefinition.properties
                            ?.findAll { propName, propDefinition -> !hasSufficientValidationRules(propName, propDefinition) }
                            ?.collect { propName, propDefinition -> "$schemaName.$propName" }
                            ?: []
                })
        invalidProperties.size() == 0
    }

    Object getApiDocs() {
        return get('/v3/api-docs').body
    }

    static boolean hasSufficientValidationRules(String propName, Object propDef) {
        if (propDef.readOnly) {
            return true
        }
        // Ignore discriminator for polymorphism (json sub types).
        if (propName == "type") {
            return true
        }
        if (propDef.type == "string") {
            return propDef.format != null || propDef.enum != null || propDef.maxLength != null || propDef.pattern != null
        }
        return true
    }
}
