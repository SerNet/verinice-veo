/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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
package org.veo.adapter.presenter.api.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

import org.veo.adapter.presenter.api.common.IModelObjectReference;

@Schema(name = "EntityLayerSupertypeOwner",
        description = "A reference to the unit containing this entity.")
public interface ModelObjectReferenceEntityLayerSupertypeOwner extends IModelObjectReference {

    @Schema(description = "A friendly human readable title of the referenced unit.",
            example = "My Unit")
    @Override
    String getDisplayName();

    @Schema(required = true,
            description = "The resource URL of the referenced unit.",
            example = "http://<api.example.org>/api/v1/unit/<00000000-0000-0000-0000-000000000000>")
    @Override
    String getHref();
}
