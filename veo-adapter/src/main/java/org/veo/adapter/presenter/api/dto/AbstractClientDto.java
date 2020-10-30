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
import java.util.Set;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.Patterns;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * Base transfer object for Clients. Contains common data for all Client DTOs.
 */
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
abstract public class AbstractClientDto implements VersionedDto {

    @Schema(description = "The name for the Client.", example = "SerNet GmbH")
    @ToString.Include
    private String name;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this entity was created.",
            example = "1990-12-31T23:59:60Z")
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

    @Schema(description = "The units for the Client.")
    private Set<AbstractUnitDto> units = Collections.emptySet();

    @Schema(description = "The domains for the Client.")
    private Set<AbstractDomainDto> domains = Collections.emptySet();
}
