/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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
package org.veo.core.repository;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A set of values to be matched against in a query. The condition is true if the tested value is
 * equal to any of the values in the set.
 */
public record QueryCondition<TValue>(Set<TValue> values) {
  public <TOut> QueryCondition<Object> map(Function<TValue, TOut> transformation) {
    return new QueryCondition<>(values.stream().map(transformation).collect(Collectors.toSet()));
  }
}
