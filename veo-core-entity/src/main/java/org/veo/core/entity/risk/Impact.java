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

import org.veo.core.entity.Constraints;

/**
 * The effects of a threat event interfering with an asset / a process / a scope.
 *
 * @see org.veo.core.entity.riskdefinition.RiskDefinition
 */
@Valid
public interface Impact extends PotentialImpact {

  int EXPLANATION_MAX_LENGTH = Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH;

  /**
   * The impact for a specific combination of scenario and process/asset/scope. It may be higher or
   * lower (but usually should not be higher) than the potential impact that was initially
   * estimated.
   */
  void setSpecificImpact(ImpactRef specific);

  /**
   * The impact for a specific combination of scenario and process/asset/scope. It may be higher or
   * lower (but usually should not be higher) than the potential impact that was initially
   * estimated.
   */
  ImpactRef getSpecificImpact();

  /** The result of taking both the potential and the specific impact into account. */
  ImpactRef getEffectiveImpact();

  String getSpecificImpactExplanation();

  void setSpecificImpactExplanation(@Size(max = EXPLANATION_MAX_LENGTH) String explanation);
}
