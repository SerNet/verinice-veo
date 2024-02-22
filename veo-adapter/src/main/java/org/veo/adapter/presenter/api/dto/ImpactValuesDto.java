/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.entity.risk.ImpactReason;
import org.veo.core.entity.risk.ImpactRef;
import org.veo.core.entity.risk.PotentialImpactValues;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ImpactValuesDto implements PotentialImpactValues {
  @Schema(
      description =
          "Potential impacts for a set of risk categories. These are specific values entered by the user directly.",
      example = "{\"C\":2,\n\"I\":3}")
  private Map<String, ImpactRef> potentialImpacts;

  @Schema(
      description =
          "Potential impacts for a set of risk categories. These are calculated values based on the high water mark.",
      example = "{\"C\":2,\n\"A\":3}")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Map<String, ImpactRef> potentialImpactsCalculated;

  @Schema(
      description =
          "Potential impacts for a set of risk categories. These are either the specific or the calculated values.",
      example = "{\"C\":2,\n\"A\":1}")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Map<String, ImpactRef> potentialImpactsEffective;

  @Schema(
      description = "The reason for the chosen user-defined potential impact in each category.",
      example = "{\"C\":\"impact_reason_manual\",\n\"A\":\"impact_reason_distributive\"}")
  private Map<String, ImpactReason> potentialImpactReasons;

  @Schema(
      description = "An optional explanation for the user-entered specific potential impact.",
      example =
          "{\"C\":\"Confidentiality based on processed data.\",\n\"A\":\"Availability is distributed to redundant machines and therefore lower.\"}")
  private Map<String, String> potentialImpactExplanations;

  @Schema(
      description =
          "The reason for the effective impact. This is either the one chosen by the user for a specific impact, or the used calculation method if the value was determined automatically. The values are always translation keys.",
      example = "{\"C\":\"impact_reason_manual\",\n\"A\":\"impact_method_high_water_mark\"}")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Map<String, String> potentialImpactEffectiveReasons;
}
