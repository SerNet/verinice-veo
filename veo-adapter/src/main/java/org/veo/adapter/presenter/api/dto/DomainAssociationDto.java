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

import java.util.Map;

import org.veo.core.entity.aspects.SubTypeAspect;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DomainAssociationDto {
  @Schema(minLength = 1, maxLength = SubTypeAspect.SUB_TYPE_MAX_LENGTH)
  String subType;

  @Schema(minLength = 1, maxLength = SubTypeAspect.STATUS_MAX_LENGTH)
  String status;

  @Schema(
      description =
          "Results of all decisions concerning this element within this domain. Key is decision key, value is results.",
      accessMode = Schema.AccessMode.READ_ONLY)
  private Map<DecisionRef, DecisionResult> decisionResults;
}
