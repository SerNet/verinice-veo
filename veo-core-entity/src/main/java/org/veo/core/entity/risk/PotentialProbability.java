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
package org.veo.core.entity.risk;

import jakarta.validation.constraints.Size;

import org.veo.core.entity.Constraints;

/**
 * This class contains a reference to a potential probability. The referenced probability must be
 * defined in a risk definition.
 */
public record PotentialProbability(
    ProbabilityRef potentialProbability,
    @Size(max = EXPLANATION_MAX_LENGTH) String potentialProbabilityExplanation) {
  public static final int EXPLANATION_MAX_LENGTH = Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH;

  public PotentialProbability(ProbabilityRef potentialProbability) {
    this(potentialProbability, null);
  }
}
