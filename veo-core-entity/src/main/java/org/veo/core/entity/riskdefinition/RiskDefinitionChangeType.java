/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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

import java.util.Set;

public enum RiskDefinitionChangeType {
  NEW_RISK_DEFINITION,
  IMPACT_LINKS,
  TRANSLATION_DIFF,
  COLOR_DIFF,
  RISK_MATRIX_DIFF,
  RISK_MATRIX_RESIZE,
  IMPLEMENTATION_STATE_LIST_RESIZE,
  IMPACT_LIST_RESIZE,
  PROBABILITY_LIST_RESIZE,
  CATEGORY_LIST_RESIZE,
  RISK_VALUE_LIST_RESIZE;

  static Set<RiskDefinitionChangeType> recalulateRiskChanges() {
    return Set.of(RISK_MATRIX_DIFF, RISK_MATRIX_RESIZE);
  }

  static Set<RiskDefinitionChangeType> migrateRiskChanges() {
    return Set.of(CATEGORY_LIST_RESIZE, RISK_MATRIX_RESIZE);
  }

  public static boolean requiresRiskRecalculation(Set<RiskDefinitionChangeType> detectedChanges) {
    return detectedChanges.stream().anyMatch(t -> recalulateRiskChanges().contains(t));
  }

  public static boolean requiresMigration(Set<RiskDefinitionChangeType> detectedChanges) {
    return detectedChanges.stream().anyMatch(t -> migrateRiskChanges().contains(t));
  }

  public static boolean requiresImpactInheritanceRecalculation(
      Set<RiskDefinitionChangeType> detectedChanges) {
    return detectedChanges.contains(IMPACT_LINKS);
  }
};
