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

import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.LinkTailoringReference;
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

  protected abstract void createTailoringReference(T source, T target, TailoringReferenceType type);

  protected abstract LinkTailoringReference<T> createLinkReference(
      CustomLink link, T source, T target, TailoringReferenceType type);

  protected void createTailorreferences(Map<Element, T> elementsToCatalogItems, Domain domain) {
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
              createLinkReference(
                  link,
                  item,
                  elementsToCatalogItems.get(link.getTarget()),
                  TailoringReferenceType.LINK);
              createLinkReference(
                  link,
                  elementsToCatalogItems.get(link.getTarget()),
                  item,
                  TailoringReferenceType.LINK_EXTERNAL);
            });

    if (element instanceof CompositeElement<?> composite) {
      composite
          .getParts()
          .forEach(
              target -> {
                T partCatalogItem = elementsToCatalogItems.get(target);
                createTailoringReference(partCatalogItem, item, TailoringReferenceType.COMPOSITE);
              });
      composite
          .getComposites()
          .forEach(
              target -> {
                T compositeCatalogItem = elementsToCatalogItems.get(target);
                createTailoringReference(compositeCatalogItem, item, TailoringReferenceType.PART);
              });
    } else if (element instanceof Scope scope) {
      scope
          .getMembers()
          .forEach(
              member -> {
                T memberAsCatalogItem = elementsToCatalogItems.get(member);
                createTailoringReference(memberAsCatalogItem, item, TailoringReferenceType.SCOPE);
                createTailoringReference(item, memberAsCatalogItem, TailoringReferenceType.MEMBER);
              });
    }
  }
}
