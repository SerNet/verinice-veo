/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.aspects.SubTypeAspect;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.state.CustomAspectState;
import org.veo.core.entity.state.CustomLinkState;
import org.veo.core.entity.state.DomainAssociationState;
import org.veo.core.entity.state.DomainAssociationState.DomainAssociationStateImpl;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DomainAssociationDto {
  @NotNull
  @Schema(minLength = 1, maxLength = SubTypeAspect.SUB_TYPE_MAX_LENGTH, requiredMode = REQUIRED)
  String subType;

  @NotNull
  @Schema(minLength = 1, maxLength = SubTypeAspect.STATUS_MAX_LENGTH, requiredMode = REQUIRED)
  String status;

  @Schema(
      description =
          "Results of all decisions concerning this element within this domain. Key is decision key, value is results.",
      accessMode = Schema.AccessMode.READ_ONLY)
  private Map<DecisionRef, DecisionResult> decisionResults;

  // TODO #2542 expose in OpenApi docs, but not for legacy endpoints
  @Schema(hidden = true)
  CustomAspectMapDto customAspects;

  // TODO #2542 expose in OpenApi docs, but not for legacy endpoints
  @Schema(hidden = true)
  LinkMapDto links;

  public DomainAssociationState getDomainAssociationState(
      ITypedId<Domain> domain,
      // TODO #2543 remove
      Set<CustomAspectState> customAspectStates,
      // TODO #2543 remove
      Set<CustomLinkState> customLinkStates) {
    // TODO #2542 use new customAspects & links fields if they are set
    return new DomainAssociationStateImpl(
        domain, subType, status, customAspectStates, customLinkStates);
  }
}
