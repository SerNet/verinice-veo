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
package org.veo.core.usecase.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Catalog;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.specification.EntitySpecifications;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.Value;

public class GetCatalogsUseCase
    implements TransactionalUseCase<GetCatalogsUseCase.InputData, GetCatalogsUseCase.OutputData> {

  @Override
  public OutputData execute(InputData input) {
    List<Catalog> list =
        input.authenticatedClient.getDomains().stream()
            .filter(EntitySpecifications.isActive())
            .filter(
                input
                    .domainId
                    .map(EntitySpecifications::hasId)
                    .orElse(EntitySpecifications.matchAll()))
            .flatMap(d -> d.getCatalogs().stream())
            .toList();
    // TODO: VEO-500 Implement Catalog Search
    return new OutputData(list);
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Optional<Key<UUID>> domainId;
    Client authenticatedClient;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid List<Catalog> catalogs;
  }
}
