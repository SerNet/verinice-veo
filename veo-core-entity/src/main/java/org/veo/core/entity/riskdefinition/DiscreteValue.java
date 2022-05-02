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
package org.veo.core.entity.riskdefinition;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.Constraints;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A discrete value represents a value in the risk matrix or a level of a DimensionDefinition. It
 * defines the basic values like name and html-color. The ordinal value is managed by the contained
 * object and the same as the position in the corresponding list. The identity aka equals and
 * hashcode are defined by id and name.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DiscreteValue {
  public DiscreteValue(
      @NotNull(message = "A name must be present.") @Size(max = 120) String name,
      @Size(max = 120) String abbreviation,
      @Size(max = 120) String description,
      @Size(max = 255) String htmlColor) {
    super();
    this.name = name;
    this.abbreviation = abbreviation;
    this.description = description;
    this.htmlColor = htmlColor;
  }

  @EqualsAndHashCode.Include @ToString.Include private int ordinalValue;

  @NotNull(message = "A name must be present.")
  @Size(max = Constraints.DEFAULT_CONSTANT_MAX_LENGTH)
  @EqualsAndHashCode.Include
  @ToString.Include
  private String name;

  @Size(max = Constraints.DEFAULT_CONSTANT_MAX_LENGTH)
  private String abbreviation;

  @Size(max = Constraints.DEFAULT_CONSTANT_MAX_LENGTH)
  private String description;

  @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH)
  @ToString.Include
  private String htmlColor;
}
