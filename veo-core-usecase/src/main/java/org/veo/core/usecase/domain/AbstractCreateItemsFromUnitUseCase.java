/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
package org.veo.core.usecase.domain;

import java.util.Map;
import java.util.Optional;

import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scope;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.UnitRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class AbstractCreateItemsFromUnitUseCase<
    T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable> {

  protected final EntityFactory factory;
  protected final DomainRepository domainRepository;
  protected final GenericElementRepository genericElementRepository;
  protected final UnitRepository unitRepository;

  protected void createTailorreferences(Map<Element, T> elementsToCatalogItems, Domain domain) {
    elementsToCatalogItems.values().forEach(TemplateItem::clearTailoringReferences);
    elementsToCatalogItems.forEach(
        (element, item) ->
            createTailoringReferences(elementsToCatalogItems, domain, element, item));
  }

  protected void createTailoringReferences(
      Map<Element, T> elementsToCatalogItems, Domain domain, Element element, T item) {
    element
        .getLinks(domain)
        .forEach(
            link -> {
              var targetItem = elementsToCatalogItems.get(link.getTarget());
              item.addLinkTailoringReference(
                  TailoringReferenceType.LINK, targetItem, link.getType(), link.getAttributes());
              targetItem.addLinkTailoringReference(
                  TailoringReferenceType.LINK_EXTERNAL, item, link.getType(), link.getAttributes());
            });

    if (element instanceof CompositeElement<?> composite) {
      composite.getParts().stream()
          .filter(p -> p.isAssociatedWithDomain(domain))
          .forEach(
              target -> {
                T partCatalogItem = elementsToCatalogItems.get(target);
                partCatalogItem.addTailoringReference(TailoringReferenceType.COMPOSITE, item);
              });
      composite.getComposites().stream()
          .filter(c -> c.isAssociatedWithDomain(domain))
          .forEach(
              target -> {
                T compositeCatalogItem = elementsToCatalogItems.get(target);
                compositeCatalogItem.addTailoringReference(TailoringReferenceType.PART, item);
              });
    } else if (element instanceof Scope scope) {
      scope.getMembers().stream()
          .filter(m -> m.isAssociatedWithDomain(domain))
          .forEach(
              member -> {
                T memberAsCatalogItem = elementsToCatalogItems.get(member);
                memberAsCatalogItem.addTailoringReference(TailoringReferenceType.SCOPE, item);
                item.addTailoringReference(TailoringReferenceType.MEMBER, memberAsCatalogItem);
              });
    }

    if (element instanceof RiskAffected<?, ?> risky) {
      risky.getControlImplementations().stream()
          .filter(ci -> ci.getControl().isAssociatedWithDomain(domain))
          .forEach(
              ci ->
                  item.addControlImplementationReference(
                      elementsToCatalogItems.get(ci.getControl()),
                      Optional.ofNullable(ci.getResponsible())
                          .map(elementsToCatalogItems::get)
                          .orElse(null),
                      ci.getDescription()));
      risky.getRequirementImplementations().stream()
          .filter(ri -> ri.getControl().isAssociatedWithDomain(domain))
          .filter(ri -> !ri.isUnedited())
          .forEach(
              ri ->
                  item.addRequirementImplementationReference(
                      elementsToCatalogItems.get(ri.getControl()),
                      ri.getStatus(),
                      ri.getImplementationStatement(),
                      ri.getImplementationUntil(),
                      Optional.ofNullable(ri.getResponsible())
                          .map(elementsToCatalogItems::get)
                          .orElse(null)));
      risky.getRisks().stream()
          .filter(r -> r.getScenario().isAssociatedWithDomain(domain))
          .forEach(
              r ->
                  item.addRiskTailoringReference(
                      TailoringReferenceType.RISK,
                      elementsToCatalogItems.get(r.getScenario()),
                      Optional.ofNullable(r.getRiskOwner())
                          .map(elementsToCatalogItems::get)
                          .orElse(null),
                      Optional.ofNullable(r.getMitigation())
                          .map(elementsToCatalogItems::get)
                          .orElse(null),
                      r.getTailoringReferenceValues(domain)));
    }
  }
}
