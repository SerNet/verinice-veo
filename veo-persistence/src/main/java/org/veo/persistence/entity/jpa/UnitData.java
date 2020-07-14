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
package org.veo.persistence.entity.jpa;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.ModelPackage;
import org.veo.core.entity.Unit;

@Entity(name = "unit")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class UnitData extends BaseModelObjectData implements NameAbleData, Unit {

    @NotNull
    @Column(name = "name")
    @ToString.Include
    private String name;
    @Column(name = "abbreviation")
    private String abbreviation;
    @Column(name = "description")
    private String description;
    @NotNull
    // one to one unit-> client
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ClientData.class)
    @JoinColumn(name = "client_id")
    private Client client;
    // one to one unit-> unit
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UnitData.class)
    @JoinColumn(name = "parent_id")
    private Unit parent;
    // many to one unit-> domain
    @Column(name = "domains")
    @ManyToMany(targetEntity = DomainData.class)
    private Set<Domain> domains;

    public String getName() {
        return this.name;
    }

    public void setName(String aName) {
        this.name = aName;
    }

    public String getAbbreviation() {
        return this.abbreviation;
    }

    public void setAbbreviation(String aAbbreviation) {
        this.abbreviation = aAbbreviation;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String aDescription) {
        this.description = aDescription;
    }

    public Client getClient() {
        return this.client;
    }

    public void setClient(Client aClient) {
        this.client = aClient;
    }

    public Unit getParent() {
        return this.parent;
    }

    public void setParent(Unit aParent) {
        this.parent = aParent;
    }

    public Set<Domain> getDomains() {
        return this.domains;
    }

    public void setDomains(Set<Domain> aDomains) {
        this.domains = aDomains;
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
    public Class<? extends ModelObject> getModelInterface() {
        return Unit.class;
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_UNIT;
    }

}
