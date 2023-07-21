/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Domain;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.RiskRelated;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@Entity
public abstract class RiskAffectedData<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
    extends ElementData implements RiskAffected<T, R>, RiskRelated {

  @Override
  public void transferToDomain(Domain oldDomain, Domain newDomain) {
    super.transferToDomain(oldDomain, newDomain);
    findAspectByDomain(riskValuesAspects, oldDomain).ifPresent(a -> a.setDomain(newDomain));
    risks.forEach(r -> r.transferToDomain(oldDomain, newDomain));
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = ImpactValuesAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  protected final Set<ImpactValuesAspectData> riskValuesAspects = new HashSet<>();

  @Override
  public void setImpactValues(Domain domain, Map<RiskDefinitionRef, ImpactValues> riskValues) {
    var aspect =
        findAspectByDomain(riskValuesAspects, domain)
            .orElseGet(
                () -> {
                  var newAspect = new ImpactValuesAspectData(domain, this);
                  riskValuesAspects.add(newAspect);
                  return newAspect;
                });
    aspect.setValues(riskValues);
  }

  public Optional<Map<RiskDefinitionRef, ImpactValues>> getImpactValues(Domain domain) {
    return findAspectByDomain(riskValuesAspects, domain).map(ImpactValuesAspectData::getValues);
  }

  @Override
  public Optional<ImpactValues> getImpactValues(Domain domain, RiskDefinitionRef riskDefinition) {
    return getImpactValues(domain)
        .map(impactValuesByRiskDefinition -> impactValuesByRiskDefinition.get(riskDefinition));
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = AbstractRiskData.class,
      mappedBy = "entity",
      fetch = FetchType.LAZY)
  private final Set<R> risks = new HashSet<>();

  @Override
  public Set<R> getRisks() {
    return risks;
  }

  @Override
  public R obtainRisk(Scenario scenario, Domain domain) {
    scenario.checkSameClient(this);
    isDomainValid(domain);
    var risk =
        risks.stream()
            .filter(r -> r.getScenario().equals(scenario))
            .findAny()
            .orElseGet(
                () -> {
                  var riskData = createRisk(scenario);
                  addRisk(riskData);
                  return riskData;
                });
    risk.addToDomains(domain);
    return risk;
  }

  abstract R createRisk(Scenario scenario);

  @Override
  public void associateWithDomain(@NonNull Domain domain, String subType, String status) {
    super.associateWithDomain(domain, subType, status);
    risks.forEach(r -> r.addToDomains(domain));
  }

  @Override
  public boolean removeFromDomains(Domain domain) {
    boolean removed = super.removeFromDomains(domain);
    if (removed) {
      getRisks().forEach(r -> r.removeFromDomains(domain));
    }
    return removed;
  }

  @Override
  public boolean removeRiskDefinition(RiskDefinitionRef riskDefinition, Domain domain) {
    return getRisks().stream()
        .map(r -> r.removeRiskDefinition(riskDefinition, domain))
        .toList()
        .contains(true);
  }
}
