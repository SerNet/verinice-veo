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

import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * References a {@link Rule} on a {@link Decision} using the index the rule has in the decision's
 * list of rules.
 */
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Value
@Schema(description = "Index of a rule in a decision's rules list", type = "integer")
public class DecisionRuleRef {
  @Getter @JsonValue private final int index;

  public DecisionRuleRef(int index, Decision decision) {
    this(index);
    if (decision.getRules().size() <= index) {
      throw new IllegalArgumentException();
    }
  }
}
