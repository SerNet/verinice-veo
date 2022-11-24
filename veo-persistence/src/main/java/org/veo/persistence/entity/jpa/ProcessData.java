/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.risk.ProcessImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Entity(name = "process")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class ProcessData extends RiskAffectedData<Process, ProcessRisk> implements Process {

  @ManyToMany(
      targetEntity = ProcessData.class,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "process_parts",
      joinColumns = @JoinColumn(name = "composite_id"),
      inverseJoinColumns = @JoinColumn(name = "part_id"))
  @Valid
  @Getter
  private final Set<Process> parts = new HashSet<>();

  @ManyToMany(targetEntity = ProcessData.class, mappedBy = "parts", fetch = FetchType.LAZY)
  @Getter
  private final Set<Process> composites = new HashSet<>();

  @Override
  ProcessRisk createRisk(Scenario scenario) {
    return new ProcessRiskData(this, scenario);
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = ProcessImpactValuesAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private final Set<ProcessImpactValuesAspectData> riskValuesAspects = new HashSet<>();

  @Override
  public void setImpactValues(
      DomainBase domain, Map<RiskDefinitionRef, ProcessImpactValues> riskValues) {
    var aspect =
        findAspectByDomain(riskValuesAspects, domain)
            .orElseGet(
                () -> {
                  var newAspect = new ProcessImpactValuesAspectData(domain, this);
                  riskValuesAspects.add(newAspect);
                  return newAspect;
                });
    aspect.setValues(riskValues);
  }

  public Optional<Map<RiskDefinitionRef, ProcessImpactValues>> getImpactValues(DomainBase domain) {
    return findAspectByDomain(riskValuesAspects, domain)
        .map(ProcessImpactValuesAspectData::getValues);
  }

  @Override
  public Optional<ProcessImpactValues> getImpactValues(
      DomainBase domain, RiskDefinitionRef riskDefinition) {
    return getImpactValues(domain)
        .map(impactValuesByRiskDefinition -> impactValuesByRiskDefinition.get(riskDefinition));
  }

  @Override
  public void transferToDomain(Domain oldDomain, Domain newDomain) {
    super.transferToDomain(oldDomain, newDomain);
    findAspectByDomain(riskValuesAspects, oldDomain).ifPresent(a -> a.setDomain(newDomain));
  }
}
