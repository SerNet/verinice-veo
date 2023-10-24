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
package org.veo.core.entity;

import java.util.Optional;
import java.util.Set;

import org.veo.core.entity.exception.UnprocessableDataException;

public interface ProfileItem extends TemplateItem<ProfileItem>, ClientOwned {
  String SINGULAR_TERM = "profile-item";
  String PLURAL_TERM = "profile-items";

  /** All the tailoring references for this catalog item. */
  Set<TailoringReference<ProfileItem>> getTailoringReferences();

  @Override
  default DomainBase getDomainBase() {
    return getOwner().getOwner();
  }

  default void setTailoringReferences(Set<TailoringReference<ProfileItem>> tailoringReferences) {
    getTailoringReferences().clear();
    tailoringReferences.forEach(tailoringReference -> tailoringReference.setOwner(this));
    getTailoringReferences().addAll(tailoringReferences);
  }

  CatalogItem getAppliedCatalogItem();

  void setAppliedCatalogItem(CatalogItem item);

  @Override
  default Class<? extends Identifiable> getModelInterface() {
    return ProfileItem.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  default Optional<Client> getOwningClient() {
    return Optional.ofNullable(getOwner())
        .filter(ClientOwned.class::isInstance)
        .map(ClientOwned.class::cast)
        .flatMap(ClientOwned::getOwningClient);
  }

  public Profile getOwner();

  /**
   * @return this item's domain if the item belongs to a domain and can be applied
   * @throws UnprocessableDataException if this belongs to a domain template and cannot be applied
   */
  default Domain requireDomainMembership() {
    return getOwner().requireDomainMembership();
  }
}
