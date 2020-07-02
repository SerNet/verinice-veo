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
package org.veo.adapter.presenter.api.request;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceAssetDomains;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceAssetOwner;
import org.veo.adapter.presenter.api.response.CustomLinkDto;
import org.veo.adapter.presenter.api.response.CustomPropertiesDto;
import org.veo.core.entity.Domain;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Unit;

@Data
public final class CreateGroupDto {

    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
             flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "ID for new objects must either be null or a valid UUID string following RFC 4122.")
    @Schema(description = "ID must be a valid UUID string following RFC 4122.",
            example = "adf037f1-0089-48ad-9177-92269918758b")
    private String id;

    // TODO Add an example for the API documentation for field name.
    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this version of the entity was saved.",
            example = "1990-12-31T23:59:60Z")
    @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2}[Tt]\\d{2}:\\d{2}:\\d{2}(\\.\\d{0,2})?([zZ]|[+-]\\d{2}:\\d{2}))")
    private String validFrom;

    // TODO Add an example for the API documentation for field name.
    @Schema(description = "A timestamp acc. to RFC 3339 specifying the point in time when this version of the entity was superseded "
            + "by a newer version or deleted. Empty if this is the current version.",
            example = "1990-12-31T23:59:60Z")
    @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2}[Tt]\\d{2}:\\d{2}:\\d{2}(\\.\\d{0,2})?([zZ]|[+-]\\d{2}:\\d{2}))")
    private String validUntil;

    @NotNull(message = "A name must be present.")
    // TODO Add an example for the API documentation for field name.
    @Schema(description = "The name for the Asset.",
            example = "<add example here>",
            required = true)
    private String name;

    // TODO Add an example for the API documentation for field abbreviation.
    @Schema(description = "The abbreviation for the Asset.",
            example = "<add example here>",
            required = false)
    private String abbreviation;

    // TODO Add an example for the API documentation for field description.
    @Schema(description = "The description for the Asset.",
            example = "<add example here>",
            required = false)
    private String description;

    @NotNull(message = "A type must be present.")
    // TODO Add an example for the API documentation for field type in Asset.
    @Schema(description = "The type for the group.",
            example = "<add example here>",
            required = true)
    public GroupType type;

    // TODO Add an example for the API documentation for field domains in Asset.
    @ArraySchema(schema = @Schema(implementation = ModelObjectReferenceAssetDomains.class))
    private List<ModelObjectReference<Domain>> domains = Collections.emptyList();

    // TODO Add an example for the API documentation for field links.
    @Schema(description = "The links for the Asset.",
            example = "<add example here>",
            required = false)
    private List<CustomLinkDto> links = Collections.emptyList();

    // TODO Add an example for the API documentation for field customAspects.
    @Schema(description = "The customAspects for the Asset.",
            example = "<add example here>",
            required = false)
    private List<CustomPropertiesDto> customAspects = Collections.emptyList();

    @NotNull(message = "A owner must be present.")
    // TODO Add an example for the API documentation for field owner in Asset.
    @Schema(required = true, implementation = ModelObjectReferenceAssetOwner.class)
    private ModelObjectReference<Unit> owner;

}
