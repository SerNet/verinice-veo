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

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import org.veo.core.entity.Element;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Result of a {@link Decision} for an {@link Element}
 */
@Getter
@JsonAutoDetect(fieldVisibility = ANY)
@RequiredArgsConstructor
public class DecisionResult {
    public DecisionResult() {
        this(null, null, List.of(), List.of());
    }

    /** Actual decision result value */
    final Boolean value;

    /**
     * Index of the rule on the decision that caused this decision result (i.e. the
     * matching rule with the highest priority)
     */
    final Integer decisiveRule;

    /** Indexes of all rules that matched. */
    @NotNull
    final List<Integer> matchingRules;

    /**
     * Indexes of all rules that matched and would have caused the same decision
     * result value as the decisive rule.
     */
    @NotNull
    final List<Integer> agreeingRules;
}
