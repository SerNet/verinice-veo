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
package org.veo.adapter.presenter.api.dto.create;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.Domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CreateDomainDto {
  @NotNull
  @Schema(description = "Domain name / standard", example = "Data protection")
  @Size(min = 1, max = Domain.NAME_MAX_LENGTH)
  private String name;

  @Schema(example = "Data prot.")
  @Size(min = 1, max = Domain.ABBREVIATION_MAX_LENGTH)
  private String abbreviation;

  @Schema(example = "Everything around data protection")
  @Size(min = 1, max = Domain.DESCRIPTION_MAX_LENGTH)
  private String description;

  @NotNull
  @Schema(description = "The organization that publishes a standard", example = "ISO")
  @Size(min = 1, max = Domain.AUTHORITY_MAX_LENGTH)
  private String authority;
}
