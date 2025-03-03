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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.veo.core.entity.Domain;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.state.CustomAspectState;
import org.veo.core.entity.state.CustomLinkState;
import org.veo.core.entity.state.DomainAssociationState;
import org.veo.core.entity.state.ScenarioDomainAssociationState.ScenarioDomainAssociationStateImpl;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScenarioDomainAssociationDto extends DomainAssociationDto {
  @Schema(
      description =
          "Key is risk definition ID, value are the values in the context of that risk definition.")
  Map<String, ScenarioRiskValuesDto> riskValues = new HashMap<>();

  @Override
  public DomainAssociationState getDomainAssociationState(
      ITypedId<Domain> domain,
      Set<CustomAspectState> customAspectStates,
      Set<CustomLinkState> customLinkStates) {
    return new ScenarioDomainAssociationStateImpl(
        domain,
        subType,
        status,
        riskValues,
        customAspectStates,
        customLinkStates,
        appliedCatalogItem);
  }
}
