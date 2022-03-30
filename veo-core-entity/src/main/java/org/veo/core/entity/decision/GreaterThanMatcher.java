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

import javax.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

/**
 * Matches if the value is a greater number than an injectable comparison value.
 * Only supports int, long & decimal.
 */
@RequiredArgsConstructor
public class GreaterThanMatcher implements InputMatcher {
    @NotNull
    private final BigDecimal comparisonValue;

    @Override
    public boolean matches(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof BigDecimal) {
            return comparisonValue.compareTo((BigDecimal) value) < 0;
        }
        if (value instanceof Integer) {
            return comparisonValue.compareTo(new BigDecimal((Integer) value)) < 0;
        }
        if (value instanceof Long) {
            return comparisonValue.compareTo(new BigDecimal((Long) value)) < 0;
        }
        throw new IllegalArgumentException("Cannot compare BigDecimal to " + value.getClass()
                                                                                  .getSimpleName());
    }
}
