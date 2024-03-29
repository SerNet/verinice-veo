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

/**
 * Actiontype for {@link TailoringReference}. It defines the kind of actions a {@link
 * TailoringReference} can define.
 */
public enum TailoringReferenceType {
  /** Defines an action to skip this {@link TailoringReference}. */
  OMIT,
  /**
   * Defines a {@link TailoringReference} which corresponds to a {@link CustomLink} or a {@link
   * CompositeElement} in the element of the owning {@link CatalogItem}. The target of the feature
   * need to be the element owned by the target of the {@link TailoringReference#getCatalogItem()}.
   */
  LINK,
  /**
   * Defines a link in another {@link Element} which points to the {@link CatalogItem#getElement()}.
   * The property {@link TailoringReference#getCatalogItem()} points to the {@link CatalogItem} in
   * which the {@link ExternalTailoringReference#getExternalLink()} object is added. This describes
   * the modeling of an opposite feature.
   */
  LINK_EXTERNAL,
  /**
   * Create the linked {@link CatalogItem#getElement()} in the same step. It is a bit vage in the
   * sense when to create.
   */
  COPY,
  /**
   * @see TailoringReferenceType#COPY, but clear in semantics, it alway create the linked {@link
   *     CatalogItem#getElement()} in the same step.
   */
  COPY_ALWAYS;
}
