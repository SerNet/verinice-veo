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
import java.util.Set;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.DeterminedRisk;
import org.veo.core.entity.risk.RiskTreatmentOption;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DeterminedRisk", description = "A collection of risk values for a risk category.")
public interface DeterminedRiskSchema {

  @Schema(
      description =
          "The risk that was determined from the combination of impact and "
              + "probability. Does not take existing controls into account (gross risk). "
              + "A scalar value that matches a valid probability level from a risk-definition.",
      example = "3",
      accessMode = Schema.AccessMode.READ_ONLY)
  @PositiveOrZero
  BigDecimal getInherentRisk();

  @Schema(
      description =
          "The risk that remains after taking controls into account as entered by"
              + " the user. A scalar value that matches a valid risk level from a risk-definition.",
      example = "3")
  @PositiveOrZero
  BigDecimal getUserDefinedResidualRisk();

  @Schema(
      description =
          "The inherent risk value is used as the residual risk - unless it is "
              + "overruled by the user-defined residual risk. A scalar value that matches a valid "
              + "risk level from  a risk-definition.",
      example = "3",
      accessMode = Schema.AccessMode.READ_ONLY)
  @PositiveOrZero
  BigDecimal getResidualRisk();

  @Schema(
      description = "An explanation for the user's choice of residual risk.",
      example = "Our controls are so good, even our controls are controlled by " + "controls.")
  @Size(max = DeterminedRisk.EXPLANATION_MAX_LENGTH)
  String getResidualRiskExplanation();

  @ArraySchema(
      schema = @Schema(description = "A choice of risk-treatment options as selected by the user."))
  Set<RiskTreatmentOption> getRiskTreatments();

  @Schema(
      description = "The user's explanation for her choice of risk treatment methods.",
      example = "3")
  @Size(max = DeterminedRisk.EXPLANATION_MAX_LENGTH)
  String getRiskTreatmentExplanation();

  @Schema(
      description =
          "A scalar value that matches a valid risk category from a risk-definition, such as confidentiality, integrity, availability...",
      example = "C")
  @Size(max = CategoryRef.MAX_ID_LENGTH)
  String getCategory();
}
