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
package org.veo.core.entity.definitions;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.veo.core.entity.ElementType;
import org.veo.core.entity.definitions.attribute.AttributeDefinition;
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition;
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition;
import org.veo.core.entity.definitions.attribute.ListAttributeDefinition;
import org.veo.core.entity.state.ElementTypeDefinitionState;

public interface ElementTypeDefinition extends ElementTypeDefinitionState {
  @NotNull
  ElementType getElementType();

  void setSubTypes(Map<String, SubTypeDefinition> definitions);

  void setCustomAspects(Map<String, CustomAspectDefinition> definitions);

  default Optional<LinkDefinition> findLink(String type) {
    return Optional.ofNullable(getLinks().get(type));
  }

  void setLinks(Map<String, LinkDefinition> definitions);

  void setTranslations(Map<Locale, Map<String, String>> translations);

  default String findTranslation(Locale locale, String key) {
    return Optional.ofNullable(getTranslations().get(locale)).map(l -> l.get(key)).orElse(key);
  }

  void setControlImplementationDefinition(ControlImplementationDefinition definition);

  default CustomAspectDefinition getCustomAspectDefinition(String caType) {
    return Optional.ofNullable(getCustomAspects().get(caType))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Custom aspect '%s' is not defined".formatted(caType)));
  }

  default SubTypeDefinition getSubTypeDefinition(String subType) {
    return Optional.ofNullable(getSubTypes().get(subType))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Sub type %s is not defined, availabe sub types: %s"
                        .formatted(subType, getSubTypes().keySet())));
  }

  default String localizeCustomAspectAttributeValue(
      String caType, String attrKey, @Nullable Object value, Locale locale) {
    if (value == null) {
      return "-";
    }
    return format(value, locale, getCustomAspectDefinition(caType).getAttributeDefinition(attrKey));
  }

  private String format(@NotNull Object value, Locale locale, AttributeDefinition attrDef) {
    return switch (attrDef) {
      case BooleanAttributeDefinition _ ->
          ResourceBundle.getBundle("messages", locale)
              .getString(Boolean.TRUE.equals(value) ? "yes" : "no");
      case EnumAttributeDefinition _ -> getTranslations().get(locale).get(value);
      case ListAttributeDefinition listDef ->
          ((Collection<?>) value)
              .stream()
                  .map(i -> format(i, locale, listDef.getItemDefinition()))
                  .collect(Collectors.joining(", "));
      default -> value.toString();
    };
  }
}
