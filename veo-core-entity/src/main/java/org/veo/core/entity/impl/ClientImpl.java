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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelPackage;
import org.veo.core.entity.Unit;
import org.veo.core.entity.specification.InvalidUnitException;

/**
 * A client is the root object of the organizational structure. Usually a client
 * is a company or other large closed organizational entity. The client could be
 * used for high level authorization.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ClientImpl extends BaseModelObject implements Client {

    private String name;
    private Set<Unit> units = new HashSet<>();
    private Set<Domain> domains = new HashSet<>();

    public ClientImpl(@NotNull Key<UUID> id) {
        super(id);
    }

    /**
     * Static factory method to create a new client.
     *
     * @param id
     *            The id for the newly create client
     * @param name
     *            The name for the newly created client
     * @return The client object which has been created but not yet persisted
     */
    public ClientImpl(Key<UUID> id, String name) {
        super(id);
        this.name = name;
        this.domains = new HashSet<>();
        this.units = new HashSet<>();
        setState(Lifecycle.CREATING);
    }

    @Override
    public Unit createUnit(String name) {
        Unit newUnit = new UnitImpl(Key.newUuid(), name, this);
        newUnit.setState(Lifecycle.CREATING);
        this.addToUnits(newUnit);
        return newUnit;
    }

    @Override
    public Optional<Unit> getUnit(Key<UUID> id) {
        return Unit.flatten(this.units)
                   .stream()
                   .filter(u -> u.getId()
                                 .equals(id))
                   .findFirst();

    }

    @Override
    public void removeUnit(Unit unit) {
        Unit unitToRemove = getUnit(unit.getId()).orElseThrow(() -> new InvalidUnitException(
                "Unit %s not found in client %s.", unit.getId(), this.getId()));
        this.removeFromUnits(unitToRemove);
        unitToRemove.setClient(null);
        unitToRemove.remove();
    }

    /**
     * Add the given Unit to the collection units.
     *
     * @return true if added
     */
    public boolean addToUnits(Unit aUnit) {
        boolean add = this.units.add(aUnit);
        return add;
    }

    /**
     * Remove the given Unit from the collection units.
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
    public Set<Unit> getUnits() {
        return this.units;
    }

    @Override
    public void setUnits(Set<Unit> aUnits) {
        this.units = aUnits;
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
        return "ClientImpl [name=" + name + "]";
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_CLIENT;
    }
}
