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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.entity.risk.PotentialProbabilityImpl;
import org.veo.core.entity.risk.Probability;
import org.veo.core.entity.risk.ProbabilityRef;
import org.veo.core.entity.risk.ProbabilityValueProvider;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Valid
public class ProbabilityDto implements Probability {

  ProbabilityRef potentialProbability;

  ProbabilityRef specificProbability;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  ProbabilityRef effectiveProbability;

  @Size(max = PotentialProbabilityImpl.EXPLANATION_MAX_LENGTH)
  String specificProbabilityExplanation;

  public static ProbabilityDto from(ProbabilityValueProvider from) {
    return new ProbabilityDto(
        from.getPotentialProbability(),
        from.getSpecificProbability(),
        from.getEffectiveProbability(),
        from.getSpecificProbabilityExplanation());
  }
}
