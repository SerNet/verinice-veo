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
import java.util.UUID;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.SameClientSpecification;

/**
 * A client is the root element of the ownership structure and an access barrier.
 * Data from one client must never be visible to another client.
 * Data from one client must not be referenced by or linked to by data from another client. 
 * No user-manageable configuration or settings must be shared between clients.
 *
 * A client can be used to separate multiple completely disjunct users of the system from each other.
 * 
 */
public class Client {
    
    @NotNull
    private Key<UUID> id;
    
    @NotNull
    @NotBlank(message="The name of a client must not be blank.")
    private String name;
    
    @NotNull
    @Size(min=1, max=1000000, message="A client must be working with at least one domain.")
    private Collection<Domain> domains;
    
    @NotNull
    @Size(min=1, max=1000000, message="A client must have at least one unit.")
    private Collection<Unit> units;

    
    
    public Key<UUID> getId() {
        return id;
    }

    public void setId(Key<UUID> id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Client(Key<UUID> id, String name) {
        this.id = id;
        this.name = name;
    }

    public Collection<Domain> getDomains() {
        return domains;
    }

    public void setDomains(Collection<Domain> domains) {
        this.domains = domains;
    }

    public Collection<Unit> getUnits() {
        return units;
    }

    /**
     * Replaces the collection of top level units for this client with a new collection.
     * Ensures that all units already belong to this client.
     *  
     * @param units
     */
    public void setUnits(Collection<Unit> units) {
        checkSameClient(units);
        this.units = units;
    }

    private void checkSameClient(Collection<Unit> newUnits) {
        if ( !(new SameClientSpecification<EntityLayerSupertype>(this))
                .isSatisfiedBy(newUnits)
        ) {
            throw new ClientBoundaryViolationException("Units from a different client cannot be moved to another client. "
                    + "Attempted operation failed for client: " + this.getName());
        }
        
    }

}
