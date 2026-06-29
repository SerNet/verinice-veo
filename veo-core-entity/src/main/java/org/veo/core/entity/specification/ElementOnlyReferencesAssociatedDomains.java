/*
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
 */
package org.veo.core.entity.specification;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.CustomAttributeContainer;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

/** Checks that an element only references domains that it is associated with. */
public class ElementOnlyReferencesAssociatedDomains implements EntitySpecification<Element> {

  @Override
  public boolean test(Element element) {
    return element.getDomains().containsAll(getReferencedDomains(element));
  }

  private static Set<Domain> getReferencedDomains(Element element) {
    return Stream.of(
            element.getCustomAspects().stream().map(CustomAttributeContainer::getDomain),
            element.getLinks().stream().map(CustomAttributeContainer::getDomain),
            element.getAppliedCatalogItems().stream().map(CatalogItem::requireDomainMembership))
        .flatMap(Function.identity())
        .collect(Collectors.toSet());
  }
}
