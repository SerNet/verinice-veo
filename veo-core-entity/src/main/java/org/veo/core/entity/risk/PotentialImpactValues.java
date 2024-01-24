/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade
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
package org.veo.core.entity.risk;

import java.util.Map;

/**
 * This represents a collection of impact values. The impact values are categorized by a {@link
 * CategoryRef} as dimension index.
 */
public interface PotentialImpactValues {
  /** Map of categorized specific {@link CategoryRef} impact values */
  Map<String, ImpactRef> getPotentialImpacts();

  /**
   * Map of categorized {@link CategoryRef} impact values calculated by the high-water mark method.
   */
  Map<String, ImpactRef> getPotentialImpactsCalculated();

  /** For each category, the reason for the chosen specific potential impact. */
  Map<String, ImpactReason> getPotentialImpactReasons();

  /** For each category, any optional explanations for the potential impact values. */
  Map<String, String> getPotentialImpactExplanations();
}
