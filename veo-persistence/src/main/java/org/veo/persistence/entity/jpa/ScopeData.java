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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.entity.TemplateItemAspects;
import org.veo.core.entity.domainmigration.DomainSpecificValueLocation;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.Getter;
import lombok.ToString;

@Entity(name = "SCOPE")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class ScopeData extends RiskAffectedData<Scope, ScopeRisk> implements Scope {

  @ManyToMany(
      targetEntity = ElementData.class,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "scope_members",
      joinColumns = @JoinColumn(name = "scope_id"),
      inverseJoinColumns = @JoinColumn(name = "member_id"))
  @Valid
  @Getter
  private final Set<Element> members = new HashSet<>();

  @Override
  public Set<RiskDefinitionRef> getRiskDefinitions(Domain domain) {
    return Stream.concat(
            super.getRiskDefinitions(domain).stream(), getRiskDefinition(domain).stream())
        .collect(Collectors.toSet());
  }

  @Override
  public boolean removeRiskDefinition(RiskDefinitionRef riskDefinition, Domain domain) {
    return super.removeRiskDefinition(riskDefinition, domain)
        | getRiskDefinition(domain)
            .filter(riskDefinition::equals)
            .map(
                rd -> {
                  setRiskDefinition(domain, null);
                  return true;
                })
            .orElse(false);
  }

  public boolean removeMemberById(UUID id) {
    return removeMembersById(Set.of(id));
  }

  public boolean removeMembersById(Collection<UUID> ids) {
    return members.removeIf(compositeEntity -> ids.contains(compositeEntity.getId()));
  }

  @Override
  ScopeRisk createRisk(Scenario scenario) {
    return new ScopeRiskData(this, scenario);
  }

  @Override
  protected TemplateItemAspects mapAspectsToItem(Domain domain) {
    return super.mapAspectsToItem(domain)
        .withScopeRiskDefinition(getRiskDefinition(domain).orElse(null));
  }

  @Override
  protected void applyItemAspects(TemplateItemAspects itemAspects, Domain domain) {
    super.applyItemAspects(itemAspects, domain);
    setRiskDefinition(domain, itemAspects.scopeRiskDefinition());
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = ScopeRiskValuesAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private final Set<ScopeRiskValuesAspectData> scopeRiskValuesAspects = new HashSet<>();

  @Override
  public void copyDomainData(
      Domain oldDomain,
      Domain newDomain,
      Collection<DomainSpecificValueLocation> excludedDefinitions) {
    super.copyDomainData(oldDomain, newDomain, excludedDefinitions);
    Optional<RiskDefinitionRef> riskDefinition = getRiskDefinition(oldDomain);
    // TODO: verince-veo#3381
    List<RiskDefinitionRef> newRiskDefinition =
        newDomain.getRiskDefinitions().values().stream().map(RiskDefinitionRef::from).toList();
    riskDefinition.ifPresent(
        rd -> {
          if (newRiskDefinition.contains(rd)) {
            setRiskDefinition(newDomain, rd);
          }
        });
  }

  @Override
  public void setRiskDefinition(Domain domain, RiskDefinitionRef riskDefinition) {
    var aspect =
        findAspectByDomain(scopeRiskValuesAspects, domain)
            .orElseGet(
                () -> {
                  var newAspect = new ScopeRiskValuesAspectData(domain, this);
                  scopeRiskValuesAspects.add(newAspect);
                  return newAspect;
                });
    aspect.setRiskDefinitionRef(riskDefinition);
  }

  @Override
  public Optional<RiskDefinitionRef> getRiskDefinition(Domain domain) {
    return findAspectByDomain(scopeRiskValuesAspects, domain)
        .map(ScopeRiskValuesAspectData::getRiskDefinitionRef);
  }

  @Override
  public boolean removeFromDomains(Domain domain) {
    boolean removed = super.removeFromDomains(domain);
    if (removed) {
      removeAspectByDomain(scopeRiskValuesAspects, domain);
    }
    return removed;
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
