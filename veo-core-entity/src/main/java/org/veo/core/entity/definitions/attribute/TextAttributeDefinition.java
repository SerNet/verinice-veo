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

import java.util.List;

import org.veo.core.entity.ValidationError;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public final class TextAttributeDefinition extends AttributeDefinition {
  public static final String TYPE = "text";

  @Override
  public List<ValidationError> getErrors(Object value) {
    if (!(value instanceof String)) {
      return List.of(ValidationError.localized("error_no_string"));
    }
    return List.of();
  }

  @Override
  public Class<?> getValueType() {
    return String.class;
  }
}
