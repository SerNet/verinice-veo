/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.adapter.presenter.api.dto;

import java.util.Optional;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.Ref;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

/**
 * Implements all members of {@link Versioned} and has a self reference.
 *
 * TODO VEO-902 rename back to AbstractVersionedDto when all Versioned types can
 * have a self reference.
 */
@Data
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class AbstractVersionedSelfReferencingDto implements VersionedDto {

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private Ref selfRef;

    @JsonProperty(value = "_self", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "A valid reference to this resource.", format = "uri", required = true)
    public String getSelf() {
        return Optional.ofNullable(selfRef)
                       .map(Ref::getTargetUri)
                       .orElse(null);
    }

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this entity was created.",
            example = "1990-12-31T23:59:60Z",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Pattern(regexp = Patterns.DATETIME)
    private String createdAt;

    @Schema(description = "The username of the user who created this object.",
            example = "jane_doe",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this entity was created.",
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
}
