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
package org.veo.adapter.presenter.api.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceDomains;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceOwner;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementOwner;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * Base transfer object for EntityLayerSupertypes. Contains common data for all
 * EntityLayerSupertype DTOs.
 */
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class AbstractEntityLayerSupertypeDto implements EntityLayerSupertypeDto {

    @NotNull(message = "A name must be present.")
    @Schema(description = "The name for the EntityLayerSupertype.",
            example = "Lock doors",
            required = true)
    @ToString.Include
    private String name;

    @Schema(description = "The abbreviation for the EntityLayerSupertype.",
            example = "Lock doors",
            required = false)
    private String abbreviation;

    @Schema(description = "The description for the EntityLayerSupertype.",
            example = "Lock doors",
            required = false)
    private String description;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this entity was created.",
            example = "1990-12-31T23:59:60Z",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Pattern(regexp = Patterns.DATETIME)
    private String createdAt;

    @Schema(description = "The username of the user who created this object.",
            example = "jane_doe",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this version of the entity was saved.",
            example = "1990-12-31T23:59:60Z",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Pattern(regexp = Patterns.DATETIME)
    private String updatedAt;

    @Schema(description = "The username of the user who last updated this object.",
            example = "jane_doe",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String updatedBy;

    @JsonIgnore
    private long version;

    @ArraySchema(schema = @Schema(implementation = ModelObjectReferenceDomains.class))
    @Valid
    private Set<ModelObjectReference<Domain>> domains = Collections.emptySet();

    @NotNull(message = "An owner must be present.")
    @Schema(required = true, implementation = ModelObjectReferenceOwner.class)
    private ModelObjectReference<ElementOwner> owner;

    @Valid
    @Schema(description = "Custom relations which do not affect the behavior.",
            title = "CustomLink")
    private Map<String, List<CustomLinkDto>> links = Collections.emptyMap();

    @Valid
    @Schema(description = "A custom property which is determined by the requested entity schema - see '/schemas'",
            title = "CustomAspect")
    private Map<String, CustomPropertiesDto> customAspects = Collections.emptyMap();

    @Valid
    @Schema(description = "The sub type this entity has in each domain. Domain ID is key, sub type is value.",
            title = "SubType")
    private Map<String, String> subType = Collections.emptyMap();

    @Schema(description = "Entity type identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private String type;
}
