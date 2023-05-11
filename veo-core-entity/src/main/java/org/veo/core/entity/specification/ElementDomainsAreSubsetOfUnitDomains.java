/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.entity.specification;

import org.veo.core.entity.Element;

/** Checks that an element is only associated with domains that are assigned to its unit. */
public class ElementDomainsAreSubsetOfUnitDomains implements EntitySpecification<Element> {

  @Override
  public boolean test(Element element) {
    if (element.getContainingCatalogItem() != null) {
      return true;
    }
    return element.getOwner().getDomains().containsAll(element.getDomains());
  }
}
