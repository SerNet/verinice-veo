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
package org.veo.persistence.entity.jpa;

import static jakarta.persistence.GenerationType.SEQUENCE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.riskdefinition.CategoryDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
@Entity(name = "risk_definition_set")
public class RiskDefinitionSetData {
  @Id
  @GeneratedValue(strategy = SEQUENCE, generator = "seq_risk_definition_sets")
  @SequenceGenerator(name = "seq_risk_definition_sets")
  private Long id;

  @NotNull
  @Column(columnDefinition = "jsonb")
  @Type(JsonType.class)
  @Valid
  Map<String, RiskDefinition> riskDefinitions = new HashMap<>();

  public void setRiskDefinitions(Map<String, RiskDefinition> riskDefinitions) {
    this.riskDefinitions.clear();
    riskDefinitions.forEach(this::apply);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (this == o) return true;
    if (!(o instanceof RiskDefinitionSetData other)) return false;

    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore, a transient entity must never equal another entity.
    return id != null && id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  /**
   * @return {@code true} if a new risk definition has been updated or {@code false} if an existing
   *     risk definition has been updated
   */
  public boolean apply(String riskDefinitionRef, RiskDefinition riskDefinition) {
    // TODO VEO-2258 Allow more modifications on an existing risk definition.
    if (riskDefinitions.containsKey(riskDefinitionRef)) {
      var oldRiskDef = riskDefinitions.get(riskDefinitionRef);
      if (!categoriesAddedOrUnchangedOrOnlyMatrixesDeleted(riskDefinition, oldRiskDef)
          || !oldRiskDef.getRiskValues().equals(riskDefinition.getRiskValues())
          || !oldRiskDef
              .getImplementationStateDefinition()
              .equals(riskDefinition.getImplementationStateDefinition())
          || !oldRiskDef.getProbability().equals(riskDefinition.getProbability()))
        throw new UnprocessableDataException(
            "Your modifications on this existing risk definition are not supported yet. Currently, only the impact-inheriting links can be modified.");
    }
    return riskDefinitions.put(riskDefinitionRef, riskDefinition) == null;
  }

  private boolean categoriesAddedOrUnchangedOrOnlyMatrixesDeleted(
      RiskDefinition newRiskDef, RiskDefinition oldRiskDef) {
    for (CategoryDefinition oldCategoryDef : oldRiskDef.getCategories()) {
      Optional<CategoryDefinition> newCategoryDefOpt =
          newRiskDef.getCategory(oldCategoryDef.getId());
      if (newCategoryDefOpt.isEmpty()) {
        return false;
      }
      CategoryDefinition newCategoryDef = newCategoryDefOpt.get();
      if (!oldCategoryDef.equals(newCategoryDef)) {
        if (oldCategoryDef.isRiskValuesSupported() && !newCategoryDef.isRiskValuesSupported()) {
          newCategoryDef.setValueMatrix(List.copyOf(oldCategoryDef.getValueMatrix()));
          boolean onlyRiskMatrixRemoved = oldCategoryDef.equals(newCategoryDef);
          newCategoryDef.setValueMatrix(null);
          if (onlyRiskMatrixRemoved) {
            continue;
          }
        }
        return false;
      }
    }
    return true;
  }

  public void remove(String riskDefinitionKey) {
    if (!riskDefinitions.containsKey(riskDefinitionKey)) {
      throw new NotFoundException("Risk definition '%s' not found", riskDefinitionKey);
    }
    riskDefinitions.remove(riskDefinitionKey);
  }
}
