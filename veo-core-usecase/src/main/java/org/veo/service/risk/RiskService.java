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

import java.time.Instant;
import java.util.Set;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
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
import org.veo.core.entity.riskdefinition.RiskValue;
import org.veo.core.repository.ProcessRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RiskService {

  private final ProcessRepository processRepository;

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

            // Setting values does not increase the risk's (aggregate root's) version,
            // we have to do it manually:
            risk.setUpdatedAt(Instant.now());

            // Transfer potentialProbability from scenario to riskValues if present:
            ProbabilityValueProvider riskValueProbability = risk.getProbabilityProvider(rdr);
            riskValueProbability.setPotentialProbability(
                scenario
                    .getPotentialProbability(domain, rdr)
                    .map(PotentialProbabilityImpl::getPotentialProbability)
                    .orElse(null));

            // Retrieve the resulting effective probability:
            ProbabilityRef riskValueEffectiveProbability =
                riskValueProbability.getEffectiveProbability();

            // Iterate over impact categories:
            CategorizedImpactValueProvider riskValueImpact = risk.getImpactProvider(rdr);
            for (CategoryDefinition categoryDefinition : riskDefinition.getCategories()) {
              CategoryRef cr = CategoryRef.from(categoryDefinition);

              // Transfer potentialImpact from process to riskValues if present:
              riskValueImpact.setPotentialImpact(
                  cr,
                  process
                      .getImpactValues(domain, rdr)
                      .map(ProcessImpactValues::getPotentialImpacts)
                      .map(it -> it.get(cr))
                      .orElse(null));

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
                RiskValue riskValue =
                    categoryDefinition.getRiskValue(riskValueEffectiveProbability, effectiveImpact);
                riskForCategory.setInherentRisk(RiskRef.from(riskValue));
              } else {
                riskForCategory.setInherentRisk(null);
              }
            }
          }
        }
      }
    }
  }
}
