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
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.repository.AssetRepository;

/**
 * @author urszeidler
 */
public class UpdateAssetUseCase extends ModifyEntityUseCase<Asset> {

    private final AssetRepository assetRepository;

    public UpdateAssetUseCase(AssetRepository assetRepository,
            TransformContextProvider transformContextProvider) {
        super(transformContextProvider);
        this.assetRepository = assetRepository;
    }

    @Override
    protected Asset performModification(InputData<Asset> input) {
        TransformTargetToEntityContext dataTargetToEntityContext = transformContextProvider.createTargetToEntityContext()
                                                                                           .partialDomain()
                                                                                           .partialClient();
        Asset asset = input.getEntity();
        asset.setVersion(asset.getVersion() + 1);
        asset.setValidFrom(Instant.now());
        return assetRepository.save(asset, null, dataTargetToEntityContext);
    }

}