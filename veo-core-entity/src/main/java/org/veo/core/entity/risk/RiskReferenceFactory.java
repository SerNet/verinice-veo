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

import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiskReferenceFactory {
  protected RiskRef createRiskRef(BigDecimal id) {
    return id == null ? null : new RiskRef(id);
  }

  protected ProbabilityRef createProbabilityRef(BigDecimal id) {
    return id == null ? null : new ProbabilityRef(id);
  }

  protected ImpactRef createImpactRef(BigDecimal id) {
    return id == null ? null : new ImpactRef(id);
  }

  protected CategoryRef createCategoryRef(String id) {
    return id == null ? null : new CategoryRef(id);
  }

  protected ImplementationStatusRef createImplementationStatusRef(int ordinalValue) {
    return new ImplementationStatusRef(ordinalValue);
  }

  protected RiskDefinitionRef createRiskDefinitionRef(String id) {
    return id == null ? null : new RiskDefinitionRef(id);
  }
}
