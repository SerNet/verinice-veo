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

import javax.annotation.Nullable;

import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.entity.state.ProfileItemState;

public interface ProfileItem
    extends ProfileItemState, TemplateItem<ProfileItem, Profile>, ClientOwned {
  String SINGULAR_TERM = "profile-item";
  String PLURAL_TERM = "profile-items";

  /** All the tailoring references for this catalog item. */
  @Override
  Set<TailoringReference<ProfileItem, Profile>> getTailoringReferences();

  @Override
  default DomainBase getDomainBase() {
    return getOwner().getOwner();
  }

  @Override
  default Optional<CatalogItem> findCatalogItem() {
    return Optional.ofNullable(getAppliedCatalogItem());
  }

  default void setTailoringReferences(
      Set<TailoringReference<ProfileItem, Profile>> tailoringReferences) {
    clearTailoringReferences();
    tailoringReferences.forEach(tailoringReference -> tailoringReference.setOwner(this));
    getTailoringReferences().addAll(tailoringReferences);
  }

  CatalogItem getAppliedCatalogItem();

  void setAppliedCatalogItem(CatalogItem item);

  @Override
  default Class<ProfileItem> getModelInterface() {
    return ProfileItem.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  @Override
  default Optional<Client> getOwningClient() {
    return Optional.ofNullable(getOwner())
        .filter(ClientOwned.class::isInstance)
        .map(ClientOwned.class::cast)
        .flatMap(ClientOwned::getOwningClient);
  }

  Profile getOwner();

  /**
   * @return this item's domain if the item belongs to a domain and can be applied
   * @throws UnprocessableDataException if this belongs to a domain template and cannot be applied
   */
  @Override
  default Domain requireDomainMembership() {
    return getOwner().requireDomainMembership();
  }

  @Nullable
  @Override
  default ITypedSymbolicId<CatalogItem, DomainBase> getAppliedCatalogItemRef() {
    return Optional.ofNullable(getAppliedCatalogItem()).map(TypedSymbolicId::from).orElse(null);
  }

  @Override
  default Profile getNamespace() {
    return getOwner();
  }
}
