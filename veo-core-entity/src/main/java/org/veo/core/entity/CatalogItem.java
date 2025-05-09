/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.core.entity;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.veo.core.entity.exception.UnprocessableDataException;

/**
 * CatalogItem The catalog item contains an element and/other related catalog item. It describes
 * currently two different abstract use cases: 1. Apply the contained element: defined by KEa.1 and
 * KEa.2 2. Update an entity from an old to a new version of the domainTemplate to the new one.
 * Usecase 1 is defined by the element and a set of TailoringReferences. Usecase 2 is defined by a
 * set of UpdateReferences.
 */
public interface CatalogItem extends ClientOwned, TemplateItem<CatalogItem, DomainBase> {

  String SINGULAR_TERM = "catalog-item";
  String PLURAL_TERM = "catalog-items";

  Comparator<? super CatalogItem> BY_CATALOGITEMS =
      Comparator.comparing(SymIdentifiable::getSymbolicIdAsString);

  /**
   * @return this item's domain if the item belongs to a domain and can be applied
   * @throws UnprocessableDataException if this belongs to a domain template and cannot be applied
   */
  @Override
  default Domain requireDomainMembership() {
    if (getDomainBase() instanceof Domain domain) {
      return domain;
    }
    throw new UnprocessableDataException(
        "Catalog item is part of a domain template and cannot be applied");
  }

  /**
   * Includes itself together with {@link this.getElementsToCreate()}. This list is ordered. The
   * item itself is at the first position.
   */
  default List<CatalogItem> getAllItemsToIncarnate() {
    return Stream.concat(
            Stream.of(this), getElementsToCreate().stream().sorted(BY_CATALOGITEMS).distinct())
        .toList();
  }

  /**
   * Return the set additional elements to create. These elements are defined by {@link
   * TailoringReference} of type {@link TailoringReferenceType#COPY} or {@link
   * TailoringReferenceType#COPY_ALWAYS}.
   */
  default Set<CatalogItem> getElementsToCreate() {
    Set<CatalogItem> elementsToCreate = new HashSet<>();
    this.getTailoringReferences().stream()
        .filter(TailoringReference::isCopyRef)
        .forEach(r -> addElementsToCopy(r, elementsToCreate));
    return elementsToCreate;
  }

  default void addElementsToCopy(
      TailoringReference<CatalogItem, DomainBase> reference, Set<CatalogItem> itemList) {
    itemList.add(reference.getTarget());
    reference.getTarget().getTailoringReferences().stream()
        .filter(TailoringReference::isCopyRef)
        .forEach(rr -> addElementsToCopy(rr, itemList));
  }

  default void setTailoringReferences(
      Set<TailoringReference<CatalogItem, DomainBase>> tailoringReferences) {
    clearTailoringReferences();
    tailoringReferences.forEach(tailoringReference -> tailoringReference.setOwner(this));
    getTailoringReferences().addAll(tailoringReferences);
  }

  /** All the update refreneces for this catalog item. */
  Set<UpdateReference> getUpdateReferences();

  default void setUpdateReferences(Set<UpdateReference> updateReferences) {
    getUpdateReferences().clear();
    updateReferences.forEach(updateReference -> updateReference.setOwner(this));
    getUpdateReferences().addAll(updateReferences);
  }

  @Override
  default Class<CatalogItem> getModelInterface() {
    return CatalogItem.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  @Override
  default String getDisplayName() {
    return getAbbreviation() == null ? getName() : getAbbreviation() + " " + getName();
  }

  @Override
  default Optional<Client> getOwningClient() {
    return Optional.ofNullable(getDomainBase())
        .filter(ClientOwned.class::isInstance)
        .map(ClientOwned.class::cast)
        .flatMap(ClientOwned::getOwningClient);
  }

  void setDomainBase(DomainBase owner);

  @Override
  default DomainBase getNamespace() {
    return getDomainBase();
  }
}
