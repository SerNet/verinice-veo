/*******************************************************************************
 * Copyright (c) 2021 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.usecase.catalogitem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCaseTools;

import lombok.Value;

public class GetCatalogItemsUseCase implements
        TransactionalUseCase<GetCatalogItemsUseCase.InputData, GetCatalogItemsUseCase.OutputData> {

    @Override
    public OutputData execute(InputData input) {
        Catalog catalog = input.authenticatedClient.getDomains()
                                                   .stream()
                                                   .filter(UseCaseTools.DOMAIN_IS_ACTIVE_PREDICATE)
                                                   .filter(UseCaseTools.getDomainIdPredicate(input.domainId))
                                                   .flatMap(d -> d.getCatalogs()
                                                                  .stream())
                                                   .filter(UseCaseTools.getCatalogIdPredicate(input.catalogId))
                                                   .findFirst()
                                                   .orElseThrow(() -> new NotFoundException(
                                                           input.catalogId.uuidValue()));
        List<CatalogItem> list = catalog.getCatalogItems()
                                        .stream()
                                        .filter(UseCaseTools.getNamespacePredicate(input.namespace))
                                        .collect(Collectors.toList());
        return new OutputData(list);
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Optional<String> namespace;
        Key<UUID> catalogId;
        Optional<Key<UUID>> domainId;
        Client authenticatedClient;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        List<CatalogItem> catalogItems;
    }
}