/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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

import java.util.Set;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

/** Checks that an element's custom aspects's domain is contained in the element's domains. */
public class ElementCustomAspectsHaveDomain implements EntitySpecification<Element> {

  @Override
  public boolean test(Element element) {
    Set<Domain> domains = element.getDomains();
    return element.getCustomAspects().stream().allMatch(ca -> domains.containsAll(ca.getDomains()))
        && element.getLinks().stream().allMatch(l -> domains.containsAll(l.getDomains()));
  }
}
