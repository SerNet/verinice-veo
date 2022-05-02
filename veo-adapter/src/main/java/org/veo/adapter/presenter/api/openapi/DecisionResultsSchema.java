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
package org.veo.adapter.presenter.api.openapi;

import java.util.List;

import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.entity.decision.DecisionRuleRef;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Result of a decision that was evaluated on an element",
    accessMode = Schema.AccessMode.READ_ONLY)
public abstract class DecisionResultsSchema extends DecisionResult {
  @Override
  @Schema(
      description = "Final result value of the decision. Can be null if result is undetermined.",
      nullable = true)
  public abstract Boolean getValue();

  @Override
  @Schema(
      description =
          "Decision rule that matched first and therefore determined the result value. Can be null if none of the rules matched.",
      nullable = true)
  public abstract DecisionRuleRef getDecisiveRule();

  @Override
  @Schema(
      description =
          "All decision rules that matched. Some of them may have been overruled by rules with a higher priority and therefore had no effect on the final result value.")
  public List<DecisionRuleRef> getMatchingRules() {
    return super.getMatchingRules();
  }

  @Override
  @Schema(
      description =
          "All matching decision rules that support the final result value. They matched and have output values that are identical to what became the final result value of the decision.")
  public List<DecisionRuleRef> getAgreeingRules() {
    return super.getAgreeingRules();
  }
}
