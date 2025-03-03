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

import java.util.EnumSet;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Valid
public class DeterminedRiskImpl implements DeterminedRisk {

  @NonNull
  @Setter(AccessLevel.NONE)
  private CategoryRef category;

  private RiskRef inherentRisk;

  private RiskRef userDefinedResidualRisk;

  @Size(max = DeterminedRisk.EXPLANATION_MAX_LENGTH)
  private String residualRiskExplanation;

  /** A collection of selected risk treatment options. */
  private Set<RiskTreatmentOption> riskTreatments = EnumSet.noneOf(RiskTreatmentOption.class);

  @Size(max = DeterminedRisk.EXPLANATION_MAX_LENGTH)
  private String riskTreatmentExplanation;

  public void setRiskTreatments(Set<RiskTreatmentOption> riskTreatments) {
    this.riskTreatments.clear();
    this.riskTreatments.addAll(riskTreatments);
  }
}
