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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "entitylayersupertype")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class EntityLayerSupertypeData extends BaseModelObjectData implements NameAbleData {

    @NotNull
    @Column(name = "name")
    @ToString.Include
    private String name;
    @Column(name = "abbreviation")
    private String abbreviation;
    @Column(name = "description")
    private String description;
    // many to one entitylayersupertype-> domain
    @Column(name = "domains")
    @ManyToMany
    private Set<DomainData> domains;
    // many to one entitylayersupertype-> customlink
    @Column(name = "links")
    // TODO review mappedBy attribute for VEO-161
    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CustomLinkData> links;
    // many to one entitylayersupertype-> customproperties
    @Column(name = "customaspects")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CustomPropertiesData> customAspects;
    @NotNull
    // one to one entitylayersupertype-> unit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UnitData owner;

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

    public Set<DomainData> getDomains() {
        return this.domains;
    }

    public void setDomains(Set<DomainData> aDomains) {
        this.domains = aDomains;
    }

    public Set<CustomLinkData> getLinks() {
        return this.links;
    }

    public void setLinks(Set<CustomLinkData> aLinks) {
        this.links = aLinks;
    }

    public Set<CustomPropertiesData> getCustomAspects() {
        return this.customAspects;
    }

    public void setCustomAspects(Set<CustomPropertiesData> aCustomAspects) {
        this.customAspects = aCustomAspects;
    }

    public UnitData getOwner() {
        return this.owner;
    }

    public void setOwner(UnitData aOwner) {
        this.owner = aOwner;
    }

}
