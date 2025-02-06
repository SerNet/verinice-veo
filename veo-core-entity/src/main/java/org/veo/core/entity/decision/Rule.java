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
import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.condition.Condition;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.exception.NotFoundException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configurable rule for a {@link Decision} with a list of conditions and an output value that
 * should become the decision's result value if the rule matches the element (unless another rule
 * takes precedence). An element only matches the rule if any of the rule conditions match.
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Rule {
  /**
   * Should become the decision's result value if this rule matches (unless it is overruled). Can be
   * null.
   */
  private Boolean output;

  /** Translated human-readable texts. Key is ISO language code, value is text. */
  @NotNull private TranslatedText description;

  /** The rule only matches an element if any of these conditions match the element. */
  @NotNull private final List<Condition> conditions = new ArrayList<>();

  /** Determines whether the element matches any rule conditions */
  public boolean matches(Element element, Domain domain) {
    return conditions.stream().anyMatch(c -> c.matches(element, domain));
  }

  /** Compares the output of this rule to given value. */
  public boolean outputEquals(Boolean value) {
    // Equality in Java is tricky!
    if (output == null) {
      return value == null;
    }
    return output.equals(value);
  }

  /** Determines whether this rule may yield a different result after given event. */
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return conditions.stream().anyMatch(c -> c.isAffectedByEvent(event, domain));
  }

  /**
   * @throws NotFoundException if a reference inside this rule cannot be resolved
   * @throws IllegalArgumentException for other validation errors
   */
  public void selfValidate(DomainBase domain, ElementType elementType) {
    conditions.forEach(c -> c.selfValidate(domain, elementType));
  }
}
