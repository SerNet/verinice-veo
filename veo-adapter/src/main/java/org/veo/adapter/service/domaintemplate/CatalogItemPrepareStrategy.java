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

import java.util.List;
import java.util.stream.Collectors;

import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Catalogable;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;

/**
 * Defines the behavior to prepare {@link CatalogItem} or {@link Catalogable}
 * for reuse as catalogElement or incarnation. All subparts are associated with
 * the given domain.
 */
public class CatalogItemPrepareStrategy {
    private static final String NO_DESIGNATOR = "NO_DESIGNATOR";

    /**
     * Clean up and relink a catalogItem. Add the domain to each sub element.
     */
    public void prepareCatalogItem(Domain domain, Catalog catalog, CatalogItem item) {
        item.setId(null);
        item.setCatalog(catalog);
        Catalogable element = item.getElement();
        if (element != null) {
            prepareElement(domain, element, true);
        }
    }

    /**
     * Clean up and relink a {@link Catalogable}. Add the domain to each sub
     * element. Prepare the {@link Catalogable} for usage in a catalog or as an
     * incarnation.
     */
    public void prepareElement(Domain domain, Catalogable element, boolean isCatalogElement) {
        element.setId(null);
        element.setDesignator(isCatalogElement ? NO_DESIGNATOR : null);
        if (element instanceof EntityLayerSupertype) {
            EntityLayerSupertype est = (EntityLayerSupertype) element;
            est.getDomains()
               .clear();
            est.addToDomains(domain);
            processSubTypes(domain, est);
            processLinks(domain, est);
            processCustomAspects(domain, est);
            // TODO: VEO-612 add parts from CompositeEntity
        } else {
            throw new IllegalArgumentException(
                    "Element not of known type: " + element.getModelInterface()
                                                           .getSimpleName());
        }
    }

    private void processCustomAspects(Domain domain, EntityLayerSupertype est) {
        est.getCustomAspects()
           .forEach(ca -> {
               ca.getDomains()
                 .clear();
               ca.addToDomains(domain);
           });
    }

    private void processLinks(Domain domain, EntityLayerSupertype est) {
        est.getLinks()
           .forEach(link -> {
               link.getDomains()
                   .clear();
               link.addToDomains(domain);
           });
    }

    private void processSubTypes(Domain domain, EntityLayerSupertype est) {
        if (!est.getSubTypeAspects()
                .isEmpty()) {
            List<String> aspects = est.getSubTypeAspects()
                                      .stream()
                                      .map(sa -> sa.getSubType())
                                      .collect(Collectors.toList());
            est.getSubTypeAspects()
               .clear();
            aspects.forEach(a -> est.setSubType(domain, a));
        }
    }

}
