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

import java.util.UUID;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelPackage;

/**
 * The domain should be referenced by the domain objects if applicable. It
 * defines a standard, a best practice or a company-specific context.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class DomainImpl extends BaseModelObject implements Domain {

    @NotNull
    @ToString.Include
    private String name;
    private String abbreviation;
    private String description;
    private Boolean active;

    public DomainImpl(@NotNull Key<UUID> id, String name) {
        super(id);
        this.name = name;
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
    public Boolean isActive() {
        return this.active;
    }

    @Override
    public void setActive(Boolean aActive) {
        this.active = aActive;
    }

    @Override
    public String toString() {
        return "DomainImpl [name=" + name + ",abbreviation=" + abbreviation + ",description="
                + description + ",active=" + active + "]";
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_DOMAIN;
    }
}
