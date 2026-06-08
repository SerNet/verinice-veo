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
package org.veo.core.entity.condition;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.exception.UnprocessableDataException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Provides the element's result value of a certain type of decision. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Data
public class DecisionResultValueExpression implements VeoExpression {
  @NotNull DecisionRef decision;

  private static final ScopedValue<List<DecisionRef>> DECISION_REFS = ScopedValue.newInstance();

  @Override
  public Object getValue(Element element, Domain domain) {
    var result = element.evaluateDecision(decision, domain);
    if (result == null) {
      return null;
    }
    return result.getValue();
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    List<DecisionRef> previouslyVisitedDecisions = DECISION_REFS.orElse(Collections.emptyList());
    var circularDependency = previouslyVisitedDecisions.contains(decision);
    var visitedDecisions =
        Stream.concat(previouslyVisitedDecisions.stream(), Stream.of(decision)).toList();
    if (circularDependency) {
      throw new IllegalArgumentException(
          "Circular decision dependency detected: %s requires ..."
              .formatted(
                  visitedDecisions.stream()
                      .map(DecisionRef::getKeyRef)
                      .map("'%s'"::formatted)
                      .collect(Collectors.joining(" requires "))));
    }
    var decisionObject = domain.getDecision(decision.getKeyRef());
    if (decisionObject.getElementType() != elementType) {
      throw new UnprocessableDataException(
          "Decision '%s' is defined for %s and cannot be evaluated for %s."
              .formatted(
                  decision,
                  decisionObject.getElementType().getPluralTerm(),
                  elementType.getPluralTerm()));
    }
    // This will detect circular decision dependencies.
    ScopedValue.where(DECISION_REFS, visitedDecisions)
        .run(() -> decisionObject.selfValidate(domain));
  }

  @Override
  public Class<?> getValueType(DomainBase domain, ElementType elementType) {
    return Boolean.class;
  }
}
