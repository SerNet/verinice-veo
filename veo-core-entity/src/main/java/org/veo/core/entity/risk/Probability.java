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

import static org.veo.core.entity.Nameable.DESCRIPTION_MAX_LENGTH;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.veo.core.entity.Constraints;

/**
 * The probability of a risk event occurring.
 *
 * @see org.veo.core.entity.riskdefinition.RiskDefinition
 */
@Valid
public interface Probability extends PotentialProbability {

  int EXPLANATION_MAX_LENGTH = Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH;

  /**
   * The corrected value for a specific case of a scenario interfering with a risk-affected entity.
   */
  ProbabilityRef getSpecificProbability();

  /** The result of combining the potential and specific probability. */
  ProbabilityRef getEffectiveProbability();

  String getSpecificProbabilityExplanation();

  /**
   * The corrected value for a specific case of a scenario interfering with a risk-affected entity.
   */
  void setSpecificProbability(ProbabilityRef specific);

  void setSpecificProbabilityExplanation(@Size(max = DESCRIPTION_MAX_LENGTH) String explanation);
}
