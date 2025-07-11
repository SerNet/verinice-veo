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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.veo.core.entity.Translated;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** A special dimension defining the implementation levels. */
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
// TODO #3935: when all domains are migrated to the new structure this ignoreProperties can be
// removed
@JsonIgnoreProperties({"name", "abbreviation", "description"})
public class ImplementationStateDefinition extends DimensionDefinition {
  public ImplementationStateDefinition(List<CategoryLevel> levels) {
    super(DIMENSION_IMPLEMENTATION_STATE);
    this.levels = levels;
    initLevel(levels);
  }

  public ImplementationStateDefinition(
      Translated<DiscreteValue.NameAbbreviationAndDescription> translations,
      List<CategoryLevel> levels) {
    super(DIMENSION_IMPLEMENTATION_STATE, translations);
    this.levels = levels;
    initLevel(levels);
  }

  public ImplementationStateDefinition() {
    super(DIMENSION_IMPLEMENTATION_STATE);
  }

  public void setLevels(List<CategoryLevel> levels) {
    this.levels = levels;
    initLevel(levels);
  }

  @EqualsAndHashCode.Include private List<CategoryLevel> levels = new ArrayList<>();

  public Optional<CategoryLevel> getLevel(int ordinalValue) {
    return levels.stream().filter(l -> l.getOrdinalValue() == ordinalValue).findFirst();
  }
}
