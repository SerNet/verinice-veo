/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
package org.veo.core.usecase.asset;

import java.util.UUID;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.AssetRepository;

/**
 * Reinstantiate a persisted process object.
 */
public class GetAssetUseCase
        extends UseCase<GetAssetUseCase.InputData, GetAssetUseCase.OutputData> {

    private final AssetRepository repository;
    private final TransformContextProvider transformContextProvider;

    public GetAssetUseCase(AssetRepository repository,
            TransformContextProvider transformContextProvider) {
        this.repository = repository;
        this.transformContextProvider = transformContextProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public OutputData execute(InputData input) {
        TransformTargetToEntityContext dataTargetToEntityContext = transformContextProvider.createTargetToEntityContext()
                                                                                           .partialDomain()
                                                                                           .partialClient();
        Asset asset = repository.findById(input.getId(), dataTargetToEntityContext)
                                .orElseThrow(() -> new NotFoundException(input.getId()
                                                                              .uuidValue()));
        checkSameClient(input.authenticatedClient, asset);
        return new OutputData(asset);
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        private final Key<UUID> id;
        private final Client authenticatedClient;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        private final Asset asset;
    }
}
