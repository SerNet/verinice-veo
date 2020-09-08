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
package org.veo.adapter.presenter.api.openapi;

import org.veo.adapter.presenter.api.common.IModelObjectReference;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Swagger documentation for the reference 'domains' of 'Control': A model
 * object can have several domains. No domain would mean the default one.
 */

@Schema(name = "ControlDomainsReference",
        description = "A reference to the unit containing this entity.")

public interface ModelObjectReferenceControlDomains extends IModelObjectReference {

    // TODO: update reference doc for ControlDomains

    @Schema(description = "The domains of the control.", example = "ISO 27001:2013")
    @Override
    String getDisplayName();

    @Schema(required = true,
            description = "The resource URL of the referenced domains.",
            example = "http://<api.example.org>/api/v1/domain/<00000000-0000-0000-0000-000000000000>")
    @Override
    String getTargetUri();

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    String getSearchesUri();

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    String getResourcesUri();

}
