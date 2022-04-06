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

import java.util.HashMap;
import java.util.Map;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;

import lombok.RequiredArgsConstructor;

/**
 * Runs all applicable decisions for an element and gathers the results.
 */
@RequiredArgsConstructor
public class Decider {
    public Map<DecisionRef, DecisionResult> decide(Element element, Domain domain) {
        var results = new HashMap<DecisionRef, DecisionResult>();
        domain.getDecisions()
              .forEach((key, decision) -> {
                  if (decision.getElementType()
                              .equals(element.getModelType())
                          && decision.getElementSubType()
                                     .equals(element.getSubType(domain)
                                                    .orElse(null))) {
                      results.put(new DecisionRef(key, domain), decision.evaluate(element, domain));
                  }
              });
        return results;
    }
}
