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
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@Entity
public abstract class RiskAffectedData<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
    extends ElementData implements RiskAffected<T, R> {

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
  public boolean associateWithDomain(
      @NonNull DomainTemplate domain, String subType, String status) {
    var added = super.associateWithDomain(domain, subType, status);
    if (added && domain instanceof Domain d) {
      risks.forEach(r -> r.addToDomains(d));
    }
    return added;
  }
}
