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

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Unit;

@Entity(name = "entitylayersupertype")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class EntityLayerSupertypeData extends BaseModelObjectData
        implements NameAbleData, EntityLayerSupertype {

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
    @ManyToMany(targetEntity = DomainData.class, fetch = FetchType.LAZY)
    private Set<Domain> domains;
    // many to one entitylayersupertype-> customlink
    @Column(name = "links")
    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = CustomLinkData.class,
               fetch = FetchType.LAZY)
    private Set<CustomLink> links;
    // many to one entitylayersupertype-> customproperties
    @Column(name = "customaspects")
    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = CustomPropertiesData.class,
               fetch = FetchType.LAZY)
    private Set<CustomProperties> customAspects;
    @NotNull
    // one to one entitylayersupertype-> unit
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UnitData.class)
    @JoinColumn(name = "owner_id")
    private Unit owner;

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

    public Set<Domain> getDomains() {
        return this.domains;
    }

    public void setDomains(Set<Domain> aDomains) {
        this.domains = aDomains;
    }

    public Set<CustomLink> getLinks() {
        return this.links;
    }

    public void setLinks(Set<CustomLink> aLinks) {
        this.links = aLinks;
    }

    public Set<CustomProperties> getCustomAspects() {
        return this.customAspects;
    }

    public void setCustomAspects(Set<CustomProperties> aCustomAspects) {
        this.customAspects = aCustomAspects;
    }

    public Unit getOwner() {
        return this.owner;
    }

    public void setOwner(Unit aOwner) {
        this.owner = aOwner;
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

    /**
     * Add the given CustomLink to the collection links. opposite of
     * EntityLayerSupertype.source
     *
     * @return true if added
     */
    public boolean addToLinks(CustomLink aCustomLink) {
        boolean add = this.links.add(aCustomLink);
        return add;
    }

    /**
     * Remove the given CustomLink from the collection links. opposite of
     * EntityLayerSupertype.source
     *
     * @return true if removed
     */
    public boolean removeFromLinks(CustomLink aCustomLink) {
        boolean remove = this.links.remove(aCustomLink);
        return remove;
    }

    /**
     * Add the given CustomProperties to the collection customAspects.
     *
     * @return true if added
     */
    public boolean addToCustomAspects(CustomProperties aCustomProperties) {
        boolean add = this.customAspects.add(aCustomProperties);
        return add;
    }

    /**
     * Remove the given CustomProperties from the collection customAspects.
     *
     * @return true if removed
     */
    public boolean removeFromCustomAspects(CustomProperties aCustomProperties) {
        boolean remove = this.customAspects.remove(aCustomProperties);
        return remove;
    }

}
