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

import org.veo.core.entity.definitions.attribute.AttributeDefinition;
import org.veo.core.entity.exception.InvalidAttributeException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Validates custom aspect / link attributes according to the domain's element type definitions. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AttributeValidator {

  static void validate(
      Map<String, Object> attributes, Map<String, AttributeDefinition> attributeDefinitions) {
    attributes.forEach((attrKey, attrValue) -> validate(attrKey, attrValue, attributeDefinitions));
  }

  public static void validate(
      String attrKey, Object attrValue, Map<String, AttributeDefinition> definitions) {
    var attrDefinition = definitions.get(attrKey);
    if (attrDefinition == null) {
      throw new IllegalArgumentException(String.format("Attribute '%s' is not defined", attrKey));
    }
    try {
      attrDefinition.validate(attrValue);
    } catch (InvalidAttributeException e) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid value '%s' for attribute '%s': %s", attrValue, attrKey, e.getMessage()));
    }
  }
}
