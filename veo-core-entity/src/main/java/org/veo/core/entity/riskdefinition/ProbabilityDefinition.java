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
import java.util.Map;
import java.util.Optional;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** A special dimension defining the probability levels. */
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class ProbabilityDefinition extends DimensionDefinition {

  public ProbabilityDefinition(
      Map<String, Map<String, String>> translations, List<ProbabilityLevel> levels) {
    super(DIMENSION_PROBABILITY, translations);
    this.levels = levels;
    initLevel(levels);
  }

  public ProbabilityDefinition(List<ProbabilityLevel> levels) {
    super(DIMENSION_PROBABILITY);
    this.levels = levels;
    initLevel(levels);
  }

  public ProbabilityDefinition() {
    super(DIMENSION_PROBABILITY);
  }

  @EqualsAndHashCode.Include private List<ProbabilityLevel> levels = new ArrayList<>();

  public Optional<ProbabilityLevel> getLevel(int ordinalValue) {
    return levels.stream().filter(l -> l.getOrdinalValue() == ordinalValue).findFirst();
  }
}
