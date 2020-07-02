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
package org.veo.core.entity.groups;

import java.util.Collections;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.ModelPackage;
import org.veo.core.entity.Person;
import org.veo.core.entity.Unit;
import org.veo.core.entity.impl.BaseModelGroup;
import org.veo.core.entity.impl.PersonImpl;

/**
 * The group for Person objects.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class PersonGroup extends BaseModelGroup<Person> implements Person, ModelGroup<Person> {
    private Set<Person> members = Collections.emptySet();
    @ToString.Include
    private Person instance = new PersonImpl(null, null, null);

    @Override
    public Person getInstance() {
        return instance;
    }

    @Override
    public void setInstance(Person instance) {
        this.instance = instance;
    }

    @Override
    public Set<Person> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<Person> members) {
        this.members = members;
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_PERSON + "_GROUP";
    }

    @Override
    public String getName() {
        return instance.getName();
    }

    public void setName(String aName) {
        instance.setName(aName);
    }

    @Override
    public String getAbbreviation() {
        return instance.getAbbreviation();
    }

    public void setAbbreviation(String aAbbreviation) {
        instance.setAbbreviation(aAbbreviation);
    }

    @Override
    public String getDescription() {
        return instance.getDescription();
    }

    public void setDescription(String aDescription) {
        instance.setDescription(aDescription);
    }

    @Override
    public boolean addToDomains(Domain aDomain) {
        return instance.addToDomains(aDomain);
    }

    @Override
    public boolean removeFromDomains(Domain aDomain) {
        return instance.removeFromDomains(aDomain);
    }

    @Override
    public Set<Domain> getDomains() {
        return instance.getDomains();
    }

    public void setDomains(Set<Domain> aDomains) {
        instance.setDomains(aDomains);
    }

    @Override
    public boolean addToLinks(CustomLink aCustomLink) {
        return instance.addToLinks(aCustomLink);
    }

    @Override
    public boolean removeFromLinks(CustomLink aCustomLink) {
        return instance.removeFromLinks(aCustomLink);
    }

    @Override
    public Set<CustomLink> getLinks() {
        return instance.getLinks();
    }

    public void setLinks(Set<CustomLink> aLinks) {
        instance.setLinks(aLinks);
    }

    @Override
    public boolean addToCustomAspects(CustomProperties aCustomProperties) {
        return instance.addToCustomAspects(aCustomProperties);
    }

    @Override
    public boolean removeFromCustomAspects(CustomProperties aCustomProperties) {
        return instance.removeFromCustomAspects(aCustomProperties);
    }

    @Override
    public Set<CustomProperties> getCustomAspects() {
        return instance.getCustomAspects();
    }

    public void setCustomAspects(Set<CustomProperties> aCustomAspects) {
        instance.setCustomAspects(aCustomAspects);
    }

    @Override
    public Unit getOwner() {
        return instance.getOwner();
    }

    public void setOwner(Unit aOwner) {
        instance.setOwner(aOwner);
    }

}
