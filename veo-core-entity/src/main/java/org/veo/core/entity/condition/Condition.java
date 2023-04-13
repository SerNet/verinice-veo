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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.event.ElementEvent;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Configurable condition which checks elements using an injectable input provider and matcher. */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Condition {
  @Valid @NotNull private InputProvider inputProvider;
  @Valid @NotNull private InputMatcher inputMatcher;

  /**
   * Determines whether the data provided by the {@link InputProvider} for the given element is
   * matched by the {@link InputMatcher}.
   */
  public boolean matches(Element element, Domain domain) {
    return inputMatcher.matches(inputProvider.getValue(element, domain));
  }

  /** Determines whether this condition may yield a different result after given event. */
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return inputProvider.isAffectedByEvent(event, domain);
  }
}
