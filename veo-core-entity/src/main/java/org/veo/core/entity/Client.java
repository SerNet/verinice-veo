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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A client is the root object of the organizational structure. Usually a client
 * is a company or other large closed organizational entity. The client could be
 * used for high level authorization.
 */
public interface Client extends ModelObject {

    String getName();

    void setName(String aName);

    /**
     * Add the given Unit to the collection units.
     *
     * @return true if added
     */
    boolean addToUnits(Unit aUnit);

    /**
     * Remove the given Unit from the collection units.
     *
     * @return true if removed
     */
    boolean removeFromUnits(Unit aUnit);

    Set<Unit> getUnits();

    void setUnits(Set<Unit> aUnits);

    /**
     * Add the given Domain to the collection domains.
     *
     * @return true if added
     */
    boolean addToDomains(Domain aDomain);

    /**
     * Remove the given Domain from the collection domains.
     *
     * @return true if removed
     */
    boolean removeFromDomains(Domain aDomain);

    Set<Domain> getDomains();

    void setDomains(Set<Domain> aDomains);

    /**
     * Factory method to create a new unit in this client.
     *
     * @param name
     *            The name of the new unit
     * @return The newly created unit
     */
    public Unit createUnit(String name);

    /**
     * Returns a unit this client for the given id. Recurses into subunits,
     * sub-sub-units etc. to find a match.
     *
     * @param id
     *            The id of the unit to find
     * @return
     */
    public Optional<Unit> getUnit(Key<UUID> id);

    /**
     * Remove a unit or subunit from this client. This method correctly updates the
     * bidirectional relationship between the client and the unit. It will also
     * disassociate the unit from its parent unit if it has one.
     *
     * @param unit
     *            the unit or sub-unit to delete
     */
    public void removeUnit(Unit unit);

}
