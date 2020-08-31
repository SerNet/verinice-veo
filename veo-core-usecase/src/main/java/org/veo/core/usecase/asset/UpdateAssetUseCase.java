/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
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

import java.time.Instant;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.repository.AssetRepository;

/**
 * @author urszeidler
 */
public class UpdateAssetUseCase<R> extends ModifyEntityUseCase<Asset, R> {

    private final AssetRepository assetRepository;

    public UpdateAssetUseCase(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    public OutputData<Asset> execute(InputData<Asset> input) {
        Asset entity = input.getEntity();
        Client authenticatedClient = input.getAuthenticatedClient();
        checkSameClient(authenticatedClient, entity);
        return performModification(input);
    }

    @Override
    protected OutputData<Asset> performModification(InputData<Asset> input) {
        Asset asset = input.getEntity();
        asset.setValidFrom(Instant.now());
        return new OutputData<>(assetRepository.save(asset));
    }

}