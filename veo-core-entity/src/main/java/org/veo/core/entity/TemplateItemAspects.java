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

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.veo.core.entity.risk.ControlRiskValues;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.RiskDefinitionRef;

/**
 * Serializable container for aspects to be used in a template item.
 *
 * <p>For each element type, only a subset of the information representable in this class is
 * relevant.
 */
public record TemplateItemAspects(
    Map<RiskDefinitionRef, ControlRiskValues> controlRiskValues,
    Map<RiskDefinitionRef, ImpactValues> impactValues,
    Map<RiskDefinitionRef, PotentialProbability> scenarioRiskValues) {
  public TemplateItemAspects {
    controlRiskValues = immutableCopy(controlRiskValues);
    impactValues = immutableCopy(impactValues);
    scenarioRiskValues = immutableCopy(scenarioRiskValues);
  }

  private static <TKey, TValue> @Nullable Map<TKey, TValue> immutableCopy(
      @Nullable Map<TKey, TValue> map) {
    return Optional.ofNullable(map).map(Map::copyOf).orElse(null);
  }

  public TemplateItemAspects() {
    this(null, null, null);
  }
}
