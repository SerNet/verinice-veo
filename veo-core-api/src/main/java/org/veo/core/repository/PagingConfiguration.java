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

import lombok.Data;

/** Used to limit the results from a repository to a number of items */
@Data
public class PagingConfiguration<TSortCriterion> {

  public static final PagingConfiguration<String> UNPAGED =
      new PagingConfiguration<>(Integer.MAX_VALUE, 0, "name", SortOrder.ASCENDING);

  private final int pageSize;
  private final int pageNumber;
  private final TSortCriterion sortColumn;
  private final SortOrder sortOrder;

  public enum SortOrder {
    ASCENDING,
    DESCENDING
  }
}
