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
package org.veo.adapter.service.domaintemplate;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;

/**
 * Defines the behavior to prepare {@link CatalogItem} or {@link Element} for reuse as
 * catalogElement or incarnation. All subparts are associated with the given domain.
 */
public class CatalogItemPrepareStrategy {
  private static final String NO_DESIGNATOR = "NO_DESIGNATOR";

  /** Assigns designator to catalog item. */
  public void prepareCatalogItem(CatalogItem item) {
    item.getElement().setDesignator(NO_DESIGNATOR);
  }

  /**
   * Clean up and relink a {@link Element}. Add the domain to each sub element. Prepare the {@link
   * Element} for usage in a catalog or as an incarnation.
   */
  public void prepareElement(Domain domain, Element element, boolean isCatalogElement) {
    element.setId(null);
    element.setDesignator(isCatalogElement ? NO_DESIGNATOR : null);
    element.getDomains().clear();
    element.getDomains().add(domain);
    processSubTypes(domain, element);
    processLinks(domain, element);
    processCustomAspects(domain, element);
    // TODO: VEO-612 add parts from CompositeEntity
  }

  private void processCustomAspects(DomainBase domain, Element est) {
    est.getCustomAspects()
        .forEach(
            ca -> {
              ca.setDomain(domain);
            });
  }

  private void processLinks(DomainBase domain, Element est) {
    est.getLinks()
        .forEach(
            link -> {
              link.setDomain(domain);
            });
  }

  private void processSubTypes(DomainBase domain, Element est) {
    est.getSubTypeAspects().forEach(oldAspect -> oldAspect.setDomain(domain));
  }
}
