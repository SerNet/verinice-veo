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
package org.veo.adapter.presenter.api.dto.full;

import java.util.List;

import jakarta.validation.Valid;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Domain;
import org.veo.core.entity.risk.DeterminedRisk;
import org.veo.core.entity.risk.DeterminedRiskImpl;
import org.veo.core.entity.risk.Impact;
import org.veo.core.entity.risk.ImpactImpl;
import org.veo.core.entity.risk.Probability;
import org.veo.core.entity.risk.ProbabilityImpl;
import org.veo.core.entity.risk.RiskDefinitionRef;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
@AllArgsConstructor
@Valid
@Schema(description = "A set of risk values")
public class RiskValuesDto {

  @Valid
  @JsonDeserialize(as = ProbabilityImpl.class)
  @Schema(description = "Values describing the probability of this risk occurring")
  private Probability probability;

  @Valid
  @JsonDeserialize(contentAs = ImpactImpl.class)
  @ArraySchema(
      arraySchema =
          @Schema(
              description =
                  "Values describing the impacts of this risk in different risk categories"))
  private List<Impact> impactValues;

  @Valid
  @JsonDeserialize(contentAs = DeterminedRiskImpl.class)
  @ArraySchema(
      arraySchema =
          @Schema(description = "Values describing the evaluated risk in different categories"))
  private List<DeterminedRisk> riskValues;

  public static RiskValuesDto from(AbstractRisk<?, ?> risk, RiskDefinitionRef rd, Domain domain) {
    return RiskValuesDto.builder()
        .probability(risk.getProbabilityProvider(rd, domain).getProbability())
        .impactValues(risk.getImpactProvider(rd, domain).getCategorizedImpacts())
        .riskValues(risk.getRiskProvider(rd, domain).getCategorizedRisks())
        .build();
  }
}
