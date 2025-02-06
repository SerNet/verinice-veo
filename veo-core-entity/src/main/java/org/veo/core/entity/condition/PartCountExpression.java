/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.event.ControlPartsChangedEvent;
import org.veo.core.entity.event.ElementEvent;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Provides the amount of a composite element's parts. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Data
public class PartCountExpression implements VeoExpression {
  /** Define this to only count parts of a certain subtype */
  private @NotNull String partSubType;

  @Override
  public Object getValue(Element element, Domain domain) {
    if (element instanceof CompositeElement) {
      var parts = ((CompositeElement<?>) element).getParts();
      if (partSubType != null) {
        parts =
            parts.stream()
                .filter(
                    c ->
                        partSubType.equals(
                            // TODO VEO-1569: this fails if the part's domainAssociations are a
                            // hibernate proxy
                            c.findSubType(domain).orElse(null)))
                .collect(Collectors.toSet());
      }
      return parts.size();
    }
    return 0;
  }

  @Override
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return event instanceof ControlPartsChangedEvent && event.getEntityType().equals(Control.class);
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    domain.getElementTypeDefinition(elementType).getSubTypeDefinition(getPartSubType());
  }

  @Override
  public Class<?> getValueType(DomainBase domain, ElementType elementType) {
    return Integer.class;
  }
}
