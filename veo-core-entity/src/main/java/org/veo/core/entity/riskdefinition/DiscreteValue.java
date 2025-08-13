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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.Constraints;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Translated;
import org.veo.core.entity.TranslationProvider;

import lombok.AllArgsConstructor;
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
@EqualsAndHashCode
@AllArgsConstructor
public class DiscreteValue
    implements TranslationProvider<DiscreteValue.NameAbbreviationAndDescription> {

  public DiscreteValue(@Size(max = 255) String htmlColor) {
    super();
    this.htmlColor = htmlColor;
  }

  private int ordinalValue;

  @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH)
  @ToString.Include
  private String htmlColor;

  @ToString.Exclude @NotNull @Valid
  private Translated<NameAbbreviationAndDescription> translations = new Translated<>();

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class NameAbbreviationAndDescription implements Nameable {
    private String name;
    private String abbreviation;
    private String description;
  }
}
