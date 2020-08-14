/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
package org.veo.adapter.presenter.api.dto.create;

import javax.validation.constraints.NotNull;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceEntityLayerSupertypeOwner;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Unit;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

@Data
public final class CreateGroupDto {
    @NotNull(message = "A name must be present.")
    @Schema(description = "The name for the Asset.",
            example = "<add example here>",
            required = true)
    @ToString.Include
    private String name;

    @NotNull(message = "A owner must be present.")
    @Schema(required = true, implementation = ModelObjectReferenceEntityLayerSupertypeOwner.class)
    private ModelObjectReference<Unit> owner;

    @NotNull(message = "A type must be present.")
    @Schema(description = "The type for the group.", required = true)
    private GroupType type;

}
