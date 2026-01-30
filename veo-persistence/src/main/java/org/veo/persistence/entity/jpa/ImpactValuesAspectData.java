/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.persistence.entity.jpa;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.veo.core.entity.Domain;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.CategoryDefinition;
import org.veo.core.entity.riskdefinition.DimensionDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Holds impact related information for an element in a specific domain. */
@Entity(name = "impact_values_aspect")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ImpactValuesAspectData extends AspectData {

  ImpactValuesAspectData(Domain domain, RiskAffected<?, ?> owner) {
    super(domain, owner);
  }

  @Getter
  @NotNull
  @Column(columnDefinition = "jsonb", name = "impact_values")
  @JdbcTypeCode(SqlTypes.JSON)
  Map<RiskDefinitionRef, ImpactValues> values;

  @Deprecated()
  void setValues(Map<RiskDefinitionRef, ImpactValues> values) {
    if (values == null) {
      throw new IllegalArgumentException("The impact values need to be set.");
    }
    validateRiskDefinitionExist(values);
    validateAllCategoriesExist(values);
    validateAllImpactsExist(values);
    this.values = values;
  }

  private void validateAllImpactsExist(Map<RiskDefinitionRef, ImpactValues> values) {
    values
        .entrySet()
        .forEach(
            e -> {
              RiskDefinition riskDefinition =
                  this.getDomain().getRiskDefinitions().get(e.getKey().getIdRef());
              ImpactValues impactValues = e.getValue();
              impactValues.potentialImpacts().entrySet().stream()
                  .forEach(
                      impacts -> {
                        CategoryDefinition categoryDefinition =
                            riskDefinition.getCategory(impacts.getKey().getIdRef()).orElseThrow();
                        int impactValueId = impacts.getValue().getIdRef().intValue();
                        if (impactValueId < 0
                            || impactValueId
                                > categoryDefinition.getPotentialImpacts().size() - 1) {
                          throw new IllegalArgumentException(
                              "Impact value %d for category '%s' is out of range"
                                  .formatted(impactValueId, categoryDefinition.getId()));
                        }
                      });
            });
  }

  /** Validates that all defined Impact Categories exist in the referenced risk definition. */
  private void validateAllCategoriesExist(Map<RiskDefinitionRef, ImpactValues> values) {
    values
        .entrySet()
        .forEach(
            e -> {
              RiskDefinition riskDefinition =
                  this.getDomain().getRiskDefinitions().get(e.getKey().getIdRef());

              Set<String> categoryIds =
                  riskDefinition.getCategories().stream()
                      .map(DimensionDefinition::getId)
                      .collect(Collectors.toSet());
              Set<String> usedCatIds =
                  e.getValue().potentialImpacts().keySet().stream()
                      .map(CategoryRef::getIdRef)
                      .collect(Collectors.toSet());

              usedCatIds.removeAll(categoryIds);
              if (!usedCatIds.isEmpty()) {
                throw new IllegalArgumentException(
                    "Undefined categorie definitions: " + usedCatIds);
              }
            });
  }

  /** Validates that the risk definition is part of associated domain. */
  private void validateRiskDefinitionExist(Map<RiskDefinitionRef, ImpactValues> values) {
    Set<String> idSet =
        this.getDomain().getRiskDefinitions().entrySet().stream()
            .map(e -> e.getValue().getId())
            .collect(Collectors.toSet());
    Set<String> usedIds =
        values.entrySet().stream().map(e -> e.getKey().getIdRef()).collect(Collectors.toSet());
    usedIds.removeAll(idSet);
    if (!usedIds.isEmpty()) {
      throw new IllegalArgumentException("Undefined risk definitions: " + usedIds);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
