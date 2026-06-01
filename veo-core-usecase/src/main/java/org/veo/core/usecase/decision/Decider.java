/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.usecase.decision;

import java.time.Instant;
import java.util.Map;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.repository.GenericElementRepository;

import lombok.RequiredArgsConstructor;

/** Runs all applicable decisions for an element and gathers the results. */
@RequiredArgsConstructor
public class Decider {
  private final GenericElementRepository elementRepository;

  /**
   * Updates all decisions on given element.
   *
   * @return new decision results
   */
  public Map<DecisionRef, DecisionResult> decide(Element element, Domain domain) {
    // TODO #4837 provide repo access to the evaluation
    element.evaluateDecisions(domain, null);
    return element.getDecisionResults(domain);
  }

  /**
   * Reevaluates all decisions on an element that are affected by given event and updates the
   * decision results on the element accordingly.
   */
  public void updateDecisions(ElementEvent event) {
    elementRepository
        .findById(event.getEntityId(), event.getEntityType(), event.getClientId())
        .ifPresent(
            element ->
                element.getDomains().stream()
                    .filter(
                        d ->
                            !(event instanceof RiskAffectingElementChangeEvent riskEvent)
                                || riskEvent.getDomain() == null
                                || riskEvent.getDomain().equals(d))
                    .forEach(
                        domain -> {
                          if (element.evaluateDecisions(domain, event)) {
                            element.setUpdatedAt(Instant.now());
                          }
                        }));
  }
}
