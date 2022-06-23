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

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.event.RiskChangedEvent;
import org.veo.core.entity.risk.CategorizedImpactValueProvider;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.DeterminedRiskImpl;
import org.veo.core.entity.risk.ImpactRef;
import org.veo.core.entity.risk.PotentialProbabilityImpl;
import org.veo.core.entity.risk.ProbabilityRef;
import org.veo.core.entity.risk.ProbabilityValueProvider;
import org.veo.core.entity.risk.ProcessImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.RiskRef;
import org.veo.core.entity.risk.RiskValuesProvider;
import org.veo.core.entity.riskdefinition.CategoryDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.service.EventPublisher;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RiskService {

  private final ProcessRepository processRepository;

  private final EventPublisher eventPublisher;

  public void evaluateChangedRiskComponent(Element element) {
    Class<? extends Identifiable> type = element.getModelInterface();
    if (Process.class.isAssignableFrom(type) || Scenario.class.isAssignableFrom(type)) {
      determineAllRiskValues(element.getOwningClient().orElseThrow());
    }
  }

  private void determineAllRiskValues(Client client) {
    log.info("Determine all risk values for {}", client);
    Set<Process> processes = processRepository.findAllHavingRisks(client);
    log.debug(
        "Select {} processes for risk calculation in client {}.",
        processes.size(),
        client.getIdAsString());

    for (Process process : processes) {
      calculateValuesForProcess(process);
    }
  }

  private void calculateValuesForProcess(Process process) {
    var entityEvent = new RiskAffectingElementChangeEvent(process, this);
    for (ProcessRisk risk : process.getRisks()) {
      var events = calculateValuesForRisk(process, risk);
      events.forEach(entityEvent::addChangedRisk);
    }
    if (entityEvent.hasChangedRisks()) {
      eventPublisher.publish(entityEvent);
    }
  }

  private Set<RiskChangedEvent> calculateValuesForRisk(Process process, ProcessRisk risk) {
    Set<RiskChangedEvent> riskEvents = new HashSet<>();
    Scenario scenario = risk.getScenario();
    for (Domain domain : risk.getDomains()) {
      riskEvents.addAll(calculateValuesForDomain(process, risk, scenario, domain));
    }
    return riskEvents;
  }

  private Set<RiskChangedEvent> calculateValuesForDomain(
      Process process, ProcessRisk risk, Scenario scenario, Domain domain) {
    log.info("Determine values for {} of {} in {}", risk, process, domain);
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
                + "unused in the risk for process {} / scenario {}.",
            rdr.getIdRef(),
            risk.getEntity().getIdAsString(),
            risk.getScenario().getIdAsString());
      }
    }
    return riskEvents;
  }

  private Optional<RiskChangedEvent> calculateValuesForRiskDefinition(
      Process process,
      ProcessRisk risk,
      Scenario scenario,
      Domain domain,
      RiskDefinition riskDefinition) {
    var riskEvent = new RiskChangedEvent(risk, this);
    var riskDefRef = RiskDefinitionRef.from(riskDefinition);
    riskEvent = riskEvent.withDomainId(domain.getId()).withRiskDefinition(riskDefRef);

    // Setting values does not increase the risk's (aggregate root's) version,
    // we have to do it manually:
    risk.setUpdatedAt(Instant.now());

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
      eventPublisher.publish(riskEvent);
      return Optional.of(riskEvent);
    }
    return Optional.empty();
  }

  /** Transfers potentialProbability from scenario to riskValues if present */
  private ProbabilityRef calculateProbability(
      ProcessRisk risk,
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
      Process process,
      ProcessRisk risk,
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
      ProcessRisk risk,
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
      Process process,
      Domain domain,
      RiskDefinitionRef riskDefinitionRef,
      RiskChangedEvent riskEvent,
      CategorizedImpactValueProvider riskValueImpact,
      CategoryRef categoryRef) {
    ImpactRef newImpact =
        process
            .getImpactValues(domain, riskDefinitionRef)
            .map(ProcessImpactValues::getPotentialImpacts)
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
