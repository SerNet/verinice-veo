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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.validation.Valid;

import org.veo.core.entity.Process;
import org.veo.core.entity.state.ProcessState;
import org.veo.core.entity.state.RiskImpactDomainAssociationState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(
    title = "Process",
    description =
        "A series of activities that uses organization resources to transform input into results - this DTO represents a process from the viewpoint of a domain and contains both basic and domain-specific properties.")
public abstract class AbstractProcessInDomainDto
    extends AbstractCompositeElementInDomainDto<Process>
    implements ProcessState, RiskImpactDomainAssociationState, RiskAffectedDto<Process> {

  @Override
  @Schema(example = "External wage accounting")
  public String getName() {
    return super.getName();
  }

  @Override
  @Schema(description = "Short human-readable identifier (not unique)", example = "EWA")
  public String getAbbreviation() {
    return super.getAbbreviation();
  }

  @Override
  @Schema(example = "Data Inc. delegates wage accounting to Money Inc.")
  public String getDescription() {
    return super.getDescription();
  }

  @Override
  @Schema(description = "Unique human-readable identifier", example = "PRO-18")
  public String getDesignator() {
    return super.getDesignator();
  }

  @Override
  public Class<Process> getModelInterface() {
    return Process.class;
  }

  @Schema(
      description =
          "Key is risk definition ID, value contains risk values in the context of that risk definition.")
  Map<String, ImpactRiskValuesDto> riskValues = new HashMap<>();

  @Valid private Set<ControlImplementationDto> controlImplementations = new HashSet<>();
}
