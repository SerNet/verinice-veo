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
public abstract class AbstractCreateItemsFromUnitUseCase<T extends TemplateItem<T>> {

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
              item.addLinkTailoringReference(
                  TailoringReferenceType.LINK,
                  elementsToCatalogItems.get(link.getTarget()),
                  link.getType(),
                  link.getAttributes());
              elementsToCatalogItems
                  .get(link.getTarget())
                  .addLinkTailoringReference(
                      TailoringReferenceType.LINK_EXTERNAL,
                      item,
                      link.getType(),
                      link.getAttributes());
            });

    if (element instanceof CompositeElement<?> composite) {
      composite
          .getParts()
          .forEach(
              target -> {
                T partCatalogItem = elementsToCatalogItems.get(target);
                partCatalogItem.addTailoringReference(TailoringReferenceType.COMPOSITE, item);
              });
      composite
          .getComposites()
          .forEach(
              target -> {
                T compositeCatalogItem = elementsToCatalogItems.get(target);
                compositeCatalogItem.addTailoringReference(TailoringReferenceType.PART, item);
              });
    } else if (element instanceof Scope scope) {
      scope
          .getMembers()
          .forEach(
              member -> {
                T memberAsCatalogItem = elementsToCatalogItems.get(member);
                memberAsCatalogItem.addTailoringReference(TailoringReferenceType.SCOPE, item);
                item.addTailoringReference(TailoringReferenceType.MEMBER, memberAsCatalogItem);
              });
    }

    if (element instanceof RiskAffected<?, ?> risky) {
      risky
          .getRisks()
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
