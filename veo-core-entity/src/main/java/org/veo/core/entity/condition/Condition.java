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

import static java.util.stream.Collectors.joining;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.exception.NotFoundException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Configurable condition which checks elements using an injectable input provider and matcher. */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Condition {
  @Valid @NotNull private VeoExpression inputProvider;
  @Valid @NotNull private InputMatcher inputMatcher;

  /**
   * Determines whether the data provided by the {@link VeoExpression} for the given element is
   * matched by the {@link InputMatcher}.
   */
  public boolean matches(Element element, Domain domain) {
    return inputMatcher.matches(inputProvider.getValue(element, domain));
  }

  /** Determines whether this condition may yield a different result after given event. */
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return inputProvider.isAffectedByEvent(event, domain);
  }

  /**
   * @throws NotFoundException if a reference inside this condition cannot be resolved
   * @throws IllegalArgumentException for other validation errors
   */
  public void selfValidate(DomainBase domain, String elementType) {
    inputProvider.selfValidate(domain, elementType);
    var supportedTypes = inputMatcher.getSupportedTypes();
    var inputType = inputProvider.getValueType(domain, elementType);
    if (supportedTypes.stream().noneMatch(t -> t.isAssignableFrom(inputType))) {
      throw new IllegalArgumentException(
          "Provider yields %s, but matcher only supports [%s]"
              .formatted(
                  inputType.getSimpleName(),
                  supportedTypes.stream()
                      .map(Class::getSimpleName)
                      .sorted()
                      .collect(joining(","))));
    }
  }
}
