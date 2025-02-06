/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.entity;

import java.util.Collection;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.condition.VeoExpression;
import org.veo.core.entity.exception.UnprocessableDataException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/** Adds risks for a collection of scenarios to the element. */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NotNull
@Data
@Slf4j
public final class AddRisksStep extends ActionStep {
  @NotNull private VeoExpression scenarios;

  public Collection<Scenario> getScenarios(Element element, Domain domain) {
    var result = scenarios.getValue(element, domain);
    if (result instanceof Collection<?> elements) {
      return elements.stream()
          .map(
              o -> {
                if (o instanceof Scenario s) return s;
                throw new UnprocessableDataException(
                    "Cannot add risk for %s, expected a scenario".formatted(o.getClass()));
              })
          .collect(Collectors.toSet());
    }
    throw new UnprocessableDataException(
        "Cannot add risks for %s, expected a collection of scenarios".formatted(result.getClass()));
  }

  @Override
  void selfValidate(Domain domain, ElementType elementType) {
    if (!ElementType.RISK_AFFECTED_TYPES.contains(elementType)) {
      throw new UnprocessableDataException("Cannot create risks for %s".formatted(elementType));
    }
    scenarios.selfValidate(domain, elementType);
    if (Collection.class.isAssignableFrom(scenarios.getValueType(domain, elementType))) {
      throw new UnprocessableDataException("Invalid expression, must return a collection");
    }
  }
}
