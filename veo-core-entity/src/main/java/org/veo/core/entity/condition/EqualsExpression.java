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

import java.util.Objects;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returns whether the value of one expression ("left") equals the value of another expression
 * ("right").
 */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EqualsExpression implements VeoExpression {
  private VeoExpression left;
  private VeoExpression right;

  @Override
  public Object getValue(Element element, Domain domain) {
    return Objects.equals(left.getValue(element, domain), right.getValue(element, domain));
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    left.selfValidate(domain, elementType);
    right.selfValidate(domain, elementType);
    var leftType = left.getValueType(domain, elementType);
    var rightType = right.getValueType(domain, elementType);
    if (leftType != null && rightType != null && !leftType.equals(rightType)) {
      throw new IllegalArgumentException(
          "Cannot compare %s to %s".formatted(leftType.getSimpleName(), rightType.getSimpleName()));
    }
  }

  @Override
  public Class<?> getValueType(DomainBase domain, ElementType elementType) {
    return Boolean.class;
  }
}
