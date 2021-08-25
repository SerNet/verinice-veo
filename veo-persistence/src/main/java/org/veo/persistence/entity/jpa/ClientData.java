/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.persistence.entity.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.specification.ClientBoundaryViolationException;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "client")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class ClientData extends IdentifiableVersionedData implements Client, Nameable {

    @Id
    @ToString.Include
    private String dbId;

    @Column(name = "name")
    @ToString.Include
    private String name;

    @Column(name = "abbreviation")
    private String abbreviation;

    @Column(name = "description", length = Nameable.DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "domains")
    @OneToMany(mappedBy = "owner",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = DomainData.class)
    @Valid
    final private Set<Domain> domains = new HashSet<>();

    public void setDomains(Set<Domain> newDomains) {
        domains.clear();
        newDomains.forEach(domain -> domain.setOwner(this));
        domains.addAll(newDomains);
    }

    /**
     * Add the given Domain to the collection domains.
     *
     * @return true if added
     */
    public boolean addToDomains(Domain aDomain) {
        aDomain.setOwner(this);
        return this.domains.add(aDomain);
    }

    /**
     * Remove the given Domain from the collection domains.
     *
     * @return true if removed
     */
    public boolean removeFromDomains(Domain aDomain) {
        if (aDomain.getOwner()
                   .equals(this))
            throw new ClientBoundaryViolationException(aDomain, this);
        aDomain.setOwner(null);
        return this.domains.remove(aDomain);
    }
}
