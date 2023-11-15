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
package org.veo.core.entity.condition;

import java.util.Optional;
import java.util.stream.Stream;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Provides a constant primitive value. */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConstantExpression implements VeoExpression {
  private Object value;

  @Override
  public Object getValue(Element element, Domain domain) {
    return value;
  }

  @Override
  public void selfValidate(DomainBase domain, String elementType) {
    if (value != null
        && Stream.of(String.class, Boolean.class, Number.class)
            .noneMatch(t -> t.isInstance(value))) {
      throw new IllegalArgumentException("Constant value must be a string, boolean or number");
    }
  }

  @Override
  public Class<?> getValueType(DomainBase domain, String elementType) {
    return Optional.ofNullable(value).map(Object::getClass).orElse(null);
  }
}
