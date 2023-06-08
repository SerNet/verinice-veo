/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import org.veo.core.entity.Person;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(
    title = "Person",
    description =
        "Person, role or group of persons - this DTO represents a person from the viewpoint of a domain and contains both basic and domain-specific properties.")
public abstract class AbstractPersonInDomainDto
    extends AbstractCompositeElementInDomainDto<Person> {

  @Override
  @Schema(description = "Full name", example = "Viola Seher")
  public String getName() {
    return super.getName();
  }

  @Override
  @Schema(description = "Short human-readable identifier (not unique)", example = "VS")
  public String getAbbreviation() {
    return super.getAbbreviation();
  }

  @Override
  @Schema(example = "Internal data protection officer at Data Inc.")
  public String getDescription() {
    return super.getDescription();
  }

  @Override
  @Schema(description = "Unique human-readable identifier", example = "PER-35")
  public String getDesignator() {
    return super.getDesignator();
  }

  @Override
  public Class<Person> getModelInterface() {
    return Person.class;
  }
}
