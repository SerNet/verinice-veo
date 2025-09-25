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

import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.veo.adapter.presenter.api.Patterns;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(makeFinal = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbstractVersionedDto {
  @Schema(
      description = "A timestamp acc. to RFC 3339 specifying when this entity was created.",
      example = "1990-12-31T23:59:60Z")
  @Pattern(regexp = Patterns.DATETIME)
  @JsonProperty(access = Access.READ_ONLY)
  private String createdAt;

  @Schema(
      description = "The username of the user who created this object.",
      example = "jane_doe",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String createdBy;

  @Schema(
      description = "A timestamp acc. to RFC 3339 specifying when this entity was created.",
      example = "1990-12-31T23:59:60Z")
  @Pattern(regexp = Patterns.DATETIME)
  @JsonProperty(access = Access.READ_ONLY)
  private String updatedAt;

  @Schema(
      description = "The username of the user who last updated this object.",
      example = "jane_doe",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String updatedBy;

  @JsonIgnore private long version;
}
