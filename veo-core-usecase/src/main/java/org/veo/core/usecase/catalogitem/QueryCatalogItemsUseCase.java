/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.usecase.catalogitem;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Key;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryCatalogItemsUseCase
    implements TransactionalUseCase<
        QueryCatalogItemsUseCase.InputData, QueryCatalogItemsUseCase.OutputData> {

  private final DomainRepository domainRepository;
  private final CatalogItemRepository catalogItemRepository;

  @Override
  public OutputData execute(InputData input) {
    var domain = domainRepository.getActiveById(input.domainId, input.clientId);
    var query = catalogItemRepository.query(domain);
    Optional.ofNullable(input.elementTypes).ifPresent(query::whereElementTypeMatches);
    Optional.ofNullable(input.subTypes).ifPresent(query::whereSubTypeMatches);
    return new OutputData(query.execute(input.pagingConfiguration));
  }

  public record InputData(
      @NotNull Key<UUID> clientId,
      @NotNull Key<UUID> domainId,
      @NotNull PagingConfiguration pagingConfiguration,
      QueryCondition<String> elementTypes,
      QueryCondition<String> subTypes)
      implements UseCase.InputData {}

  public record OutputData(@Valid PagedResult<CatalogItem> page) implements UseCase.OutputData {}
}
