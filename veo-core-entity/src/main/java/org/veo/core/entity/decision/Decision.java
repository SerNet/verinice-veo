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
package org.veo.core.entity.decision;

import static org.veo.core.entity.Element.ELEMENT_TYPE_MAX_LENGTH;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.aspects.SubTypeAspect;
import org.veo.core.entity.event.ElementEvent;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Configurable logic for a specific decision on a type of element. Accepts an element and checks it
 * against a list of rules to determine a boolean result value. The first rule (i.e. with the
 * highest priority) that matches determines the result (first hit policy).
 */
@Data
@RequiredArgsConstructor
public class Decision {
  /** Translated human-readable text. Key is ISO language code, value is text. */
  @NotNull private final Map<String, String> name;

  @Size(max = ELEMENT_TYPE_MAX_LENGTH)
  private final String elementType;

  @Size(max = SubTypeAspect.SUB_TYPE_MAX_LENGTH)
  private final String elementSubType;
  /** Rules ordered by priority (descending). */
  private final List<Rule> rules;

  public DecisionResult evaluate(Element element, Domain domain) {
    // Find all matching rules
    var matchingRules = new ArrayList<DecisionRuleRef>();
    for (var i = 0; i < rules.size(); i++) {
      var rule = rules.get(i);
      if (rule.matches(element, domain)) {
        matchingRules.add(new DecisionRuleRef(i, this));
      }
    }

    // The first matching rule determines the result.
    return matchingRules.stream()
        .findFirst()
        .map(decisiveRuleRef -> buildResult(decisiveRuleRef, matchingRules))
        .orElse(new DecisionResult());
  }

  public Rule getRule(DecisionRuleRef ref) {
    return rules.get(ref.getIndex());
  }

  private DecisionResult buildResult(
      DecisionRuleRef decisiveRuleRef, List<DecisionRuleRef> matchingRules) {
    var value = getRule(decisiveRuleRef).getOutput();
    var agreeingRules =
        matchingRules.stream().filter(ref -> getRule(ref).outputEquals(value)).toList();
    return new DecisionResult(value, decisiveRuleRef, matchingRules, agreeingRules);
  }

  public boolean isApplicableToElement(Element element, Domain domain) {
    return getElementType().equals(element.getModelType())
        && getElementSubType().equals(element.getSubType(domain).orElse(null));
  }

  /** Determines whether this decision may yield a different result after given event. */
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return rules.stream().anyMatch(r -> r.isAffectedByEvent(event, domain));
  }
}
