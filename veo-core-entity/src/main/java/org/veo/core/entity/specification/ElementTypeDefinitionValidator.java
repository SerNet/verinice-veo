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
package org.veo.core.entity.specification;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.SubTypeDefinition;
import org.veo.core.entity.exception.UnprocessableDataException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ElementTypeDefinitionValidator {

  private static final Pattern KEY_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");

  public static void validate(ElementTypeDefinition elementTypeDefinition) {
    validate(getSubTypeKeys(elementTypeDefinition.getSubTypes()));
    validate(getCaOrLinkKeys(elementTypeDefinition.getCustomAspects()));
    validate(getCaOrLinkKeys(elementTypeDefinition.getLinks()));
    if (elementTypeDefinition.getControlImplementationDefinition() != null) {
      validate(
          getCaOrLinkKeys(
              elementTypeDefinition.getControlImplementationDefinition().getCustomAspects()));
      TranslationValidator.validateControlImplementationDefinition(
          elementTypeDefinition.getControlImplementationDefinition());
    }
    TranslationValidator.validate(elementTypeDefinition);
  }

  private static Set<String> getSubTypeKeys(Map<String, SubTypeDefinition> subTypes) {
    var keys = new HashSet<>(subTypes.keySet());
    keys.addAll(
        subTypes.values().stream()
            .flatMap(s -> s.getStatuses().stream())
            .collect(Collectors.toSet()));
    return keys;
  }

  private static Set<String> getCaOrLinkKeys(
      Map<String, ? extends CustomAspectDefinition> definitions) {
    var keys = new HashSet<>(definitions.keySet());
    keys.addAll(
        definitions.values().stream()
            .flatMap(def -> getCaOrLinkKeys(def).stream())
            .collect(Collectors.toSet()));
    return keys;
  }

  private static Set<String> getCaOrLinkKeys(CustomAspectDefinition def) {
    var keys = new HashSet<>(def.getAttributeDefinitions().keySet());
    keys.addAll(
        def.getAttributeDefinitions().values().stream()
            .flatMap(attrDef -> attrDef.getTranslationKeys().stream())
            .collect(Collectors.toSet()));
    return keys;
  }

  private static void validate(Set<String> keys) {
    keys.forEach(
        k -> {
          if (!KEY_PATTERN.matcher(k).matches()) {
            throw new UnprocessableDataException(
                "Invalid key '%s' - keys may only contain English alphabet letters, digits, underscores & hyphens"
                    .formatted(k));
          }
        });
  }
}
