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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ListOfExpression implements VeoExpression {
  @Valid @NotNull private List<VeoExpression> items;

  @Override
  public Object getValue(Element element, Domain domain) {
    return items.stream().map(e -> e.getValue(element, domain)).toList();
  }

  @Override
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return items.stream().anyMatch(i -> i.isAffectedByEvent(event, domain));
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    items.forEach(i -> i.selfValidate(domain, elementType));
  }

  @Override
  public VeoType getValueType(DomainBase domain, ElementType elementType) {
    return VeoType.listOf(
        VeoType.sumOf(items.stream().map(e -> e.getValueType(domain, elementType)).toList()));
  }
}
