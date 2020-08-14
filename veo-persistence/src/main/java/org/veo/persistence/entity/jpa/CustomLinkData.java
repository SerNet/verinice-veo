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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.ModelObject;

import lombok.EqualsAndHashCode;

@Entity(name = "customlink")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class CustomLinkData extends CustomPropertiesData implements NameAbleData, CustomLink {

    @NotNull
    @Column(name = "name")
    private String name;
    @Column(name = "abbreviation")
    private String abbreviation;
    @Column(name = "description")
    private String description;
    @NotNull
    // one to one customlink-> entitylayersupertype
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = EntityLayerSupertypeData.class)
    @JoinColumn(name = "target_id")
    private EntityLayerSupertype target;
    @NotNull
    // one to one customlink-> entitylayersupertype
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = EntityLayerSupertypeData.class)
    @JoinColumn(name = "source_id")
    private EntityLayerSupertype source;

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

    public EntityLayerSupertype getTarget() {
        return this.target;
    }

    public void setTarget(EntityLayerSupertype aTarget) {
        this.target = aTarget;
    }

    public EntityLayerSupertype getSource() {
        return this.source;
    }

    public void setSource(EntityLayerSupertype aSource) {
        this.source = aSource;
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
        return CustomLink.class;
    }

}
