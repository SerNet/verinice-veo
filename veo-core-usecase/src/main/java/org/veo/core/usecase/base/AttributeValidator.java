/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.usecase.base;

import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

/** Validates custom aspect / link attributes according to the domain's element type definitions. */
class AttributeValidator {
  // TODO VEO-1258 remove these
  private static final JsonSchemaFactory JSON_SCHEMA_FACTORY =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static void validate(Map<String, Object> attributes, Map<String, Object> attributeSchemas) {
    attributes.forEach(
        (attrKey, attrValue) -> {
          var attrSchema = attributeSchemas.get(attrKey);
          if (attrSchema == null) {
            throw new IllegalArgumentException(
                String.format("Attribute '%s' is not defined", attrKey));
          }
          // TODO-1258 use custom validation instead of JSON schema validation
          var errors =
              JSON_SCHEMA_FACTORY
                  .getSchema(OBJECT_MAPPER.valueToTree(attrSchema))
                  .validate(OBJECT_MAPPER.valueToTree(attrValue));
          if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                String.format(
                    "Invalid value for attribute '%s': %s",
                    attrKey,
                    errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining())));
          }
        });
  }
}
