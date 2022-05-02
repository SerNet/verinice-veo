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
package org.veo.core.service;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

/** The CatalogItemService creates elements based on the given {@link CatalogItem}. */
public interface CatalogItemService {
  /**
   * Creates the element instance of an {@link CatalogItem#getElement()} and set the domain in all
   * parts. It also add the item to the field {@link Element#getAppliedCatalogItems()}. It creates
   * an so called incarnation of the given {@link CatalogItem#getElement()}. It creates an exact
   * copy of the {@link CatalogItem#getElement()} with no owner set, and all references linking the
   * catalog elements. The instance need to be saved and the link targets set to concrete elements
   * in order to be persistent.
   */
  Element createInstance(CatalogItem item, Domain domain);
}
