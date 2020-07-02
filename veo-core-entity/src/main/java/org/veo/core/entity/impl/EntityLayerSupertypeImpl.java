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
import lombok.ToString;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelPackage;
import org.veo.core.entity.Unit;

/**
 * The abstract base model class. Used to prevent duplicating common methods in
 * model layer objects.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class EntityLayerSupertypeImpl extends BaseModelObject
        implements EntityLayerSupertype {

    @NotNull
    @ToString.Include
    private String name;
    private String abbreviation;
    private String description;
    private Set<Domain> domains = new HashSet<>();
    private Set<CustomLink> links = new HashSet<>();
    private Set<CustomProperties> customAspects = new HashSet<>();
    @NotNull
    private Unit owner;

    public EntityLayerSupertypeImpl(@NotNull Key<UUID> id, String name, Unit owner) {
        super(id);
        this.name = name;
        this.owner = owner;
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
    public Set<Domain> getDomains() {
        return this.domains;
    }

    @Override
    public void setDomains(Set<Domain> aDomains) {
        this.domains = aDomains;
    }

    @Override
    public Set<CustomLink> getLinks() {
        return this.links;
    }

    @Override
    public void setLinks(Set<CustomLink> aLinks) {
        this.links = aLinks;
    }

    @Override
    public Set<CustomProperties> getCustomAspects() {
        return this.customAspects;
    }

    @Override
    public void setCustomAspects(Set<CustomProperties> aCustomAspects) {
        this.customAspects = aCustomAspects;
    }

    @Override
    public Unit getOwner() {
        return this.owner;
    }

    @Override
    public void setOwner(Unit aOwner) {
        this.owner = aOwner;
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_ENTITYLAYERSUPERTYPE;
    }
}
