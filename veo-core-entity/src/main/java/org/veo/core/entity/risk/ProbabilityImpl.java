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

import javax.validation.Valid;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Valid
public class ProbabilityImpl implements Probability {

  private ProbabilityRef specificProbability;

  @Setter(AccessLevel.NONE)
  private ProbabilityRef effectiveProbability;

  @Size(max = ExplainedPotentialProbability.EXPLANATION_MAX_LENGTH)
  private String specificProbabilityExplanation;

  private ProbabilityRef potentialProbability;

  public void setPotentialProbability(ProbabilityRef potentialProbability) {
    this.potentialProbability = potentialProbability;
    updateEffectiveProbability();
  }

  public void setSpecificProbability(ProbabilityRef specificProbability) {
    this.specificProbability = specificProbability;
    updateEffectiveProbability();
  }

  private void updateEffectiveProbability() {
    if (specificProbability != null) effectiveProbability = specificProbability;
    else effectiveProbability = potentialProbability;
  }
}
