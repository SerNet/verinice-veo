/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
package org.veo.core.usecase.domain;

import java.util.Set;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Domain;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.statistics.CatalogItemsTypeCount;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.SubTypeCount;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EntityId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GetCatalogItemsTypeCountUseCase
    implements TransactionalUseCase<EntityId, GetCatalogItemsTypeCountUseCase.OutputData> {

  private final DomainRepository domainRepository;
  private final CatalogItemRepository itemRepository;

  @Override
  public OutputData execute(EntityId input, UserAccessRights userAccessRights) {
    Domain domain = domainRepository.getById(input.id(), userAccessRights.getClientId());
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }

    CatalogItemsTypeCount catalogItemTypeCounts = new CatalogItemsTypeCount();
    Set<SubTypeCount> counts = itemRepository.getCountsBySubType(domain);
    counts.forEach(c -> catalogItemTypeCounts.setCount(c.elementType(), c.subType(), c.count()));

    return new OutputData(catalogItemTypeCounts);
  }

  @Valid
  public record OutputData(@Valid CatalogItemsTypeCount result) implements UseCase.OutputData {}
}
