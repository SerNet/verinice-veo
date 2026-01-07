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
package org.veo.core.entity.definitions.attribute;

import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.exception.InvalidAttributeException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = PRIVATE)
@Data
@EqualsAndHashCode(callSuper = true)
public final class ListAttributeDefinition extends AttributeDefinition {
  public static final String TYPE = "list";
  @NotNull private AttributeDefinition itemDefinition;

  @Override
  public void validate(Object value) throws InvalidAttributeException {
    if (value instanceof List list) {
      for (var i = 0; i < list.size(); i++) {
        try {
          itemDefinition.validate(list.get(i));
        } catch (InvalidAttributeException ex) {
          throw new InvalidAttributeException(
              "invalid item at index %d: %s".formatted(i, ex.getMessage()));
        }
      }
    } else {
      throw new InvalidAttributeException("must be a list");
    }
  }

  @Override
  public Collection<String> getTranslationKeys() {
    return itemDefinition.getTranslationKeys();
  }

  @Override
  public Class<?> getValueType() {
    return List.class;
  }
}
