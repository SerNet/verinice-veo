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

import org.veo.core.entity.Key;
import org.veo.core.entity.asset.Asset;
import org.veo.core.entity.asset.IAssetRepository;
import org.veo.core.usecase.UseCase;

public class CreateAssetUseCase
        extends UseCase<CreateAssetUseCase.InputData, CreateAssetUseCase.OutputData> {

    private IAssetRepository assetRepository;

    public CreateAssetUseCase(IAssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    public OutputData execute(InputData input) {
        Asset asset = createAsset(input);
        return new OutputData(assetRepository.store(asset));
    }

    private Asset createAsset(InputData input) {
        return new Asset(Key.undefined(), input.getName());
    }

    // TODO: use lombok @Value instead?
    public static class InputData implements UseCase.InputData {

        private final Key key;
        private final String name;

        public Key getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public InputData(Key key, String name) {
            this.key = key;
            this.name = name;
        }
    }

    // TODO: use lombok @Value instead?
    public static class OutputData implements UseCase.OutputData {

        private final Asset asset;

        public Asset getAsset() {
            return asset;
        }

        public OutputData(Asset asset) {
            this.asset = asset;
        }
    }
}
