/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Urs Zeidler
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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.core.entity.NameAbbreviationAndDescription;
import org.veo.core.entity.Translated;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DomainMetadataDto {

  @NotNull
  @Schema(
      description = "The name / standard for the Domain.",
      accessMode = Schema.AccessMode.READ_ONLY,
      example = "ISO 27001")
  private String name;

  @Schema(
      description = "The organization that published a standard",
      example = "ISO",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String authority;

  @Schema(
      description =
          "A timestamp acc. to RFC 3339 specifying when this domain was created / imported.",
      example = "1990-12-31T23:59:60Z",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Pattern(regexp = Patterns.DATETIME)
  private String createdAt;

  @NotNull
  @Schema(
      description = "Template version in Semantic Versioning 2.0.0 format",
      example = "1.0.0",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String templateVersion;

  @Schema(description = "The translations for the domain.")
  private Translated<NameAbbreviationAndDescription> translations = new Translated<>();
}
