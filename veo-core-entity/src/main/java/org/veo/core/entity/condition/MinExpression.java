/*
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
 */
package org.veo.core.entity.condition;

import java.util.List;
import java.util.Objects;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.type.VeoType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class MinExpression implements VeoExpression {
  private VeoExpression values;

  @Override
  public Object getValue(Element element, Domain domain) {
    var in = (List<?>) values.getValue(element, domain);
    return in.stream()
        .filter(Objects::nonNull)
        .min(getItemType(domain, element.getType()).getComparator())
        .orElse(null);
  }

  @Override
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return values.isAffectedByEvent(event, domain);
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    getItemType(domain, elementType).getComparator();
  }

  @Override
  public VeoType getValueType(DomainBase domain, ElementType elementType) {
    return getItemType(domain, elementType).orNothing();
  }

  private VeoType getItemType(DomainBase domain, ElementType elementType) {
    return values
        .getValueType(domain, elementType)
        .mustBeListAndGetValueType("minimum can only be determined from a list");
  }
}
