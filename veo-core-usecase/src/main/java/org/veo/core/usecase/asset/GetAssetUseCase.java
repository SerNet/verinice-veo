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

import javax.validation.Valid;

import org.veo.core.entity.Asset;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.AssetRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.IdAndClient;

import lombok.Value;

public class GetAssetUseCase
        implements TransactionalUseCase<IdAndClient, GetAssetUseCase.OutputData> {

    private final AssetRepository repository;

    public GetAssetUseCase(AssetRepository repository) {
        this.repository = repository;
    }

    public OutputData execute(IdAndClient input) {
        Asset asset = repository.findById(input.getId())
                                .orElseThrow(() -> new NotFoundException(input.getId()
                                                                              .uuidValue()));
        asset.checkSameClient(input.getAuthenticatedClient());
        return new OutputData(asset);
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        Asset asset;
    }
}
