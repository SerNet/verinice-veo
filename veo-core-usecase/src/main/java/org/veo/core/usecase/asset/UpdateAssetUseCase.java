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
import javax.validation.constraints.NotNull;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import org.veo.core.entity.Key;
import org.veo.core.entity.asset.Asset;
import org.veo.core.entity.asset.AssetRepository;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.NotFoundException;

/**
 * Abstract superclass for all operations that change an asset. The
 * <code>update()</code> method must be overwritten to make all necessary
 * changes to the asset.
 *
 * Note: increasing the version number of the key here will lead to a new asset
 * being saved since the version is part of the entity ID together with the
 * UUID. In almost all cases increasing the version number should be left to the
 * repository.
 *
 *
 */
@Slf4j
public abstract class UpdateAssetUseCase
        extends UseCase<UpdateAssetUseCase.InputData, UpdateAssetUseCase.OutputData> {

    private final AssetRepository assetRepository;

    public UpdateAssetUseCase(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public OutputData execute(InputData input) {
        log.info("Updating asset with id {}", input.getId());
        final Key<UUID> id = input.getId();
        return this.assetRepository.findById(id)
                                   .map(a -> update(a, input))
                                   .map(this::save)
                                   .map(this::output)
                                   .orElseThrow(() -> new NotFoundException(
                                           "Asset %s was not found.", id.uuidValue()));
    }

    protected abstract Asset update(Asset asset, InputData input);

    private Asset save(Asset asset) {
        return this.assetRepository.save(asset);
    }

    private OutputData output(Asset asset) {
        return new OutputData(asset);
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        @Valid
        @NotNull
        private final Key<UUID> id;
        @Valid
        private final Asset changedAsset;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        private final Asset asset;
    }
}
