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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.aspects.ElementDomainAssociation;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.exception.NotFoundException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configurable logic for a specific decision on a type of element. Accepts an element and checks it
 * against a list of rules to determine a boolean result value. The first rule (i.e. with the
 * highest priority) that matches determines the result (first hit policy).
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Decision {
  @NotNull private TranslatedText name;

  @NotNull private ElementType elementType;

  @NotNull
  @Size(max = ElementDomainAssociation.SUB_TYPE_MAX_LENGTH)
  private String elementSubType;

  /** Rules ordered by priority (descending). */
  @NotNull private List<Rule> rules = new LinkedList<>();

  /** The decision result value in the case that none of the rules apply (can be null) */
  private Boolean defaultResultValue;

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
        .orElse(new DecisionResult(defaultResultValue));
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
    return getElementType().equals(element.getType())
        && getElementSubType().equals(element.findSubType(domain).orElse(null));
  }

  /** Determines whether this decision may yield a different result after given event. */
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return rules.stream().anyMatch(r -> r.isAffectedByEvent(event, domain));
  }

  /**
   * @throws NotFoundException if a reference inside this decision cannot be resolved
   * @throws IllegalArgumentException for other validation errors
   */
  public void selfValidate(DomainBase domain) {
    domain.getElementTypeDefinition(getElementType()).getSubTypeDefinition(getElementSubType());
    rules.forEach(r -> r.selfValidate(domain, getElementType()));
  }
}
