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

import java.util.List;
import java.util.Objects;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provides whether all given expressions ("operands") provide true. All operands must be boolean
 * expressions.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AndExpression implements VeoExpression {
  private List<VeoExpression> operands;

  @Override
  public Object getValue(Element element, Domain domain) {
    return operands.stream().allMatch(o -> Objects.equals(o.getValue(element, domain), true));
  }

  @Override
  public void selfValidate(DomainBase domain, String elementType) {
    operands.forEach(o -> o.selfValidate(domain, elementType));
    if (!operands.stream()
        .allMatch(o -> o.getValueType(domain, elementType).equals(Boolean.class))) {
      throw new IllegalArgumentException("Only boolean values can be used in an AND expression");
    }
  }

  @Override
  public Class<?> getValueType(DomainBase domain, String elementType) {
    return Boolean.class;
  }
}
