/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade
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
import java.util.Optional;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Removes an item ("value") from a list attribute ("from"). */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RemoveExpression implements VeoExpression {
  private CustomAspectAttributeValueExpression from;
  private ConstantExpression value;

  @Override
  public Object getValue(Element element, Domain domain) {
    Object val = value.getValue(element, domain);
    return Optional.ofNullable(from.getValue(element, domain))
        .map(List.class::cast)
        .map(l -> l.stream().filter(it -> !Objects.equals(it, val)).toList())
        .orElse(null);
  }

  @Override
  public void selfValidate(DomainBase domain, String elementType) {
    value.selfValidate(domain, elementType);
    from.selfValidate(domain, elementType);
    var fromType = from.getValueType(domain, elementType);
    if (!List.class.isAssignableFrom(fromType)) {
      throw new IllegalArgumentException(
          "Cannot use %s as 'from' type.".formatted(fromType.getSimpleName()));
    }
  }

  @Override
  public Class<?> getValueType(DomainBase domain, String elementType) {
    return List.class;
  }
}
