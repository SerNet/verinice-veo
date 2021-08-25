/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
 * This class is for documentation only. It specifies type-specific comments and
 * examples for a reference to a DomaintemplateReference", description = "The
 * reference to a domaintemplate..
 *
 * It is only supposed to be used in OpenApi annotations and should not be
 * extended and implemented.
 */
@Schema(name = "DomaintemplateReference", description = "The reference to a domaintemplate.")
public interface IdRefCatalogDomainTemplate extends IIdRef {

    @Schema(description = "A friendly human readable title of the referenced domaintemplate.",
            example = "EU GDPR 2016-05-04")
    @Override
    String getDisplayName();

    @Schema(required = true,
            description = "The resource URL of the referenced domain.",
            example = "http://<api.example.org>/api/v1/domaintemplates/<00000000-0000-0000-0000-000000000000>")
    @Override
    String getTargetUri();

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    String getSearchesUri();

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    String getResourcesUri();
}