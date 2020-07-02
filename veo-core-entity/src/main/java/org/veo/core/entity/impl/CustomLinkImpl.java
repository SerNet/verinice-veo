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

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelPackage;

/**
 * A link connects two model objects. This link serve only documentation
 * purpose.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class CustomLinkImpl extends CustomPropertiesImpl implements CustomLink {

    @NotNull
    @ToString.Include
    private String name;
    private String abbreviation;
    private String description;
    @NotNull
    @ToString.Include
    private EntityLayerSupertype target;
    @NotNull
    @ToString.Include
    private EntityLayerSupertype source;

    public CustomLinkImpl(@NotNull Key<UUID> id, String name, EntityLayerSupertype target,
            EntityLayerSupertype source) {
        super(id);
        this.name = name;
        this.target = target;
        this.source = source;
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
    public EntityLayerSupertype getTarget() {
        return this.target;
    }

    @Override
    public void setTarget(EntityLayerSupertype aTarget) {
        this.target = aTarget;
    }

    @Override
    public EntityLayerSupertype getSource() {
        return this.source;
    }

    /**
     * opposite of CustomLink.links
     **/
    @Override
    public void setSource(EntityLayerSupertype aSource) {
        this.source = aSource;
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_CUSTOMLINK;
    }
}
