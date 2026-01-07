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

import org.veo.core.entity.exception.InvalidAttributeException;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public final class IntegerAttributeDefinition extends AttributeDefinition {
  public static final String TYPE = "integer";

  @Override
  public void validate(Object value) throws InvalidAttributeException {
    if (!(value instanceof Integer)) {
      throw new InvalidAttributeException("must be an integer");
    }
  }

  @Override
  public Class<?> getValueType() {
    return Integer.class;
  }
}
