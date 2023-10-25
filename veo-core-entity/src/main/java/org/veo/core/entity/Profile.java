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

import org.veo.core.entity.exception.UnprocessableDataException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES")
public interface Profile extends Versioned, Identifiable, ClientOwned {
  String SINGULAR_TERM = "profile";
  String PLURAL_TERM = "profiles";

  @Override
  default Class<? extends Identifiable> getModelInterface() {
    return Profile.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  String getName();

  void setName(String name);

  String getDescription();

  void setDescription(String description);

  String getLanguage();

  void setLanguage(String language);

  Set<ProfileItem> getItems();

  void setItems(Set<ProfileItem> items);

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
