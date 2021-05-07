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

/**
 * Something that can own an element, and therefore can be used as the owner of
 * an element. Unit and CatalogItems are examples.
 */
public interface ElementOwner extends ModelObject, Displayable, ClientOwned {
    Client getClient();

    /**
     * Returns the owner casted as Unit.
     *
     * @throws IllegalArgumentException
     *             when the owner is no an unit
     */
    default Unit asUnit() {
        if (isUnitType())
            return (Unit) this;
        else
            throw new UnsupportedOperationException("Owner not of type Unit.");
    }

    default boolean isCatalogItemType() {
        return getModelInterface().equals(CatalogItem.class);
    }

    default boolean isUnitType() {
        return getModelInterface().equals(Unit.class);
    }

    default boolean isPartOfDomainTemplate() {
        return getClient() == null;
    }
}
