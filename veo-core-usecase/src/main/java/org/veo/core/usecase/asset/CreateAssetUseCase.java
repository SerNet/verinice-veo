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
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.AssetRepository;
import org.veo.core.usecase.repository.UnitRepository;

import lombok.Value;

public class CreateAssetUseCase
        extends UseCase<CreateAssetUseCase.InputData, CreateAssetUseCase.OutputData> {

    private final UnitRepository unitRepository;
    private final AssetRepository assetRepository;

    public CreateAssetUseCase(UnitRepository unitRepository, AssetRepository assetRepository) {
        this.unitRepository = unitRepository;
        this.assetRepository = assetRepository;
    }

    @Override
    public OutputData execute(InputData input) {
        Asset asset = input.getNewAsset();
        asset.setId(Key.newUuid());
        Unit unit = unitRepository.findById(asset.getOwner()
                                                 .getId())
                                  .orElseThrow(() -> new NotFoundException("Unit %s not found.",
                                          asset.getOwner()
                                               .getId()
                                               .uuidValue()));
        unit.checkSameClient(input.authenticatedClient);
        asset.version(input.username, null);
        return new OutputData(assetRepository.save(asset));
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Asset newAsset;
        Client authenticatedClient;
        String username;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        Asset asset;
    }
}
