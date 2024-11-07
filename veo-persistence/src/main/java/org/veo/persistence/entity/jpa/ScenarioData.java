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
package org.veo.persistence.entity.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.TemplateItemAspects;
import org.veo.core.entity.definitions.MigrationDefinition;
import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Entity(name = "scenario")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class ScenarioData extends ElementData implements Scenario {

  @ManyToMany(
      targetEntity = ScenarioData.class,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "scenario_parts",
      joinColumns = @JoinColumn(name = "composite_id"),
      inverseJoinColumns = @JoinColumn(name = "part_id"))
  @Valid
  @Getter
  private final Set<Scenario> parts = new HashSet<>();

  @Override
  protected void applyItemAspects(TemplateItemAspects itemAspects, Domain domain) {
    setPotentialProbability(
        domain, Optional.ofNullable(itemAspects.scenarioRiskValues()).orElse(new HashMap<>()));
  }

  @Override
  protected TemplateItemAspects mapAspectsToItem(Domain domain) {
    return new TemplateItemAspects(null, null, getPotentialProbability(domain));
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = ScenarioRiskValuesAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private final Set<ScenarioRiskValuesAspectData> riskValuesAspects = new HashSet<>();

  @ManyToMany(targetEntity = ScenarioData.class, mappedBy = "parts", fetch = FetchType.LAZY)
  @Valid
  @Getter
  private final Set<Scenario> composites = new HashSet<>();

  @Override
  public void copyDomainData(
      Domain oldDomain, Domain newDomain, Collection<MigrationDefinition> excludedDefinitions) {
    super.copyDomainData(oldDomain, newDomain, excludedDefinitions);
    Map<RiskDefinitionRef, PotentialProbability> probability = getPotentialProbability(oldDomain);
    // TODO: verince-veo#3381
    List<RiskDefinitionRef> newRiskDefinition =
        newDomain.getRiskDefinitions().values().stream().map(RiskDefinitionRef::from).toList();
    probability.entrySet().removeIf(e -> !newRiskDefinition.contains(e.getKey()));
    setPotentialProbability(newDomain, probability);
  }

  public void setPotentialProbability(
      Domain domain, Map<RiskDefinitionRef, PotentialProbability> potentialProbability) {
    if (potentialProbability.isEmpty()) {
      removeAspectByDomain(riskValuesAspects, domain);
      return;
    }
    var aspect =
        findAspectByDomain(this.riskValuesAspects, domain)
            .orElseGet(
                () -> {
                  var newRiskValues = new ScenarioRiskValuesAspectData(domain, this);
                  this.riskValuesAspects.add(newRiskValues);
                  return newRiskValues;
                });
    aspect.setPotentialProbability(potentialProbability);
  }

  @Override
  public Set<RiskDefinitionRef> getRiskDefinitions(Domain domain) {
    return findAspectByDomain(riskValuesAspects, domain).stream()
        .flatMap(a -> a.potentialProbability.keySet().stream())
        .collect(Collectors.toSet());
  }

  @Override
  public boolean removeRiskDefinition(RiskDefinitionRef riskDefinition, Domain domain) {
    return findAspectByDomain(riskValuesAspects, domain)
        .map(rv -> rv.potentialProbability.remove(riskDefinition) != null)
        .orElse(false);
  }

  public Map<RiskDefinitionRef, PotentialProbability> getPotentialProbability(Domain domain) {
    return findAspectByDomain(riskValuesAspects, domain)
        .map(ScenarioRiskValuesAspectData::getPotentialProbability)
        .orElse(Map.of());
  }

  @Override
  public void transferToDomain(Domain oldDomain, Domain newDomain) {
    findAspectByDomain(riskValuesAspects, oldDomain).ifPresent(a -> a.setDomain(newDomain));
    super.transferToDomain(oldDomain, newDomain);
  }
}
