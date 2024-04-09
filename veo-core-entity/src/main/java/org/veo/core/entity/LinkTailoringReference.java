/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler
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

import java.util.Map;

import org.veo.core.entity.state.LinkTailoringReferenceState;

/**
 * This reference type is used to describe a {@link CustomLink} on an {@link Element}. When this is
 * applied with {@link TailoringReferenceType#LINK}, a custom link is created from the element
 * describes by the owning template item to the element described by the target template item. When
 * this is applied with {@link TailoringReferenceType#LINK_EXTERNAL}, a custom link is created in
 * the opposite direction.
 */
public interface LinkTailoringReference<T extends TemplateItem<T>>
    extends TailoringReference<T>, LinkTailoringReferenceState<T> {
  void setLinkType(String aType);

  void setAttributes(Map<String, Object> attributes);

  default T getLinkSourceItem() {
    if (getReferenceType().equals(TailoringReferenceType.LINK_EXTERNAL)) {
      return getTarget();
    }
    return getOwner();
  }

  default T getLinkTargetItem() {
    if (getReferenceType().equals(TailoringReferenceType.LINK_EXTERNAL)) {
      return getOwner();
    }
    return getTarget();
  }
}
