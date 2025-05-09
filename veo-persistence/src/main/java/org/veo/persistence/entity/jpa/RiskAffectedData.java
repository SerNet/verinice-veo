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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.TemplateItemAspects;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.ReqImplRef;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.domainmigration.DomainSpecificValueLocation;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@Slf4j
@Entity
public abstract class RiskAffectedData<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
    extends ElementData implements RiskAffected<T, R> {

  @Override
  public void copyDomainData(
      Domain oldDomain,
      Domain newDomain,
      Collection<DomainSpecificValueLocation> excludedDefinitions) {
    super.copyDomainData(oldDomain, newDomain, excludedDefinitions);
    Map<RiskDefinitionRef, ImpactValues> impactValues = new HashMap<>(getImpactValues(oldDomain));
    // TODO: verince-veo#3381
    List<RiskDefinitionRef> newRiskDefinition =
        newDomain.getRiskDefinitions().values().stream().map(RiskDefinitionRef::from).toList();
    impactValues.entrySet().removeIf(e -> !newRiskDefinition.contains(e.getKey()));
    setImpactValues(newDomain, impactValues);
    getRisks().forEach(r -> r.copyAspectData(oldDomain, newDomain));
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = ImpactValuesAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  protected final Set<ImpactValuesAspectData> riskValuesAspects = new HashSet<>();

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = RequirementImplementationData.class,
      mappedBy = "origin",
      fetch = FetchType.LAZY)
  @Valid
  protected final Set<RequirementImplementation> requirementImplementations = new HashSet<>();

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = ControlImplementationData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  protected final Set<ControlImplementation> controlImplementations = new HashSet<>();

  @Override
  public void setImpactValues(Domain domain, Map<RiskDefinitionRef, ImpactValues> riskValues) {
    if (riskValues.isEmpty()) {
      removeAspectByDomain(riskValuesAspects, domain);
      return;
    }
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

  @Override
  public Map<RiskDefinitionRef, ImpactValues> getImpactValues(Domain domain) {
    return findAspectByDomain(riskValuesAspects, domain)
        .map(ImpactValuesAspectData::getValues)
        .orElse(Map.of());
  }

  @Override
  public Optional<ImpactValues> getImpactValues(Domain domain, RiskDefinitionRef riskDefinition) {
    return Optional.ofNullable(getImpactValues(domain).get(riskDefinition));
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
  public R obtainRisk(Scenario scenario) {
    scenario.checkSameClient(this);
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
    return risk;
  }

  @Override
  public ControlImplementation implementControl(Control control) {
    var implementationOptional =
        this.controlImplementations.stream()
            .filter(ci -> ci.getControl().equals(control))
            .findFirst();
    if (implementationOptional.isPresent()) {
      return implementationOptional.get();
    }

    var newCI = ControlImplementationData.createNew(this, control);
    add(newCI);
    return newCI;
  }

  @Override
  public void disassociateControl(Control control) {
    new HashSet<>(controlImplementations)
        .stream()
            .filter(ci -> ci.getControl().equals(control))
            .map(ControlImplementationData.class::cast)
            .forEach(this::remove);
  }

  @Override
  public void removeRequirementImplementation(Control control) {
    findRequirementImplementation(control)
        .ifPresent(
            ri -> {
              controlImplementations.forEach(ci -> ci.remove(ri));
              requirementImplementations.remove(ri);
            });
  }

  private void add(ControlImplementationData ci) {
    this.controlImplementations.add(ci);
    ci.setOwner(this);
  }

  private void remove(ControlImplementationData ci) {
    var control = ci.getControl();
    risks.stream()
        .filter(r -> control.equals(r.getMitigation()))
        .findAny()
        .ifPresent(
            mitigatedRisk -> {
              var scenario = mitigatedRisk.getScenario();
              throw new UnprocessableDataException(
                  "Control '%s' (%s) cannot be disassociated, because it mitigates a risk for scenario '%s' (%s)."
                      .formatted(
                          control.getName(),
                          control.getId(),
                          scenario.getName(),
                          scenario.getId()));
            });
    this.controlImplementations.remove(ci);
    ((RiskAffectedData<T, R>) ci.getOwner())
        .removeUnedited(
            ci.getRequirementImplementations().stream()
                .filter(
                    it ->
                        controlImplementations.stream()
                            .noneMatch(
                                remainingCi ->
                                    remainingCi.getRequirementImplementations().contains(it)))
                .collect(Collectors.toSet()));
    ci.remove();
  }

  abstract R createRisk(Scenario scenario);

  @Override
  public boolean removeFromDomains(Domain domain) {
    boolean removed = super.removeFromDomains(domain);
    if (removed) {
      Set.copyOf(getRisks()).stream().forEach(risk -> risk.removeFromDomains(domain));
      setImpactValues(domain, Collections.emptyMap());
    }
    return removed;
  }

  @Override
  protected TemplateItemAspects mapAspectsToItem(Domain domain) {
    return new TemplateItemAspects(this.getImpactValues(domain), null, null);
  }

  @Override
  protected void applyItemAspects(TemplateItemAspects itemAspects, Domain domain) {
    setImpactValues(
        domain, Optional.ofNullable(itemAspects.impactValues()).orElse(new HashMap<>()));
  }

  @Override
  public Set<RiskDefinitionRef> getRiskDefinitions(Domain domain) {
    return Stream.concat(
            findAspectByDomain(riskValuesAspects, domain).stream()
                .flatMap(a -> a.values.keySet().stream()),
            risks.stream().flatMap(r -> r.getRiskDefinitions(domain).stream()))
        .collect(Collectors.toSet());
  }

  @Override
  public boolean removeRiskDefinition(RiskDefinitionRef riskDefinition, Domain domain) {
    return getRisks().stream()
            .map(r -> r.removeRiskDefinition(riskDefinition, domain))
            .toList()
            .contains(true)
        | findAspectByDomain(riskValuesAspects, domain)
            .map(a -> a.values.remove(riskDefinition) != null)
            .orElse(false);
  }

  /**
   * Add the requirement-implementation to this element. If one already exists for the referenced
   * control, do not add a new one.
   *
   * @param ri the requirem ent-iomplementation that should be added
   * @return the added requirement implementation or the existing one if one was already present
   */
  RequirementImplementation addRequirementImplementation(RequirementImplementation ri) {
    var existingRi =
        requirementImplementations.stream()
            .filter(myRI -> myRI.getControl().equals(ri.getControl()))
            .findFirst();
    if (existingRi.isPresent()) {
      log.debug(
          "Entity {} of type {} already contains a requirement " + "for control {}.",
          this.getIdAsString(),
          this.getModelType(),
          ri.getControl().getIdAsString());
      return existingRi.get();
    }

    this.requirementImplementations.add(ri);
    ri.setOrigin(this);
    return ri;
  }

  private void remove(RequirementImplementation ri) {
    requirementImplementations.remove(ri);
  }

  @Override
  public RequirementImplementation addRequirementImplementation(Control control) {
    return addRequirementImplementation(RequirementImplementationData.createNew(control));
  }

  @Override
  public Set<ControlImplementation> getControlImplementations() {
    return controlImplementations;
  }

  @Override
  public Set<RequirementImplementation> getRequirementImplementations() {
    return requirementImplementations;
  }

  @Override
  public RequirementImplementation getRequirementImplementation(ReqImplRef reqImplRef) {
    return requirementImplementations.stream()
        .filter(ri -> ri.getId().equals(reqImplRef.getUUID()))
        .findFirst()
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Could not find requirement implementation %s", reqImplRef.getUUID()));
  }

  private void removeUnedited(Set<ReqImplRef> toRemove) {
    toRemove.stream()
        .map(this::getRequirementImplementation)
        .filter(RequirementImplementation::isUnedited)
        .forEach(this::remove);
  }

  @Override
  public ControlImplementation getImplementationFor(Control control) {
    return getControlImplementations().stream()
        .filter(i -> i.getControl().equals(control))
        .findFirst()
        .orElseThrow(
            () ->
                new NotFoundException(
                    "%s %s does not implement control %s"
                        .formatted(
                            this.getModelType(), this.getIdAsString(), control.getIdAsString())));
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
