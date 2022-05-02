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
package org.veo.adapter.presenter.api.openapi;

import java.math.BigDecimal;

import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import org.veo.core.entity.risk.Probability;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This class is for documentation only.
 *
 * <p>It is only supposed to be used in OpenApi annotations and should not be extended and
 * implemented.
 */
@Schema(name = "Probability", description = "A collection of probability values.")
public interface ProbabilitySchema {

  @Schema(
      description =
          "The probability of a scenario in a specific circumstance. A scalar value that matches a valid probability level from a risk-definition.",
      example = "3")
  @PositiveOrZero
  BigDecimal getSpecificProbability();

  @Schema(
      description =
          "Either the potential probability or the specific probability where "
              + "the latter takes precedence. A scalar value that matches a valid probability level from a risk-definition.",
      example = "4")
  @PositiveOrZero
  BigDecimal getEffectiveProbability();

  @Schema(
      description = "A user-defined explanation for the selection of the probability value.",
      example = "No risk no fun.")
  @Size(max = Probability.EXPLANATION_MAX_LENGTH)
  String getSpecificProbabilityExplanation();

  @Schema(
      description =
          "The potential probability derived from the scenario associated with " + "this risk.",
      example = "No risk no fun.",
      accessMode = Schema.AccessMode.READ_ONLY)
  BigDecimal getPotentialProbability();
}
