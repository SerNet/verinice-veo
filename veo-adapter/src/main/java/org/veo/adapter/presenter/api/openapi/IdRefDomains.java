/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.presenter.api.openapi;

import org.veo.adapter.presenter.api.common.IIdRef;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Swagger documentation for the reference 'domains': The domains the entity is
 * part of.
 */
@Schema(name = "DomainsReference", description = "A reference to a domain")
public interface IdRefDomains extends IIdRef {

    // TODO: update reference doc for DomainsReference

    @Schema(description = "A friendly human readable title of the referenced domain.",
            example = "ISO 27001:2013",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    String getDisplayName();

    @Schema(required = true,
            description = "The resource URL of the referenced domains.",
            example = "http://<api.example.org>/api/v1/domain/<00000000-0000-0000-0000-000000000000>",
            format = "uri")
    @Override
    String getTargetUri();

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    String getSearchesUri();

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    String getResourcesUri();
}
