/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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

import static org.veo.core.entity.Nameable.DESCRIPTION_MAX_LENGTH;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The probability of a risk event occurring.
 *
 * @see org.veo.core.entity.riskdefinition.RiskDefinition
 */
@Valid
@Schema(description = "A collection of probability values.")
public interface Probability {
  @Schema(
      description =
          "The potential probability derived from the scenario associated with this risk.",
      accessMode = Schema.AccessMode.READ_ONLY,
      example = "1",
      minimum = "0")
  ProbabilityRef getPotentialProbability();

  void setPotentialProbability(ProbabilityRef potential);

  /**
   * The corrected value for a specific case of a scenario interfering with a risk-affected entity.
   */
  @Schema(
      description =
          "The probability of a scenario in a specific circumstance. A scalar value that matches a valid probability level from a risk-definition.",
      minimum = "0",
      example = "3")
  ProbabilityRef getSpecificProbability();

  /** The result of combining the potential and specific probability. */
  @Schema(
      description =
          "Either the potential probability or the specific probability where "
              + "the latter takes precedence. A scalar value that matches a valid probability level from a risk-definition.",
      example = "4",
      minimum = "0")
  ProbabilityRef getEffectiveProbability();

  @Size(max = PotentialProbability.EXPLANATION_MAX_LENGTH)
  @Schema(
      description = "A user-defined explanation for the selection of the probability value.",
      example = "No risk no fun.")
  String getSpecificProbabilityExplanation();

  /**
   * The corrected value for a specific case of a scenario interfering with a risk-affected entity.
   */
  void setSpecificProbability(ProbabilityRef specific);

  void setSpecificProbabilityExplanation(@Size(max = DESCRIPTION_MAX_LENGTH) String explanation);
}
