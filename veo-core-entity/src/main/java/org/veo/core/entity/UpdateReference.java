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

import jakarta.validation.constraints.NotNull;

/**
 * An update reference refers a catalog item of another catalog or version of the catalog. It
 * describes how the previous calalog item need to be modified to comply to the new version. The
 * following constrains applies to the update refs: 1. The reference catalogItem point to a
 * catalogitem in the previous catalog when: The update type is upate, replace, split or join 2. The
 * reference catalogItem point to a catalogitem in this catalog when: The update type is add
 */
public interface UpdateReference extends TemplateItemReference<CatalogItem> {
  String SINGULAR_TERM = "updatereference";
  String PLURAL_TERM = "updatereferences";

  /** The type of action for the update reference. */
  @NotNull
  ItemUpdateType getUpdateType();

  void setUpdateType(ItemUpdateType aUpdateType);

  @Override
  default Class<UpdateReference> getModelInterface() {
    return UpdateReference.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }
}
