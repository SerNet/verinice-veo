/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
 * The domain should be referenced by the domain objects if applicable. It
 * defines a standard, a best practice or a company-specific context.
 */
public interface Domain extends ModelObject, ClientOwned {

    String SINGULAR_TERM = "domain";
    String PLURAL_TERM = "domains";

    Boolean isActive();

    void setActive(Boolean aActive);

    @Override
    default Class<? extends ModelObject> getModelInterface() {
        return Domain.class;
    }

    @Override
    default String getModelType() {
        return SINGULAR_TERM;
    }

    void setOwner(Client owner);

    Client getOwner();

    default Client getClient() {
        return getOwner();
    }

    default void validateSubType(Class<? extends ModelObject> entityType, String subType)
            throws InvalidSubTypeException {
        // TODO VEO-516: validate subtype
    }
}
