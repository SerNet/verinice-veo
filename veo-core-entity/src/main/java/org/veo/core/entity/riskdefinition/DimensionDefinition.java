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

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.Constraints;
import org.veo.core.entity.Translated;
import org.veo.core.entity.TranslationProvider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The basic class for a dimension definition. A dimension definition has an unique id and can work
 * with {@link DiscreteValue} as level.
 */
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode
@NoArgsConstructor
@Data
@AllArgsConstructor
public abstract class DimensionDefinition<T extends DiscreteValue>
    implements TranslationProvider<DiscreteValue.NameAbbreviationAndDescription> {
  protected static final String DIMENSION_PROBABILITY = "Prob";
  protected static final String DIMENSION_IMPLEMENTATION_STATE = "Ctr";

  @NotNull(message = "An id must be present.")
  @Size(max = Constraints.DEFAULT_CONSTANT_MAX_LENGTH)
  @ToString.Include
  private String id;

  @NotNull @Valid
  private Translated<DiscreteValue.NameAbbreviationAndDescription> translations =
      new Translated<>();

  public DimensionDefinition(String id) {
    this.id = id;
  }

  /** Initialize the ordinal value of each DiscreteValue in the list. */
  static void initLevel(List<? extends DiscreteValue> discreteValues) {
    for (int i = 0; i < discreteValues.size(); i++) {
      discreteValues.get(i).setOrdinalValue(i);
    }
  }

  public abstract List<T> getLevels();
}
