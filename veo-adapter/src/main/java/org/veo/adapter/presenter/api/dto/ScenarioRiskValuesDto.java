/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Daniel Murygin
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
package org.veo.adapter.presenter.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.ScenarioRiskValues;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ScenarioRiskValuesDto implements ScenarioRiskValues {
  @PositiveOrZero
  @Schema(
      description =
          "The potential probability for occurrence of the scenario, a reference to a probability "
              + "level in the risk definition. Enter the ordinal value of a probability level.",
      example = "2")
  BigDecimal potentialProbability;

  @Size(max = PotentialProbability.EXPLANATION_MAX_LENGTH)
  @Schema(
      description = "The optional potential probability explanation.",
      example = "Because of sience.")
  private String potentialProbabilityExplanation;
}
