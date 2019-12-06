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

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Unit;
import org.veo.core.entity.asset.Asset;
import org.veo.core.entity.asset.AssetRepository;
import org.veo.core.usecase.UseCase;

public class CreateAssetUseCase
        extends UseCase<CreateAssetUseCase.InputData, CreateAssetUseCase.OutputData> {

    private final AssetRepository assetRepository;

    public CreateAssetUseCase(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public OutputData execute(InputData input) {
        Asset asset = createAsset(input);
        return new OutputData(assetRepository.save(asset));
    }

    private Asset createAsset(InputData input) {
        return Asset.newAsset(input.getUnit(), input.getName());
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        @Valid
        private final Unit unit;
        private final String name;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        private final Asset asset;
    }
}
