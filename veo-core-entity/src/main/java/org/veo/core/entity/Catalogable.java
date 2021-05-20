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

import java.util.Optional;
import java.util.Set;

/**
 * Catalogable, the basic interface for all object which can be contained by a
 * catalogItem. A catalogable can refers several catalogItems which it represent
 * in the different domains.
 */
public interface Catalogable extends ModelObject, Displayable {

    /**
     * Stores the references of the applied catalog items. Question: Should this be
     * unique in one domain/template? Should an object exist in two different
     * version of the same domainTemplate?
     */
    Set<CatalogItem> getAppliedCatalogItems();

    void setAppliedCatalogItems(Set<CatalogItem> aCatalogitems);

    default ElementOwner getOwnerOrContainingCatalogItem() {
        return Optional.<ElementOwner> ofNullable(getOwner())
                       .orElse(getContainingCatalogItem());
    }

    default void setOwnerOrContainingCatalogItem(ElementOwner owner) {
        if (owner instanceof Unit) {
            this.setOwner((Unit) owner);
            this.setContainingCatalogItem(null);
        } else {
            this.setOwner(null);
            this.setContainingCatalogItem((CatalogItem) owner);
        }
    }

    Unit getOwner();

    void setOwner(Unit unit);

    CatalogItem getContainingCatalogItem();

    void setContainingCatalogItem(CatalogItem containigCatalogItem);

}
