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
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;

public class PagingMapper {

    public static <TDto, TEntity extends EntityLayerSupertype> PageDto<TDto> toPage(
            PagedResult<TEntity> input, Function<TEntity, TDto> mapper) {
        return new PageDto<TDto>(input.getResultPage()
                                      .stream()
                                      .map(mapper)
                                      .collect(Collectors.toList()),
                input.getTotalResults(), input.getTotalPages(), input.getPagingConfiguration()
                                                                     .getPageNumber());
    }

    public static PagingConfiguration toConfig(int pageSize, int pageNumber, String sortColumn,
            String sortOrder) {
        return new PagingConfiguration(pageSize, pageNumber, sortColumn, getSortOrder(sortOrder));
    }

    private static PagingConfiguration.SortOrder getSortOrder(String sortOrder) {
        if (sortOrder.toUpperCase()
                     .equals("DESC")) {
            return PagingConfiguration.SortOrder.DESCENDING;
        }
        return PagingConfiguration.SortOrder.ASCENDING;
    }
}
