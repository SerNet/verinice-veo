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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Configurable rule for a {@link Decision} with a list of conditions and an
 * output value that should become the decision's result value if the rule
 * matches the element (unless another rule takes precedence). An element only
 * matches the rule if all the rule conditions match.
 */
@RequiredArgsConstructor
public class Rule {
    /**
     * Should become the decision's result value if this rule matches (unless it is
     * overruled). Can be null.
     */
    @Getter
    private final Boolean output;

    /** Translated human-readable texts. Key is ISO language code, value is text. */
    @Getter
    @NotNull
    private final Map<String, String> description;

    /**
     * The rule only matches an element if all these conditions match the element.
     */
    private final List<RuleCondition> conditions = new ArrayList<>();

    /** Determines whether the element matches all rule conditions */
    public boolean matches(Element element, Domain domain) {
        return conditions.stream()
                         .allMatch(c -> c.matches(element, domain));
    }

    /** Add a condition that a custom aspect attribute must equal given value */
    public Rule ifAttributeEquals(Object comparisonValue, String attributeType,
            String customAspectType) {
        conditions.add(new RuleCondition(
                new CustomAspectAttributeValueProvider(customAspectType, attributeType),
                new EqualsMatcher(comparisonValue)));
        return this;
    }

    /**
     * Add a condition that a custom aspect collection attribute must have a size
     * greater than given integer
     */
    public Rule ifAttributeSizeGreaterThan(int i, String attributeType, String customAspectType) {
        conditions.add(new RuleCondition(
                new CustomAspectAttributeSizeProvider(customAspectType, attributeType),
                new GreaterThanMatcher(new BigDecimal(i))));
        return this;
    }

    /**
     * Add a condition that the maximum risk affecting the element must be greater
     * than given value
     */
    public Rule ifMaxRiskGreaterThan(BigDecimal i) {
        conditions.add(new RuleCondition(new MaxRiskProvider(), new GreaterThanMatcher(i)));
        return this;
    }

    /** Add a condition that no risk values must affect the element. */
    public Rule ifNoRiskValuesPresent() {
        conditions.add(new RuleCondition(new MaxRiskProvider(), new IsNullMatcher()));
        return this;
    }

    /** Compares the output of this rule to given value. */
    public boolean outputEquals(Boolean value) {
        // Equality in Java is tricky!
        if (output == null) {
            return value == null;
        }
        return output.equals(value);
    }
}
