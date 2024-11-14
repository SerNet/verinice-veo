/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade.
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
package org.veo.core.entity;

import javax.annotation.Nonnull;

import org.veo.core.entity.definitions.attribute.AttributeDefinition;

public record BreakingChange(
    @Nonnull String type,
    @Nonnull ChangeType change,
    @Nonnull String elementType,
    @Nonnull String customAspect,
    @Nonnull String attribute,
    @Nonnull AttributeDefinition oldValue,
    AttributeDefinition value) {

  public static BreakingChange removal(
      String elementType, String customAspect, String attribute, AttributeDefinition oldValue) {
    return new BreakingChange(
        "customAspectAttribute",
        ChangeType.REMOVAL,
        elementType,
        customAspect,
        attribute,
        oldValue,
        null);
  }

  public static BreakingChange modification(
      String elementType,
      String customAspect,
      String attribute,
      AttributeDefinition oldValue,
      AttributeDefinition value) {
    return new BreakingChange(
        "customAspectAttribute",
        ChangeType.MODIFICATION,
        elementType,
        customAspect,
        attribute,
        oldValue,
        value);
  }

  @Override
  public String toString() {
    return "%s attribute '%s' of custom aspect '%s' for type %s"
        .formatted(
            change == ChangeType.REMOVAL ? "Removed" : "Modified",
            attribute,
            customAspect,
            elementType);
  }

  enum ChangeType {
    MODIFICATION,
    REMOVAL
  }
}
