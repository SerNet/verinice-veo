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
package org.veo.core.entity;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.Size;

import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.DeterminedRisk;
import org.veo.core.entity.risk.Impact;
import org.veo.core.entity.risk.ImpactRef;
import org.veo.core.entity.risk.ProbabilityRef;
import org.veo.core.entity.risk.RiskRef;
import org.veo.core.entity.risk.RiskTreatmentOption;

import lombok.With;

/** Serializable container for risk values to be used in a tailoring reference. */
@With
public record RiskTailoringReferenceValues(
    ProbabilityRef specificProbability,
    String specificProbabilityExplanation,
    Map<CategoryRef, CategoryValues> categories) {
  public RiskTailoringReferenceValues {
    categories = Map.copyOf(categories);
  }

  public record CategoryValues(
      ImpactRef specificImpact,
      @Size(max = Impact.EXPLANATION_MAX_LENGTH) String specificImpactExplanation,
      RiskRef userDefinedResidualRisk,
      @Size(max = DeterminedRisk.EXPLANATION_MAX_LENGTH) String residualRiskExplanation,
      Set<RiskTreatmentOption> riskTreatments,
      @Size(max = DeterminedRisk.EXPLANATION_MAX_LENGTH) String riskTreatmentExplanation) {
    public CategoryValues {
      if (riskTreatments == null) {
        riskTreatments = Collections.emptySet();
      } else {
        riskTreatments = Set.copyOf(riskTreatments); // enforce immutability
      }
    }
  }
}
