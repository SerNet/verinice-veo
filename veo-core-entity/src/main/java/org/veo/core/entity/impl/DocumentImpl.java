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

import org.veo.core.entity.Document;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelPackage;
import org.veo.core.entity.Unit;

/**
 * A Document is a specification, a contract or a reference.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class DocumentImpl extends EntityLayerSupertypeImpl implements Document {

    public DocumentImpl(@NotNull Key<UUID> id, String name, Unit owner) {
        super(id, name, owner);
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_DOCUMENT;
    }
}
