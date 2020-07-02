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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A unit is high level group of elements defined by organizational structure.
 * Units may contain other units. For instance, a unit could be a division, a
 * department or a project. Unit is a component that defines ownership and
 * primary responsibility. An organizational unit. Units may have sub-units.
 * Every entity object is assigned to exactly one unit at all times. When the
 * unit is deleted, all its entities will be deleted as well. A unit defines
 * object ownership. Small and medium organizations may just have one unit.
 * Large enterprises may have multiple units for different subsidiaries. Service
 * providers might have one unit for each client that is using the software. A
 * unit always belongs to exactly one client. This means that every entity also
 * transitively belongs to exactly one client. Units cannot be moved between
 * clients. The <code>EntityGroup</code> object is much more flexible and the
 * preferred choice to group entities together for business modeling purposes.
 * Units should exclusively be used to model ownership and high-level access
 * restrictions.
 */
public interface Unit extends NameAble, ModelObject {

    Client getClient();

    void setClient(Client aClient);

    /**
     * Add the given Unit to the collection units. Adding will set the parent to
     * this.
     *
     * @return true if added
     */
    boolean addToUnits(Unit aUnit);

    /**
     * Remove the given Unit from the collection units. Removing will set the parent
     * to null.
     *
     * @return true if removed
     */
    boolean removeFromUnits(Unit aUnit);

    Set<Unit> getUnits();

    void setUnits(Set<Unit> aUnits);

    Unit getParent();

    void setParent(Unit aParent);

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
     * Returns flat list of all units and their subunits recursively.
     *
     * @param units
     * @return
     */
    public static Set<Unit> flatten(Set<Unit> units) {
        return units.stream()
                    .flatMap(u -> Stream.concat(Stream.of(u), flatten(u.getUnits()).stream()))
                    .collect(Collectors.toSet());
    }

    /**
     * Creates a new unit as a sub-unit of this one.
     *
     * @param name
     *            The name for the new unit
     * @return The newly created unit
     */
    public Unit createSubUnit(String name);

    /**
     * Removes the unit. Disassociates it from a parent collection if it is a
     * sub-unit.
     */
    void remove();

}
