/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.adapter.presenter.api.io.mapper;

import java.util.function.Function;

import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PagingMapper {

  public static <TDto, TItem, TSortCriterion> PageDto<TDto> toPage(
      PagedResult<TItem, TSortCriterion> input, Function<TItem, TDto> mapper) {
    return new PageDto<>(
        input.resultPage().stream().map(mapper).toList(),
        input.totalResults(),
        input.totalPages(),
        input.pagingConfiguration().pageNumber());
  }

  public static <TSortCriterion> PagingConfiguration<TSortCriterion> toConfig(
      int pageSize, int pageNumber, TSortCriterion sortColumn, String sortOrder) {
    return new PagingConfiguration<>(pageSize, pageNumber, sortColumn, getSortOrder(sortOrder));
  }

  private static PagingConfiguration.SortOrder getSortOrder(String sortOrder) {
    if (sortOrder.equalsIgnoreCase("DESC")) {
      return PagingConfiguration.SortOrder.DESCENDING;
    }
    return PagingConfiguration.SortOrder.ASCENDING;
  }
}
