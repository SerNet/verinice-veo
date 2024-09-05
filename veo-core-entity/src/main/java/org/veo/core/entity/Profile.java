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

import java.util.Set;
import java.util.stream.Collectors;

import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.state.ProfileItemState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES")
public interface Profile extends Versioned, Identifiable, ClientOwned, ProfileState {
  String SINGULAR_TERM = "profile";
  String PLURAL_TERM = "profiles";

  @Override
  default Class<Profile> getModelInterface() {
    return Profile.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  void setName(String name);

  void setDescription(String description);

  void setLanguage(String language);

  /** {@link ProfileState#getProductId()} */
  void setProductId(String productId);

  Set<ProfileItem> getItems();

  void setItems(Set<ProfileItem> items);

  @Override
  default Set<ProfileItemState> getItemStates() {
    return getItems().stream().map(i -> (ProfileItemState) i).collect(Collectors.toSet());
  }

  void setOwner(DomainBase owner);

  DomainBase getOwner();

  default Domain requireDomainMembership() {
    if (getOwner() instanceof Domain domain) {
      return domain;
    }
    throw new UnprocessableDataException(
        "Profile is part of a domain template, items cannot be applied");
  }
}
