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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import org.veo.core.entity.Element;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Result of a {@link Decision} for an {@link Element} */
@EqualsAndHashCode
@JsonAutoDetect(fieldVisibility = ANY)
@RequiredArgsConstructor
@Getter
@Schema(
    description = "Result of a decision that was evaluated on an element",
    accessMode = Schema.AccessMode.READ_ONLY)
public class DecisionResult {
  public DecisionResult(Boolean defaultValue) {
    this(defaultValue, null, List.of(), List.of());
  }

  private DecisionResult() {
    this(null);
  }

  /** Actual decision result value */
  @Schema(
      description = "Final result value of the decision. Can be null if result is undetermined.",
      nullable = true)
  final Boolean value;

  /**
   * Index of the rule on the decision that caused this decision result (i.e. the matching rule with
   * the highest priority)
   */
  @Schema(
      description =
          "Decision rule that matched first and therefore determined the result value. Can be null if none of the rules matched.",
      nullable = true)
  final DecisionRuleRef decisiveRule;

  /** Indexes of all rules that matched. */
  @Schema(
      description =
          "All decision rules that matched. Some of them may have been overruled by rules with a higher priority and therefore had no effect on the final result value.")
  @NotNull
  final List<DecisionRuleRef> matchingRules;

  /**
   * Indexes of all rules that matched and would have caused the same decision result value as the
   * decisive rule.
   */
  @Schema(
      description =
          "All matching decision rules that support the final result value. They matched and have output values that are identical to what became the final result value of the decision.")
  @NotNull
  final List<DecisionRuleRef> agreeingRules;
}
