/*
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
 */
package org.veo.core.entity.condition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.type.VeoType;

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
  public void selfValidate(DomainBase domain, ElementType elementType) {
    value.selfValidate(domain, elementType);
    from.selfValidate(domain, elementType);
    VeoType sourceType = from.getValueType(domain, elementType);
    sourceType.mustBeListOrNothing("invalid source ('from') for removal");
    sourceType
        .findListItemType()
        .ifPresent(
            sourceItemType ->
                sourceItemType.mustIntersectWith(
                    value.getValueType(domain, elementType),
                    "source ('from') cannot contain value"));
  }

  @Override
  public VeoType getValueType(DomainBase domain, ElementType elementType) {
    return from.getValueType(domain, elementType);
  }
}
