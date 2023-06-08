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
package org.veo.adapter.presenter.api.dto;

import java.util.HashMap;
import java.util.Map;

import org.veo.core.entity.Scenario;
import org.veo.core.entity.state.ScenarioDomainAssociationState;
import org.veo.core.entity.state.ScenarioState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(
    title = "Scenario",
    description =
        "A possible situation that threatens data security - this DTO represents a scenario from the viewpoint of a domain and contains both basic and domain-specific properties.")
public abstract class AbstractScenarioInDomainDto
    extends AbstractCompositeElementInDomainDto<Scenario>
    implements ScenarioState, ScenarioDomainAssociationState {

  @Override
  @Schema(example = "Phishing")
  public String getName() {
    return super.getName();
  }

  @Override
  @Schema(description = "Short human-readable identifier (not unique)", example = "PH")
  public String getAbbreviation() {
    return super.getAbbreviation();
  }

  @Override
  @Schema(example = "Attackers trick employers into revealing sensitive information")
  public String getDescription() {
    return super.getDescription();
  }

  @Override
  @Schema(description = "Unique human-readable identifier", example = "SCN-12")
  public String getDesignator() {
    return super.getDesignator();
  }

  @Override
  public Class<Scenario> getModelInterface() {
    return Scenario.class;
  }

  @Schema(
      description =
          "Key is risk definition ID, value are the values in the context of that risk definition.")
  Map<String, ScenarioRiskValuesDto> riskValues = new HashMap<>();
}
