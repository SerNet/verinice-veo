/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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
package org.veo.core.entity.riskdefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.core.entity.TranslationMap;
import org.veo.core.entity.risk.ImpactRef;
import org.veo.core.entity.risk.ProbabilityRef;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Defines the Category of a {@link RiskDefinition}, it has a list of valid {@link CategoryLevel}
 * defining the value range. It also has a riskValueMatrik in the dimensions
 * potentialImpacts.size()*probability.getLevels().size() and defines for each combination of {@link
 * CategoryLevel} and {@link ProbabilityLevel} a {@link RiskValue}. These values must conform to the
 * {@link RiskValue} in the {@link RiskDefinition#getRiskValues()}.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class CategoryDefinition extends DimensionDefinition {
  public CategoryDefinition(
      String id,
      @NotNull List<List<RiskValue>> valueMatrix,
      @NotNull List<CategoryLevel> potentialImpacts) {
    super(id);
    this.valueMatrix = valueMatrix;
    this.potentialImpacts = potentialImpacts;
    initLevel(potentialImpacts);
  }

  public CategoryDefinition(
      String id,
      @NotNull List<List<RiskValue>> valueMatrix,
      @NotNull List<CategoryLevel> potentialImpacts,
      TranslationMap translations) {
    super(id, translations);
    this.valueMatrix = valueMatrix;
    this.potentialImpacts = potentialImpacts;
    initLevel(potentialImpacts);
  }

  @EqualsAndHashCode.Include private List<List<RiskValue>> valueMatrix;
  @EqualsAndHashCode.Include private List<CategoryLevel> potentialImpacts = new ArrayList<>();

  public RiskValue getRiskValue(ProbabilityRef effectiveProbability, ImpactRef effectiveImpact) {
    ensureRiskValuesSupported();
    int categoryOrdinalValue = effectiveImpact.getIdRef().intValue();
    if (categoryOrdinalValue > valueMatrix.size() - 1) {
      throw new IllegalArgumentException("No risk value for category: " + categoryOrdinalValue);
    }

    List<RiskValue> probability = valueMatrix.get(categoryOrdinalValue);
    int probabliltyOrdinalValue = effectiveProbability.getIdRef().intValue();
    if (probabliltyOrdinalValue > probability.size() - 1)
      throw new IllegalArgumentException(
          "No risk value for probability: " + probabliltyOrdinalValue);

    return probability.get(probabliltyOrdinalValue);
  }

  public void setPotentialImpacts(@NotNull List<CategoryLevel> potentialImpacts) {
    this.potentialImpacts = potentialImpacts;
    initLevel(potentialImpacts);
  }

  /**
   * A valid matrix contains {@link RiskValue}s where each value needs to be present in the supplied
   * riskValues and the matrix dimensions need to match the {@link
   * CategoryDefinition#potentialImpacts} size and the {@link ProbabilityDefinition#getLevels()}
   * size.
   */
  public void validateRiskCategory(
      @NotNull List<RiskValue> riskValues, @NotNull ProbabilityDefinition probability) {
    if (!isRiskValuesSupported()) {
      return;
    }

    Map<Integer, String> ordinalToSymbolicMap =
        riskValues.stream()
            .collect(Collectors.toMap(rv -> rv.getOrdinalValue(), rv -> rv.getSymbolicRisk()));
    Set<RiskValue> undefinedRiskValues =
        valueMatrix.stream()
            .flatMap(Collection::stream)
            .filter(
                rv ->
                    !ordinalToSymbolicMap
                        .getOrDefault(rv.getOrdinalValue(), "")
                        .equals(rv.getSymbolicRisk()))
            .collect(Collectors.toSet());

    if (!undefinedRiskValues.isEmpty()) {
      throw new IllegalArgumentException(
          "Invalid risk values for category " + getId() + ": " + undefinedRiskValues);
    }

    if (valueMatrix.size() != potentialImpacts.size()) {
      throw new IllegalArgumentException(
          "Value matrix for category " + getId() + " does not conform to impacts.");
    }
    valueMatrix.stream()
        .forEach(
            l -> {
              if (l.size() != probability.getLevels().size()) {
                throw new IllegalArgumentException(
                    "Value matrix for category " + getId() + " does not conform to probability.");
              }
            });
  }

  public void ensureRiskValuesSupported() {
    if (!isRiskValuesSupported()) {
      throw new IllegalArgumentException("Category " + getId() + " does not support risk values.");
    }
  }

  @JsonIgnore
  public boolean isRiskValuesSupported() {
    return valueMatrix != null;
  }

  public Optional<CategoryLevel> getLevel(int ordinalValue) {
    return potentialImpacts.stream()
        .filter(level -> level.getOrdinalValue() == ordinalValue)
        .findFirst();
  }
}
