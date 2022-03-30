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

import javax.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

/**
 * Matches if the value matches an injectable comparison value.
 */
@RequiredArgsConstructor
public class EqualsMatcher implements InputMatcher {
    @NotNull
    private final Object comparisonValue;

    @Override
    public boolean matches(Object value) {
        return comparisonValue.equals(value);
    }
}
