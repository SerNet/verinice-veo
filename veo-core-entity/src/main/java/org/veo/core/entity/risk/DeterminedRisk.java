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

import java.util.Optional;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.veo.core.entity.Constraints;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A risk value as determined by a risk service.
 *
 * <p>
 *
 * <p>The risk value will be determined based on a method defined in the risk definition (i.e. risk
 * matrix, high-water-mark, sum, product, ...)
 *
 * @see org.veo.core.entity.riskdefinition.RiskDefinition
 */
@Valid
@Schema(description = "A collection of risk values for a risk category.")
public interface DeterminedRisk {

  int EXPLANATION_MAX_LENGTH = Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH;

  void setRiskTreatmentExplanation(
      @Size(max = EXPLANATION_MAX_LENGTH) String riskTreatmentExplanation);

  /**
   * A risk value that is determined by the risk service according to the method defined in the risk
   * definition.
   *
   * @return inherent risk or null
   * @see Probability
   * @see Impact
   */
  @Schema(
      description =
          "The risk that was determined from the combination of impact and "
              + "probability. Does not take existing controls into account (gross risk). "
              + "A scalar value that matches a valid probability level from a risk-definition.",
      example = "3",
      accessMode = Schema.AccessMode.READ_ONLY)
  RiskRef getInherentRisk();

  /**
   * The residual risk (aka net risk) entered manually by the user as result of taking control
   * effects into account.
   */
  @Schema(
      description =
          "The risk that remains after taking controls into account as entered by"
              + " the user. A scalar value that matches a valid risk level from a risk-definition.",
      example = "3")
  RiskRef getUserDefinedResidualRisk();

  /**
   * The inherent risk value is used as the residual risk - unless it is overruled by the
   * user-defined residual risk.
   *
   * @return residual risk or null (if there is no user-defined residual risk and no inherent risk)
   */
  @JsonProperty(access = Access.READ_ONLY)
  @Schema(
      description =
          "The inherent risk value is used as the residual risk - unless it is "
              + "overruled by the user-defined residual risk. A scalar value that matches a valid "
              + "risk level from  a risk-definition.",
      example = "3",
      accessMode = Schema.AccessMode.READ_ONLY)
  default RiskRef getResidualRisk() {
    return Optional.ofNullable(getUserDefinedResidualRisk()).orElse(getInherentRisk());
  }

  void setResidualRiskExplanation(
      @Size(max = EXPLANATION_MAX_LENGTH) String residualRiskExplanation);

  @Schema(
      description = "An explanation for the user's choice of residual risk.",
      example = "Our controls are so good, even our controls are controlled by " + "controls.")
  @Size(max = DeterminedRisk.EXPLANATION_MAX_LENGTH)
  String getResidualRiskExplanation();

  @ArraySchema(
      arraySchema =
          @Schema(description = "A choice of risk-treatment options as selected by the user."))
  Set<RiskTreatmentOption> getRiskTreatments();

  @Schema(
      description = "The user's explanation for her choice of risk treatment methods.",
      example = "3")
  @Size(max = DeterminedRisk.EXPLANATION_MAX_LENGTH)
  String getRiskTreatmentExplanation();

  /** The risk after existing controls have been taken into account. */
  void setUserDefinedResidualRisk(RiskRef userDefinedResidualRisk);

  void setRiskTreatments(Set<RiskTreatmentOption> riskTreatments);

  @Schema(
      description =
          "A scalar value that matches a valid risk category from a risk-definition, such as confidentiality, integrity, availability...",
      example = "C")
  @Size(max = CategoryRef.MAX_ID_LENGTH)
  CategoryRef getCategory();
}
