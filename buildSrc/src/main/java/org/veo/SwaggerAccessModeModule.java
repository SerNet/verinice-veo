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
package org.veo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;

/**
 * Adds properties "readOnly" or "writeOnly" to property JSON schema if the
 * {@link Schema} defines one of these behaviours.
 * https://json-schema.org/draft-07/json-schema-validation.html#rfc.section.10.3
 */
public class SwaggerAccessModeModule implements Module {
    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
               .withInstanceAttributeOverride((ObjectNode collectedMemberAttributes,
                       FieldScope member, SchemaGenerationContext context) -> {
                   var schema = member.getAnnotationConsideringFieldAndGetter(Schema.class);
                   if (schema != null) {
                       if (schema.accessMode() == AccessMode.READ_ONLY) {
                           collectedMemberAttributes.put("readOnly", true);
                       } else if (schema.accessMode() == AccessMode.WRITE_ONLY) {
                           collectedMemberAttributes.put("writeOnly", true);
                       }
                   }
               });
    }
}
