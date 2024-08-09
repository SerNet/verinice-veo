/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.Ref;
import org.veo.adapter.presenter.api.response.IdentifiableDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

@Data
@Schema(accessMode = Schema.AccessMode.READ_ONLY)
public class DomainTemplateMetadataDto implements IdentifiableDto {

  @JsonIgnore
  @Getter(AccessLevel.NONE)
  private Ref selfRef;

  @JsonProperty(value = "_self", access = READ_ONLY)
  @Schema(description = "Absolute target URL of this domain template", format = "uri")
  public String getSelf() {
    return Optional.ofNullable(selfRef).map(Ref::getTargetUri).orElse(null);
  }

  @ToString.Include private UUID id;

  @Schema(description = "The name / standard for the DomainTemplate.", example = "ISO 27001")
  private String name;

  @Schema(
      description =
          "A timestamp acc. to RFC 3339 specifying when this template was created / imported.",
      example = "1990-12-31T23:59:60Z")
  @Pattern(regexp = Patterns.DATETIME)
  private String createdAt;

  @NotNull(message = "A templateVersion must be present.")
  @Schema(description = "The version for the DomainTemplate.", example = "1.0.0")
  private String templateVersion;
}
