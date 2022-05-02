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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.vladmihalcea.hibernate.type.json.JsonType;

import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Process;
import org.veo.core.entity.risk.ProcessImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.CategoryDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Holds risk related info for a process in a specific domain. */
@Entity(name = "process_impact_values_aspect")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@TypeDef(name = "json", typeClass = JsonType.class, defaultForType = Map.class)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ProcessImpactValuesAspectData extends AspectData {

  public ProcessImpactValuesAspectData(DomainTemplate domain, Process owner) {
    super(domain, owner);
  }

  @Getter
  @NotNull
  @Column(columnDefinition = "jsonb", name = "process_impact_values")
  @Type(type = "json")
  Map<RiskDefinitionRef, ProcessImpactValues> values;

  public void setValues(Map<RiskDefinitionRef, ProcessImpactValues> values) {
    if (values == null) {
      throw new IllegalArgumentException("The impact values need to be set.");
    }
    validateRiskDefinitionExist(values);
    validateAllCategoriesExist(values);
    validateAllImpactsExist(values);
    this.values = values;
  }

  private void validateAllImpactsExist(Map<RiskDefinitionRef, ProcessImpactValues> values) {
    values
        .entrySet()
        .forEach(
            e -> {
              RiskDefinition riskDefinition =
                  this.getDomain().getRiskDefinitions().get(e.getKey().getIdRef());
              ProcessImpactValues impactValues = e.getValue();
              impactValues.getPotentialImpacts().entrySet().stream()
                  .forEach(
                      impacts -> {
                        CategoryDefinition categoryDefinition =
                            riskDefinition.getCategory(impacts.getKey().getIdRef()).orElseThrow();
                        int impactValueId = impacts.getValue().getIdRef().intValue();
                        if (impactValueId < 0
                            || impactValueId
                                > categoryDefinition.getPotentialImpacts().size() - 1) {
                          throw new IllegalArgumentException(
                              "Impact value for category '"
                                  + categoryDefinition.getId()
                                  + "' is out of range "
                                  + impactValueId);
                        }
                      });
            });
  }

  /** Validates that all defined Impact Categories exist in the referenced risk definition. */
  private void validateAllCategoriesExist(Map<RiskDefinitionRef, ProcessImpactValues> values) {
    values
        .entrySet()
        .forEach(
            e -> {
              RiskDefinition riskDefinition =
                  this.getDomain().getRiskDefinitions().get(e.getKey().getIdRef());

              Set<String> categoryIds =
                  riskDefinition.getCategories().stream()
                      .map(c -> c.getId())
                      .collect(Collectors.toSet());
              Set<String> usedCatIds =
                  e.getValue().getPotentialImpacts().keySet().stream()
                      .map(c -> c.getIdRef())
                      .collect(Collectors.toSet());

              usedCatIds.removeAll(categoryIds);
              if (!usedCatIds.isEmpty()) {
                throw new IllegalArgumentException(
                    "Undefined categorie definitions: " + usedCatIds);
              }
            });
  }

  /** Validates that the risk definition is part of associated domain. */
  private void validateRiskDefinitionExist(Map<RiskDefinitionRef, ProcessImpactValues> values) {
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
}
