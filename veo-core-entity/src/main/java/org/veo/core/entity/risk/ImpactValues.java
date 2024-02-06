/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;
import static java.util.HashMap.newHashMap;
import static java.util.function.Function.identity;
import static org.veo.core.entity.risk.ImpactMethod.HIGH_WATER_MARK;
import static org.veo.core.entity.risk.ImpactReason.MANUAL;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds risk-related information for an element. An {@link ImpactValues} object is only valid for a
 * certain risk definition.
 *
 * <p>Contains the following information:
 *
 * <ul>
 *   <li>potentialImpacts: The specific potential impact values for each category
 *   <li>calculatedPotentialImpacts: The high-water-mark impact values for each category as
 *       determined by the risk service
 *   <li>effectivePotentialImpacts: The specific impact if one exists, otherwise the calculated
 *       impact.
 *   <li>specificPotentialImpactReason: The reason for the chosen specific impact in each category
 *   <li>potentialImpactExplanations: An additional explanation for each category
 *   <li>potentialImpactEffectiveReasons: The reason for the effective impact. This is either the
 *       one chosen by the user for a specific impact, or the used calculation method if the value
 *       was determined automatically.
 * </ul>
 */
public record ImpactValues(
    @NotNull Map<CategoryRef, ImpactRef> potentialImpacts,
    Map<CategoryRef, ImpactRef> potentialImpactsCalculated,
    Map<CategoryRef, ImpactReason> potentialImpactReasons,
    Map<CategoryRef, String> potentialImpactExplanations) {

  public ImpactValues {
    if (potentialImpactsCalculated == null) {
      potentialImpactsCalculated = newHashMap(5);
    }
    if (potentialImpactReasons == null) {
      potentialImpactReasons = newHashMap(5);
    }
    if (potentialImpactExplanations == null) {
      potentialImpactExplanations = newHashMap(5);
    }
    // TODO #2663 remove this automatism (the API client will have to ensure consistent impact maps)
    // Use MANUAL as default reason
    for (var cat : potentialImpacts.keySet()) {
      potentialImpactReasons.putIfAbsent(cat, MANUAL);
    }
  }

  public ImpactValues(Map<CategoryRef, ImpactRef> potentialImpacts) {
    this(potentialImpacts, newHashMap(5), newHashMap(5), newHashMap(5));
  }

  /**
   * For each category, return the specific value if present. Otherwise, return the calculated
   * value. The effective value is the one that should be used for any further impact calculations,
   * i.e. during risk analysis. It allows the user to work with calculated values by default but
   * being able to override them with specific values if necessary.
   */
  @JsonProperty(access = READ_ONLY)
  public Map<CategoryRef, ImpactRef> getPotentialImpactsEffective() {
    return Stream.of(potentialImpacts, potentialImpactsCalculated)
        .filter(Objects::nonNull)
        .flatMap(map -> map.keySet().stream())
        .distinct()
        .collect(
            Collectors.toMap(
                identity(),
                key -> potentialImpacts.getOrDefault(key, potentialImpactsCalculated.get(key))));
  }

  @JsonProperty(access = READ_ONLY)
  public Map<CategoryRef, String> getPotentialImpactEffectiveReasons() {
    return Stream.of(potentialImpacts, potentialImpactsCalculated)
        .filter(Objects::nonNull)
        .flatMap(map -> map.keySet().stream())
        .distinct()
        .collect(Collectors.toMap(identity(), this::determineEffectiveReasonMessage));
  }

  private String determineEffectiveReasonMessage(CategoryRef cat) {
    // If there is a specific impact, return the given reason for it.
    // If there is no specific impact, return the method used to calculate the impact.
    // NOTE: currently this is always the high-water-mark method
    return potentialImpacts.containsKey(cat)
        ? potentialImpactReasons.getOrDefault(cat, MANUAL).getTranslationKey()
        : HIGH_WATER_MARK.getTranslationKey();
  }
}
