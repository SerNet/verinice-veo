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

import java.util.Collection;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CustomAttributeContainer;
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
public class AttributeExpression implements VeoExpression {
  @NotNull private VeoExpression source;
  @NotNull private String attribute;

  @Override
  public Object getValue(Element element, Domain domain) {
    Object sourceValue = source.getValue(element, domain);
    if (sourceValue instanceof CustomAttributeContainer caOrLink) {
      return getValue(caOrLink);
    }
    if (sourceValue instanceof Collection<?> casOrLinks) {
      return casOrLinks.stream()
          .map(CustomAttributeContainer.class::cast)
          .map(this::getValue)
          .toList();
    }
    throw new IllegalArgumentException("Unexpected source value");
  }

  private Object getValue(CustomAttributeContainer caOrLink) {
    return caOrLink.getAttributes().get(attribute);
  }

  @Override
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return source.isAffectedByEvent(event, domain);
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    getValueType(domain, elementType);
  }

  @Override
  public VeoType getValueType(DomainBase domain, ElementType elementType) {
    VeoType sourcetype = source.getValueType(domain, elementType);
    return sourcetype
        .findListItemType()
        .map(
            itemType ->
                VeoType.listOf(itemType.getAttributeType(attribute, "invalid source items")))
        .orElseGet(() -> sourcetype.getAttributeType(attribute, "invalid source"));
  }
}
