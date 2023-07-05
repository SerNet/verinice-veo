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

import static org.veo.core.entity.riskdefinition.DimensionDefinition.initLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.Constraints;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Defines the {@link CategoryDefinition}'s and possible {@link RiskValue}'s for a matrix based risk
 * determination.
 */
@EqualsAndHashCode()
@Data()
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
public class RiskDefinition {

  public static final int MAX_ID_SIZE = Constraints.DEFAULT_CONSTANT_MAX_LENGTH;

  @NotNull(message = "An id must be present.")
  @Size(max = MAX_ID_SIZE)
  @ToString.Include
  private String id;

  @Valid @NotNull private ProbabilityDefinition probability;
  @Valid @NotNull private ImplementationStateDefinition implementationStateDefinition;

  @Valid @NotNull @NotEmpty private List<CategoryDefinition> categories = new ArrayList<>();

  @Valid @NotNull private List<RiskValue> riskValues = new ArrayList<>();
  @Valid @NotNull private RiskMethod riskMethod;

  public Optional<CategoryDefinition> getCategory(String categoryId) {
    return categories.stream().filter(c -> c.getId().equals(categoryId)).findAny();
  }

  public void setCategories(List<CategoryDefinition> categories) {
    var ids = categories.stream().map(CategoryDefinition::getId).toList();
    if (ids.size() > ids.stream().distinct().count()) {
      throw new IllegalArgumentException("Categories not unique.");
    }
    this.categories = categories;
  }

  public void setRiskValues(List<RiskValue> values) {
    var ids = values.stream().map(RiskValue::getSymbolicRisk).toList();
    if (ids.size() > ids.stream().distinct().count()) {
      throw new IllegalArgumentException("SymbolicRisk not unique.");
    }
    this.riskValues = values;
    initLevel(values);
  }

  /**
   * A {@link RiskDefinition} is valid if the {@link RiskDefinition#probability} is set and not
   * empty, the id of the {@link CategoryDefinition} in {@link RiskDefinition#categories} is unique
   * and not empty. All {@link RiskDefinition#riskValues} {@link RiskValue#getSymbolicRisk()} need
   * to be unique. Each {@link CategoryDefinition} in {@link RiskDefinition#categories} need to be
   * valid.
   */
  public void validateRiskDefinition() {
    categories.stream().forEach(cd -> cd.validateRiskCategory(riskValues, probability));
  }

  public Optional<RiskValue> getRiskValue(String symbolicRiskId) {
    return riskValues.stream()
        .filter(rv -> rv.getSymbolicRisk().equals(symbolicRiskId))
        .findFirst();
  }

  public Optional<RiskValue> getRiskValue(int ordinalValue) {
    return riskValues.stream().filter(rv -> rv.getOrdinalValue() == ordinalValue).findFirst();
  }
}
