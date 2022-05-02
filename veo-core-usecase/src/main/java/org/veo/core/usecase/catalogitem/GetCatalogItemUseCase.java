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

import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.EntitySpecifications;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.Value;

public class GetCatalogItemUseCase
    implements TransactionalUseCase<
        GetCatalogItemUseCase.InputData, GetCatalogItemUseCase.OutputData> {

  @Override
  public OutputData execute(InputData input) {

    CatalogItem catalogItem =
        input.getAuthenticatedClient().getDomains().stream()
            .filter(EntitySpecifications.isActive())
            .filter(
                input
                    .domainId
                    .map(EntitySpecifications::hasId)
                    .orElse(EntitySpecifications.matchAll()))
            .flatMap(d -> d.getCatalogs().stream())
            .filter(EntitySpecifications.hasId(input.catalogId))
            .flatMap(c -> c.getCatalogItems().stream())
            .filter(item -> item.getId().equals(input.itemId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(input.itemId.uuidValue()));

    return new OutputData(catalogItem);
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Key<UUID> itemId;
    Key<UUID> catalogId;
    Optional<Key<UUID>> domainId;
    Client authenticatedClient;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid CatalogItem catalogItem;
  }
}
