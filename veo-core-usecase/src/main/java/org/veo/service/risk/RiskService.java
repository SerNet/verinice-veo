/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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
package org.veo.service.risk;

import static org.veo.core.entity.event.RiskEvent.ChangedValues.IMPACT_VALUES_CHANGED;
import static org.veo.core.entity.event.RiskEvent.ChangedValues.PROBABILITY_VALUES_CHANGED;
import static org.veo.core.entity.event.RiskEvent.ChangedValues.RISK_VALUES_CHANGED;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Process;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.event.RiskChangedEvent;
import org.veo.core.entity.risk.CategorizedImpactValueProvider;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.DeterminedRiskImpl;
import org.veo.core.entity.risk.ImpactRef;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.PotentialProbabilityImpl;
import org.veo.core.entity.risk.ProbabilityRef;
import org.veo.core.entity.risk.ProbabilityValueProvider;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.RiskRef;
import org.veo.core.entity.risk.RiskValuesProvider;
import org.veo.core.entity.riskdefinition.CategoryDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.repository.AssetRepository;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.repository.ScopeRepository;
import org.veo.core.service.EventPublisher;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RiskService {

  private final ProcessRepository processRepository;
  private final AssetRepository assetRepository;
  private final ScopeRepository scopeRepository;

  private final EventPublisher eventPublisher;

  public void evaluateChangedRiskComponent(Element element) {
    Class<? extends Identifiable> type = element.getModelInterface();

    if (Asset.class.isAssignableFrom(type)) {
      assetRepository.findWithRisksAndScenarios(Set.of(element.getId()));
      calculateValuesForObject((Asset) element);
    } else if (Scope.class.isAssignableFrom(type)) {
      scopeRepository.findWithRisksAndScenarios(Set.of(element.getId()));
      calculateValuesForObject((Scope) element);
    } else if (Process.class.isAssignableFrom(type)) {
      processRepository.findWithRisksAndScenarios(Set.of(element.getId()));
      calculateValuesForObject((Process) element);
    } else if (Scenario.class.isAssignableFrom(type)) {
      Set<Process> processes = processRepository.findByRisk((Scenario) element);
      processRepository.findWithRisksAndScenarios(
          processes.stream().map(Process::getId).collect(Collectors.toSet()));

      Set<Asset> assets = assetRepository.findByRisk((Scenario) element);
      assetRepository.findWithRisksAndScenarios(
          assets.stream().map(Asset::getId).collect(Collectors.toSet()));

      Set<Scope> scopes = scopeRepository.findByRisk((Scenario) element);
      scopeRepository.findWithRisksAndScenarios(
          scopes.stream().map(Scope::getId).collect(Collectors.toSet()));

      Set<RiskAffected<?, ?>> elements = new HashSet<>();
      elements.addAll(scopes);
      elements.addAll(processes);
      elements.addAll(assets);

      for (RiskAffected<?, ?> e : elements) {
        calculateValuesForObject(e);
      }
    }
  }

  private void calculateValuesForObject(RiskAffected<?, ?> process) {
    var entityEvent = new RiskAffectingElementChangeEvent(process, this);
    for (AbstractRisk<?, ?> risk : process.getRisks()) {
      var events = calculateValuesForRisk(process, risk);
      events.forEach(entityEvent::addChangedRisk);
    }
    if (entityEvent.hasChangedRisks()) {
      eventPublisher.publish(entityEvent);
    }
  }

  private Set<RiskChangedEvent> calculateValuesForRisk(
      RiskAffected<?, ?> process, AbstractRisk<?, ?> risk) {
    Set<RiskChangedEvent> riskEvents = new HashSet<>();
    Scenario scenario = risk.getScenario();
    for (Domain domain : risk.getDomains()) {
      riskEvents.addAll(calculateValuesForDomain(process, risk, scenario, domain));
    }
    return riskEvents;
  }

  private Set<RiskChangedEvent> calculateValuesForDomain(
      RiskAffected<?, ?> process, AbstractRisk<?, ?> risk, Scenario scenario, Domain domain) {
    log.debug("Determine values for {} of {} in {}", risk, process, domain);
    Set<RiskChangedEvent> riskEvents = new HashSet<>();

    for (RiskDefinition riskDefinition : domain.getRiskDefinitions().values()) {
      RiskDefinitionRef rdr = RiskDefinitionRef.from(riskDefinition);
      if (risk.getRiskDefinitions(domain).contains(rdr)) {
        var riskEvent =
            calculateValuesForRiskDefinition(process, risk, scenario, domain, riskDefinition);
        riskEvent.ifPresent(riskEvents::add);
      } else {
        log.debug(
            "Skipping the domain's risk definition {} because it is "
                + "unused in the risk for object {} / scenario {}.",
            rdr.getIdRef(),
            risk.getEntity().getIdAsString(),
            risk.getScenario().getIdAsString());
      }
    }
    return riskEvents;
  }

  private Optional<RiskChangedEvent> calculateValuesForRiskDefinition(
      RiskAffected<?, ?> process,
      AbstractRisk<?, ?> risk,
      Scenario scenario,
      Domain domain,
      RiskDefinition riskDefinition) {
    var riskEvent = new RiskChangedEvent(risk, this);
    var riskDefRef = RiskDefinitionRef.from(riskDefinition);
    riskEvent = riskEvent.withDomainId(domain.getId()).withRiskDefinition(riskDefRef);

    ProbabilityRef riskValueEffectiveProbability =
        calculateProbability(risk, scenario, domain, riskDefRef, riskEvent);

    // Iterate over impact categories:
    for (CategoryDefinition categoryDefinition : riskDefinition.getCategories()) {
      calculateValuesForCategory(
          process,
          risk,
          domain,
          riskDefRef,
          riskEvent,
          riskValueEffectiveProbability,
          categoryDefinition);
    }

    if (!riskEvent.getChanges().isEmpty()) {
      // Setting values does not increase the risk's (aggregate root's) version,
      // we have to do it manually:
      risk.setUpdatedAt(Instant.now());
      eventPublisher.publish(riskEvent);
      return Optional.of(riskEvent);
    }
    return Optional.empty();
  }

  /** Transfers potentialProbability from scenario to riskValues if present */
  private ProbabilityRef calculateProbability(
      AbstractRisk<?, ?> risk,
      Scenario scenario,
      Domain domain,
      RiskDefinitionRef rdr,
      RiskChangedEvent riskEvent) {
    ProbabilityValueProvider riskValueProbability = risk.getProbabilityProvider(rdr, domain);

    ProbabilityRef newProbability =
        scenario
            .getPotentialProbability(domain, rdr)
            .map(PotentialProbabilityImpl::getPotentialProbability)
            .orElse(null);
    if (!Objects.equals(newProbability, riskValueProbability.getPotentialProbability())) {
      riskEvent.addChange(PROBABILITY_VALUES_CHANGED);
    }

    riskValueProbability.setPotentialProbability(newProbability);

    // Retrieve the resulting effective probability:
    return riskValueProbability.getEffectiveProbability();
  }

  private void calculateValuesForCategory(
      RiskAffected<?, ?> process,
      AbstractRisk<?, ?> risk,
      Domain domain,
      RiskDefinitionRef riskDefinitionRef,
      RiskChangedEvent riskEvent,
      ProbabilityRef riskValueEffectiveProbability,
      CategoryDefinition categoryDefinition) {
    CategoryRef categoryRef = CategoryRef.from(categoryDefinition);
    var riskValueImpact = risk.getImpactProvider(riskDefinitionRef, domain);

    ImpactRef effectiveImpact =
        calculateImpact(
            process, domain, riskDefinitionRef, riskEvent, riskValueImpact, categoryRef);

    calculateRisk(
        risk,
        riskDefinitionRef,
        riskEvent,
        domain,
        riskValueEffectiveProbability,
        categoryDefinition,
        effectiveImpact);
  }

  /* Calculates riskValue using the riskDefinition and sets it as the inherentRisk. */
  private void calculateRisk(
      AbstractRisk<?, ?> risk,
      RiskDefinitionRef riskDefinitionRef,
      RiskChangedEvent riskEvent,
      Domain domain,
      ProbabilityRef riskValueEffectiveProbability,
      CategoryDefinition categoryDefinition,
      ImpactRef effectiveImpact) {
    var category = CategoryRef.from(categoryDefinition);

    // Cast to implementing classes to gain package-private access to field
    // 'inherentRisk':
    DeterminedRiskImpl riskForCategory =
        (DeterminedRiskImpl)
            ((RiskValuesProvider) risk.getRiskProvider(riskDefinitionRef, domain))
                .riskCategoryById(category);

    RiskRef inherentRisk =
        determineInherentRisk(categoryDefinition, riskValueEffectiveProbability, effectiveImpact);
    if (!Objects.equals(riskForCategory.getInherentRisk(), inherentRisk)) {
      riskEvent.addChange(RISK_VALUES_CHANGED);
      riskForCategory.setInherentRisk(inherentRisk);
    }
  }

  private RiskRef determineInherentRisk(
      CategoryDefinition categoryDefinition,
      ProbabilityRef riskValueEffectiveProbability,
      ImpactRef effectiveImpact) {
    if (riskValueEffectiveProbability != null && effectiveImpact != null) {
      return RiskRef.from(
          categoryDefinition.getRiskValue(riskValueEffectiveProbability, effectiveImpact));
    }
    return null;
  }

  /* Transfers potentialImpact from process to riskValues if present. */
  private ImpactRef calculateImpact(
      RiskAffected<?, ?> process,
      Domain domain,
      RiskDefinitionRef riskDefinitionRef,
      RiskChangedEvent riskEvent,
      CategorizedImpactValueProvider riskValueImpact,
      CategoryRef categoryRef) {
    ImpactRef newImpact =
        process
            .getImpactValues(domain, riskDefinitionRef)
            .map(ImpactValues::getPotentialImpacts)
            .map(it -> it.get(categoryRef))
            .orElse(null);
    if (!Objects.equals(newImpact, riskValueImpact.getPotentialImpact(categoryRef))) {
      riskEvent.addChange(IMPACT_VALUES_CHANGED);
    }
    riskValueImpact.setPotentialImpact(categoryRef, newImpact);

    // Retrieve the resulting effectiveImpact:
    return riskValueImpact.getEffectiveImpact(categoryRef);
  }
}
