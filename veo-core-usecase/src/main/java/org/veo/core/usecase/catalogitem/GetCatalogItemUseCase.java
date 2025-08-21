/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetCatalogItemUseCase
    implements TransactionalUseCase<
        GetCatalogItemUseCase.InputData, GetCatalogItemUseCase.OutputData> {
  private final DomainRepository domainRepository;
  private final CatalogItemRepository catalogItemRepository;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var domain = domainRepository.getActiveById(input.domainId, input.authenticatedClient.getId());
    return new OutputData(catalogItemRepository.getByIdInDomain(input.itemId, domain));
  }

  @Valid
  public record InputData(
      @NonNull UUID itemId, @NonNull UUID domainId, @NonNull Client authenticatedClient)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid CatalogItem catalogItem) implements UseCase.OutputData {}
}
