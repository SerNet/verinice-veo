/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.Constraints;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractProfileDto extends AbstractVersionedSelfReferencingDto
    implements ModelDto {

  @Schema(
      description = "Human readable profile name in the profile's language",
      example = "example organization",
      requiredMode = REQUIRED)
  @ToString.Include
  @NotNull(message = "A name must be present.")
  @Size(max = Nameable.NAME_MAX_LENGTH)
  private String name;

  @Size(max = Nameable.DESCRIPTION_MAX_LENGTH)
  @Schema(
      description = "The description of the Profile.",
      example = "An example organization modelled for itgs")
  private String description;

  @Schema(description = "The language  of the Profile.", example = "de_DE")
  @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH)
  private String language;

  @Override
  public Class<Profile> getModelInterface() {
    return Profile.class;
  }
}
