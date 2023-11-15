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
package org.veo.core.entity.inspection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.condition.Condition;
import org.veo.core.entity.condition.DecisionResultValueExpression;
import org.veo.core.entity.condition.EqualsMatcher;
import org.veo.core.entity.condition.InputMatcher;
import org.veo.core.entity.condition.PartCountExpression;
import org.veo.core.entity.condition.VeoExpression;
import org.veo.core.entity.decision.DecisionRef;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Configurable check that can be performed on an {@link Element}. If all {@link Condition}s match
 * on the element, the inspection will yield a {@link Finding} with the configured description,
 * {@link Severity} & {@link Suggestion}s.
 */
@Data
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
@RequiredArgsConstructor
public class Inspection {
  public Inspection(
      Severity severity, TranslatedText description, String elementType, String elementSubType) {
    this(severity, description);
    this.elementType = elementType;
    this.elementSubType = elementSubType;
  }

  @NonNull Severity severity;
  final TranslatedText description;
  String elementType;
  String elementSubType;
  final List<Condition> conditions = new ArrayList<>();
  final List<Suggestion> suggestions = new ArrayList<>();

  public Optional<Finding> run(Element element, Domain domain) {
    if (elementType != null && !elementType.equals(element.getModelType())) {
      return Optional.empty();
    }
    if (elementSubType != null
        && !elementSubType.equals(element.findSubType(domain).orElse(null))) {
      return Optional.empty();
    }
    if (conditions.stream().allMatch(c -> c.matches(element, domain))) {
      return Optional.of(new Finding(severity, description, suggestions));
    }
    return Optional.empty();
  }

  public Inspection addCondition(VeoExpression provider, InputMatcher matcher) {
    conditions.add(new Condition(provider, matcher));
    return this;
  }

  public Inspection ifDecisionResultEquals(Boolean result, DecisionRef decision) {
    return addCondition(new DecisionResultValueExpression(decision), new EqualsMatcher(result));
  }

  public Inspection ifPartAbsent(String subType) {
    return addCondition(new PartCountExpression(subType), new EqualsMatcher(0));
  }

  public Inspection suggestAddingPart(String subType) {
    suggestions.add(new AddPartSuggestion(subType));
    return this;
  }
}
