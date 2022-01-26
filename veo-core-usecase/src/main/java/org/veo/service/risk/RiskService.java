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

import java.util.Map;
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
            determineAllRiskValues(element.getOwningClient()
                                          .orElseThrow());
        }
    }

    private void determineAllRiskValues(Client client) {
        log.info("Determine all risk values for {}", client);
        Set<Process> processes = processRepository.findAllHavingRisks(client);
        for (Process process : processes) {

            for (ProcessRisk risk : process.getRisks()) {
                Scenario scenario = risk.getScenario();

                for (Domain domain : risk.getDomains()) {
                    log.info("Determine values for {} of {} in {}", risk, process, domain);
                    Map<String, RiskDefinition> riskDefinitions = domain.getRiskDefinitions();
                    Map<RiskDefinitionRef, PotentialProbabilityImpl> probability = scenario.getPotentialProbability(domain)
                                                                                           .orElseThrow();

                    for (RiskDefinition riskDefinition : riskDefinitions.values()) {
                        RiskDefinitionRef rdr = RiskDefinitionRef.from(riskDefinition);
                        PotentialProbabilityImpl potentialProbabilityImpl = probability.get(rdr);

                        ProbabilityValueProvider probabilityProvider = risk.getProbabilityProvider(rdr);
                        probabilityProvider.setPotentialProbability(potentialProbabilityImpl.getPotentialProbability());

                        CategorizedImpactValueProvider impactProvider = risk.getImpactProvider(rdr);
                        RiskValuesProvider riskValueProvider = (RiskValuesProvider) risk.getRiskProvider(rdr);
                        Map<CategoryRef, ImpactRef> potentialImpacts = process.getImpactValues(domain)
                                                                              .orElseThrow()
                                                                              .get(rdr)
                                                                              .getPotentialImpacts();
                        ProbabilityRef effectiveProbability = probabilityProvider.getEffectiveProbability();
                        for (CategoryDefinition categoryDefinition : riskDefinition.getCategories()) {
                            CategoryRef cr = CategoryRef.from(categoryDefinition);
                            impactProvider.setPotentialImpact(cr, potentialImpacts.get(cr));
                            DeterminedRiskImpl riskCategory = (DeterminedRiskImpl) riskValueProvider.riskCategoryById(cr);

                            ImpactRef effectiveImpact = impactProvider.getEffectiveImpact(cr);

                            if (effectiveProbability != null && effectiveImpact != null) {
                                RiskValue riskValue = categoryDefinition.getRiskValue(effectiveProbability,
                                                                                      effectiveImpact);
                                riskCategory.setInherentRisk(RiskRef.from(riskValue));
                            } else {
                                riskCategory.setInherentRisk(null);
                            }
                        }
                    }
                }
            }
        }
    }

}
