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
package org.veo.core.entity.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelPackage;
import org.veo.core.entity.Unit;

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
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class UnitImpl extends BaseModelObject implements Unit {

    @NotNull
    private String name;
    private String abbreviation;
    private String description;
    @NotNull
    private Client client;
    private Set<Unit> units = new HashSet<>();
    private Unit parent;
    private Set<Domain> domains = new HashSet<>();

    public UnitImpl(@NotNull Key<UUID> id, String name, Client client) {
        super(id);
        this.name = name;
        this.client = client;
    }

    /**
     * Factory method to create a new subunit as a member of this unit.
     *
     * @param name
     *            The name of the new subunit
     * @return The newly created subunit
     */
    public Unit createSubUnit(String name) {
        Unit newSubunit = new UnitImpl(Key.newUuid(), name, getClient());
        newSubunit.setParent(this);
        this.addToUnits(newSubunit);
        return newSubunit;
    }

    @Override
    public void remove() {
        this.state = Lifecycle.STORED_DELETED;
        if (parent != null) {
            parent.removeFromUnits(this);
            parent = null;
        }
    }

    /**
     * Add the given Unit to the collection units. opposite of Unit.parent
     *
     * @return true if added
     */
    public boolean addToUnits(Unit aUnit) {
        boolean add = this.units.add(aUnit);
        return add;
    }

    /**
     * Remove the given Unit from the collection units. opposite of Unit.parent
     *
     * @return true if removed
     */
    public boolean removeFromUnits(Unit aUnit) {
        boolean remove = this.units.remove(aUnit);
        return remove;
    }

    /**
     * Add the given Domain to the collection domains.
     *
     * @return true if added
     */
    public boolean addToDomains(Domain aDomain) {
        boolean add = this.domains.add(aDomain);
        return add;
    }

    /**
     * Remove the given Domain from the collection domains.
     *
     * @return true if removed
     */
    public boolean removeFromDomains(Domain aDomain) {
        boolean remove = this.domains.remove(aDomain);
        return remove;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String aName) {
        this.name = aName;
    }

    @Override
    public String getAbbreviation() {
        return this.abbreviation;
    }

    @Override
    public void setAbbreviation(String aAbbreviation) {
        this.abbreviation = aAbbreviation;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String aDescription) {
        this.description = aDescription;
    }

    @Override
    public Client getClient() {
        return this.client;
    }

    @Override
    public void setClient(Client aClient) {
        this.client = aClient;
    }

    @Override
    public Set<Unit> getUnits() {
        return this.units;
    }

    @Override
    public void setUnits(Set<Unit> aUnits) {
        this.units = aUnits;
    }

    @Override
    public Unit getParent() {
        return this.parent;
    }

    /**
     * opposite of Unit.units
     **/
    @Override
    public void setParent(Unit aParent) {
        this.parent = aParent;
    }

    @Override
    public Set<Domain> getDomains() {
        return this.domains;
    }

    @Override
    public void setDomains(Set<Domain> aDomains) {
        this.domains = aDomains;
    }

    @Override
    public String toString() {
        return "UnitImpl [name=" + name + ",abbreviation=" + abbreviation + ",description="
                + description + "]";
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_UNIT;
    }
}
