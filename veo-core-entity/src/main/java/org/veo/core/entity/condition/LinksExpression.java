/*******************************************************************************
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
 ******************************************************************************/
package org.veo.core.entity.condition;

import java.util.Objects;
import java.util.stream.Stream;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.LinkDirection;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.event.InboundLinkEvent;
import org.veo.core.entity.type.VeoType;

import edu.umd.cs.findbugs.annotations.NonNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class LinksExpression implements VeoExpression {
  @NotNull private LinkDirection direction;
  @NotNull private String linkType;
  private ElementType sourceType;

  @Override
  public Object getValue(Element element, Domain domain) {
    return findAllLinks(element, domain).filter(l -> l.getType().equals(linkType)).toList();
  }

  @NonNull
  private Stream<CustomLink> findAllLinks(Element element, Domain domain) {
    return direction == LinkDirection.INBOUND
        ? element.getInboundLinks(domain).stream()
            .filter(l -> l.getSource().getType() == sourceType)
        : element.getLinks(domain).stream();
  }

  @Override
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return event instanceof InboundLinkEvent ile
        && direction == LinkDirection.INBOUND
        && ile.getLinkType().equals(linkType);
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    if (direction == LinkDirection.INBOUND) {
      Objects.requireNonNull(sourceType, "sourceType must be specified for inbound links");
    } else if (sourceType != null) {
      throw new IllegalArgumentException(
          "sourceType is not applicable for outbound links and should be null");
    }
    getLinkDefinition(domain, elementType);
  }

  @Override
  public VeoType getValueType(DomainBase domain, ElementType elementType) {
    return VeoType.listOf(VeoType.attributeContainer(getLinkDefinition(domain, elementType)));
  }

  private LinkDefinition getLinkDefinition(DomainBase domain, ElementType elementType) {
    ElementType sourceType = direction == LinkDirection.INBOUND ? this.sourceType : elementType;
    return domain.getLinkDefinition(sourceType, linkType);
  }
}
