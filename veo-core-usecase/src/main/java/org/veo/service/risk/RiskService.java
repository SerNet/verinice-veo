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
import java.util.Objects;
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
      boolean publishEntityEvent = false;
      var entityEvent = new RiskAffectingElementChangeEvent(process, this);

      for (ProcessRisk risk : process.getRisks()) {
        Scenario scenario = risk.getScenario();

        for (Domain domain : risk.getDomains()) {
          log.info("Determine values for {} of {} in {}", risk, process, domain);

          for (RiskDefinition riskDefinition : domain.getRiskDefinitions().values()) {

            RiskDefinitionRef rdr = RiskDefinitionRef.from(riskDefinition);
            if (!risk.getRiskDefinitions().contains(rdr)) {
              log.debug(
                  "Skipping the domain's risk definition {} because it is "
                      + "unused in the risk for process {} / scenario {}.",
                  rdr.getIdRef(),
                  risk.getEntity().getIdAsString(),
                  risk.getScenario().getIdAsString());
              continue;
            }
            publishEntityEvent = true;
            var riskEvent = new RiskChangedEvent(risk, this);
            riskEvent = riskEvent.withDomainId(domain.getId()).withRiskDefinition(rdr);

            // Setting values does not increase the risk's (aggregate root's) version,
            // we have to do it manually:
            risk.setUpdatedAt(Instant.now());

            // Transfer potentialProbability from scenario to riskValues if present:
            ProbabilityValueProvider riskValueProbability = risk.getProbabilityProvider(rdr);

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
            ProbabilityRef riskValueEffectiveProbability =
                riskValueProbability.getEffectiveProbability();

            // Iterate over impact categories:
            CategorizedImpactValueProvider riskValueImpact = risk.getImpactProvider(rdr);
            for (CategoryDefinition categoryDefinition : riskDefinition.getCategories()) {
              CategoryRef cr = CategoryRef.from(categoryDefinition);

              // Transfer potentialImpact from process to riskValues if present:
              ImpactRef newImpact =
                  process
                      .getImpactValues(domain, rdr)
                      .map(ProcessImpactValues::getPotentialImpacts)
                      .map(it -> it.get(cr))
                      .orElse(null);
              if (!Objects.equals(newImpact, riskValueImpact.getPotentialImpact(cr))) {
                riskEvent.addChange(IMPACT_VALUES_CHANGED);
              }
              riskValueImpact.setPotentialImpact(cr, newImpact);

              // Retrieve the resulting effectiveImpact:
              ImpactRef effectiveImpact = riskValueImpact.getEffectiveImpact(cr);

              // Cast to implementing classes to gain package-private access to field
              // 'inherentRisk':
              DeterminedRiskImpl riskForCategory =
                  (DeterminedRiskImpl)
                      ((RiskValuesProvider) risk.getRiskProvider(rdr)).riskCategoryById(cr);

              // Calculate riskValue using the riskDefinition and set it
              // as the inherentRisk:
              if (riskValueEffectiveProbability != null && effectiveImpact != null) {
                RiskRef newRiskValue =
                    RiskRef.from(
                        categoryDefinition.getRiskValue(
                            riskValueEffectiveProbability, effectiveImpact));
                if (!Objects.equals(riskForCategory.getInherentRisk(), newRiskValue)) {
                  riskEvent.addChange(RISK_VALUES_CHANGED);
                }
                riskForCategory.setInherentRisk(newRiskValue);
              } else {
                if (riskForCategory.getInherentRisk() != null) {
                  riskEvent.addChange(RISK_VALUES_CHANGED);
                }
                riskForCategory.setInherentRisk(null);
              }
            }
            if (!riskEvent.getChanges().isEmpty()) {
              entityEvent.addChangedRisk(riskEvent);
              eventPublisher.publish(riskEvent);
            }
          }
        }
      }
      if (publishEntityEvent) eventPublisher.publish(entityEvent);
    }
  }
}
