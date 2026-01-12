/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Used to limit the results from a repository to a number of items */
public record PagingConfiguration<TSortCriterion>(
    int pageSize, int pageNumber, TSortCriterion sortColumn, SortOrder sortOrder) {

  public static final PagingConfiguration<String> UNPAGED =
      new PagingConfiguration<>(Integer.MAX_VALUE, 0, "name", SortOrder.ASCENDING);

  public static <T> PagingConfiguration<T> unpaged(T sortCriterion) {
    return new PagingConfiguration<>(Integer.MAX_VALUE, 0, sortCriterion, SortOrder.ASCENDING);
  }

  @Getter
  @RequiredArgsConstructor
  public enum SortOrder {
    ASCENDING("asc"),
    DESCENDING("desc");
    private final String sqlKeyword;
  }
}
