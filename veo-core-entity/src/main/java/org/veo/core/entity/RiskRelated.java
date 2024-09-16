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
package org.veo.core.entity;

import java.util.Set;

import org.veo.core.entity.risk.RiskDefinitionRef;

/** Something that holds data related to risk definitions. */
public interface RiskRelated {

  /** Finds all risk definitions used by this entity in given domain. */
  Set<RiskDefinitionRef> getRiskDefinitions(Domain domain);

  /**
   * Removes any data related to given risk definition from this entity
   *
   * @return {@code true} if anything was removed, otherwise {@code false}
   */
  boolean removeRiskDefinition(RiskDefinitionRef riskDefinition, Domain domain);
}
