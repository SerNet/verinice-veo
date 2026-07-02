/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.core.entity.type;

import java.util.Map;
import java.util.stream.Collectors;

import org.veo.core.entity.definitions.CustomAspectDefinition;

import lombok.NonNull;

public record AttributeContainerType(Map<String, VeoType> attributeTypes) implements VeoType {
  @Override
  public VeoType getAttributeType(String attribute, String errorContext) {
    if (!attributeTypes.containsKey(attribute)) {
      throw new IllegalArgumentException(
          "%s: %s does not contain attribute %s".formatted(errorContext, this, attribute));
    }
    return attributeTypes.get(attribute);
  }

  public AttributeContainerType(CustomAspectDefinition definition) {
    this(
        definition.getAttributeDefinitions().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValueType())));
  }

  @Override
  public boolean includes(VeoType other) {
    return other instanceof AttributeContainerType(Map<String, VeoType> otherAttributeTypes)
        && otherAttributeTypes.entrySet().stream()
            .allMatch(
                e ->
                    attributeTypes.containsKey(e.getKey())
                        && attributeTypes.get(e.getKey()).includes(e.getValue()));
  }

  @Override
  public boolean intersectsWith(VeoType other) {
    return other instanceof AttributeContainerType(Map<String, VeoType> otherAttributes)
        && otherAttributes.entrySet().stream()
            .anyMatch(
                e ->
                    attributeTypes.containsKey(e.getKey())
                        && attributeTypes.get(e.getKey()).intersectsWith(e.getValue()));
  }

  @Override
  public String toHumanReadable() {
    return "AttributeContainer<%s>"
        .formatted(
            attributeTypes.entrySet().stream()
                .map(e -> "%s:%s".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(",")));
  }

  @Override
  @NonNull
  public String toString() {
    return toHumanReadable();
  }
}
