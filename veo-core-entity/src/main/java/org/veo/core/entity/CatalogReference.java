/*******************************************************************************
 * Copyright (c) 2021 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.entity;

import java.util.Optional;

/**
 * Basic type for catalog references.
 */
public interface CatalogReference extends ModelObject, ClientOwned {

    /**
     * The reference to an other catalogitem.
     */
    CatalogItem getCatalogItem();

    void setCatalogItem(CatalogItem aCatalogitem);

    CatalogItem getOwner();

    void setOwner(CatalogItem owner);

    default Optional<Client> getOwningClient() {
        return Optional.ofNullable(getOwner())
                       .map(CatalogItem::getClient);
    }
}
