/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Ben Nasrallah.
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
package org.veo.core.entity;

import java.util.Map;
import java.util.Optional;

import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.RiskDefinitionRef;

/**
 * NIST: Threat Scenario: A set of discrete threat events, associated with a specific threat source
 * or multiple threat sources, partially ordered in time.
 */
public interface Scenario extends Element, CompositeElement<Scenario>, RiskRelated {

  String SINGULAR_TERM = "scenario";
  String PLURAL_TERM = "scenarios";
  String TYPE_DESIGNATOR = "SCN";

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  @Override
  default Class<Scenario> getModelInterface() {
    return Scenario.class;
  }

  @Override
  default String getTypeDesignator() {
    return TYPE_DESIGNATOR;
  }

  Map<RiskDefinitionRef, PotentialProbability> getPotentialProbability(Domain domain);

  default Optional<PotentialProbability> getPotentialProbability(
      Domain domain, RiskDefinitionRef riskDefinition) {
    return Optional.ofNullable(getPotentialProbability(domain).get((riskDefinition)));
  }

  void setPotentialProbability(
      Domain domain, Map<RiskDefinitionRef, PotentialProbability> potentialProbability);
}
