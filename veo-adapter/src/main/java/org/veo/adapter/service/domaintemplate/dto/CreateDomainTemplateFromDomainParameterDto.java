/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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
package org.veo.adapter.service.domaintemplate.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "The parameter object ")
@Data
public class CreateDomainTemplateFromDomainParameterDto {
  static final String SEM_VER_PATTERN = "[0-9]+\\.[0-9]+\\.[0-9]+";

  @Schema(
      description = "The version of the domain template, with semantic version",
      example = "1.0.0")
  @NotNull(message = "Domain template version is missing")
  @Size(max = 255)
  @Pattern(regexp = SEM_VER_PATTERN)
  private String version;
}
