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

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Matches if the value is a greater number than an injectable comparison value. Only supports int,
 * long & decimal.
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class GreaterThanMatcher implements InputMatcher {
  @NotNull private BigDecimal comparisonValue;

  @Override
  public boolean matches(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof BigDecimal bd) {
      return comparisonValue.compareTo(bd) < 0;
    }
    if (value instanceof Integer i) {
      return comparisonValue.compareTo(new BigDecimal(i)) < 0;
    }
    if (value instanceof Long l) {
      return comparisonValue.compareTo(new BigDecimal(l)) < 0;
    }
    throw new IllegalArgumentException(
        "Cannot compare BigDecimal to " + value.getClass().getSimpleName());
  }
}
