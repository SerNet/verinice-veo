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
import javax.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.ModelPackage;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "domain")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class DomainData extends BaseModelObjectData implements NameableData, Domain {

    @NotNull
    @Column(name = "name")
    @ToString.Include
    private String name;
    @Column(name = "abbreviation")
    private String abbreviation;
    @Column(name = "description")
    private String description;
    @Column(name = "active")
    private Boolean active;

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

    public Boolean isActive() {
        return this.active;
    }

    public void setActive(Boolean aActive) {
        this.active = aActive;
    }

    @Override
    public Class<? extends ModelObject> getModelInterface() {
        return Domain.class;
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_DOMAIN;
    }
}
