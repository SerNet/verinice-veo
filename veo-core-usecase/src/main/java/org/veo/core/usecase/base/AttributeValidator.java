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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.veo.core.entity.ValidationError;
import org.veo.core.entity.definitions.attribute.AttributeDefinition;
import org.veo.core.entity.exception.InvalidAttributeException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Validates custom aspect / link attributes according to the domain's element type definitions. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AttributeValidator {

  static void validate(
      Map<String, Object> attributes, Map<String, AttributeDefinition> attributeDefinitions) {
    ValidationError.throwOnErrors(getErrors(attributes, attributeDefinitions));
  }

  public static void validate(
      String attrKey, Object attrValue, Map<String, AttributeDefinition> definitions) {
    ValidationError.throwOnErrors(getErrors(attrKey, attrValue, definitions));
  }

  public static List<ValidationError> getErrors(
      Map<String, Object> attributes, Map<String, AttributeDefinition> attributeDefinitions) {
    return attributes.entrySet().stream()
        .flatMap((attr) -> getErrors(attr.getKey(), attr.getValue(), attributeDefinitions).stream())
        .toList();
  }

  public static List<ValidationError> getErrors(
      String attrKey, Object attrValue, Map<String, AttributeDefinition> definitions) {
    var attrDefinition = definitions.get(attrKey);
    if (attrDefinition == null) {
      return List.of(
          new ValidationError.Generic(String.format("Attribute '%s' is not defined", attrKey)));
    }
    try {
      attrDefinition.validate(attrValue);
    } catch (InvalidAttributeException e) {
      return List.of(
          new ValidationError.Generic(
              String.format(
                  "Invalid value '%s' for attribute '%s': %s",
                  attrValue, attrKey, e.getMessage())));
    }
    return Collections.emptyList();
  }
}
