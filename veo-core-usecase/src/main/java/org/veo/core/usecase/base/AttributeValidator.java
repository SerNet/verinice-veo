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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.veo.core.entity.ValidationError;
import org.veo.core.entity.definitions.attribute.AttributeDefinition;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Validates custom aspect / link attributes according to the domain's element type definitions. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AttributeValidator {

  static void validate(
      Map<String, Object> attributes,
      Map<String, AttributeDefinition> attributeDefinitions,
      Map<Locale, Map<String, String>> translations) {
    ValidationError.throwOnErrors(getErrors(attributes, attributeDefinitions, translations));
  }

  public static List<ValidationError> getErrors(
      Map<String, Object> attributes,
      Map<String, AttributeDefinition> attributeDefinitions,
      Map<Locale, Map<String, String>> translations) {
    return attributes.entrySet().stream()
        .flatMap(
            (attr) ->
                getErrors(attr.getKey(), attr.getValue(), attributeDefinitions, translations)
                    .stream())
        .toList();
  }

  public static List<ValidationError> getErrors(
      String attrKey,
      Object attrValue,
      Map<String, AttributeDefinition> definitions,
      Map<Locale, Map<String, String>> translations) {
    var attrDefinition = definitions.get(attrKey);
    if (attrDefinition == null) {
      return List.of(ValidationError.localized("error_attribute_not_defined", attrKey));
    }

    return ValidationError.mergeIfAny(
        ValidationError.localized(
            "error_invalid_attribute_value",
            List.of(
                (_) -> attrValue,
                l -> translations.getOrDefault(l, Map.of()).getOrDefault(attrKey, attrKey))),
        attrDefinition.getErrors(attrValue));
  }
}
