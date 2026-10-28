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
import java.util.stream.Stream;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.LinkDirection;
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
  private String linkType;
  private ElementType sourceType;
  private boolean impactInheritingOnly;

  public LinksExpression(LinkDirection direction, String linkType, ElementType sourceType) {
    this(direction, linkType, sourceType, false);
  }

  @Override
  public Object getValue(Element element, Domain domain) {
    return findAllLinks(element, domain)
        .filter(l -> linkType == null || l.getType().equals(linkType))
        .filter(
            l ->
                !impactInheritingOnly
                    || isImpactInheriting(domain, l.getSource().getType(), l.getType()))
        .toList();
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
        && (linkType == null || ile.getLinkType().equals(linkType));
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    if (direction == LinkDirection.INBOUND) {
      Objects.requireNonNull(sourceType, "sourceType must be specified for inbound links");
    } else if (sourceType != null) {
      throw new IllegalArgumentException(
          "sourceType is not applicable for outbound links and should be null");
    }
    getValueType(domain, elementType);
  }

  @Override
  public VeoType getValueType(DomainBase domain, ElementType elementType) {
    ElementType linkSourceType = direction == LinkDirection.INBOUND ? this.sourceType : elementType;
    if (linkType != null) {
      if (impactInheritingOnly && !isImpactInheriting(domain, linkSourceType, linkType)) {
        throw new IllegalArgumentException(
            "Link type '%s' does not inherit impacts".formatted(linkType));
      }
      return VeoType.listOf(
          VeoType.attributeContainer(domain.getLinkDefinition(linkSourceType, linkType)));
    }
    var linkTypes =
        domain.getElementTypeDefinition(linkSourceType).getLinks().entrySet().stream()
            .filter(
                e ->
                    direction == LinkDirection.OUTBOUND
                        || e.getValue().getTargetType() == elementType)
            .filter(
                e ->
                    !impactInheritingOnly || isImpactInheriting(domain, linkSourceType, e.getKey()))
            .map(e -> VeoType.attributeContainer(e.getValue()))
            .toList();
    if (linkTypes.isEmpty()) {
      throw new IllegalArgumentException(
          "No matching links defined for " + elementType.getPluralTerm());
    }
    return VeoType.listOf(linkTypes.size() == 1 ? linkTypes.getFirst() : VeoType.sumOf(linkTypes));
  }

  private static boolean isImpactInheriting(
      DomainBase domain, ElementType linkSourceType, String linkType) {
    return domain.getRiskDefinitions().values().stream()
        .anyMatch(
            rd ->
                rd.getImpactInheritingLinks()
                    .getOrDefault(linkSourceType, List.of())
                    .contains(linkType));
  }
}
