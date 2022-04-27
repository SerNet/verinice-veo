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

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/** Configurable condition which checks elements using an injectable input provider and matcher. */
@Data
@RequiredArgsConstructor
public class Condition {
  private final InputProvider inputProvider;
  private final InputMatcher inputMatcher;

  /**
   * Determines whether the data provided by the {@link InputProvider} for given element is matched
   * by the {@link InputMatcher}.
   */
  public boolean matches(Element element, Domain domain) {
    return inputMatcher.matches(inputProvider.getValue(element, domain));
  }
}
