/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.validation.EntityValidatorRegistry;

/**
 * An organizational unit. Units may have sub-units.
 * Every entity object is assigned to exactly one unit
 * at all times. When the unit is deleted, all its entities will be deleted as well.
 * 
 * A unit defines object ownership. Small and medium organizations may
 * just have one unit. Large enterprises may have multiple units for different 
 * subsidiaries. Service providers might have one unit for each client that
 * is using the software.
 * 
 * The <code>EntityGroup</code> object is much more flexible and the
 * preferred choice to group entities together in most cases.
 * 
 *  @see EntityGroup
 * 
 *
 */
public class Unit {

    @NotNull
    private Key<UUID> id;
    
    @NotNull
    @NotBlank(message="The name of the unit may not be blank.")
    private String name;
    
    @NotNull
    @Size(min=0, max=1000000)
    private Set<Unit> subUnits;
    
    @NotNull
    private Client client;

    

    public Unit(Key<UUID> id, String name, Client client) {
        this.id = id;
        this.name = name;
        this.client = client;
        this.subUnits = new HashSet<>();
        validate();
    }
    
    
    private void validate() {
        EntityValidatorRegistry.getValidator().validate(this);
    }


    public Unit(@NotNull Key<UUID> id,
            @NotNull @NotBlank(message = "The name of the unit may not be blank.") String name) {
        this.id = id;
        this.name = name;
    }



    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<Unit> getSubUnits() {
        return subUnits;
    }

    public void setSubUnits(Set<Unit> subUnits) {
        this.subUnits = subUnits;
    }

    
}
