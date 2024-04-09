/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.entity.state;

import javax.annotation.Nullable;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.ref.ITypedId;

public interface ProfileItemState extends TemplateItemState<ProfileItem> {
  @Nullable
  ITypedId<CatalogItem> getAppliedCatalogItemRef();
}
