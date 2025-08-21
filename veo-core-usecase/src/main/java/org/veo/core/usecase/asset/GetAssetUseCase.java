/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.core.usecase.asset;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Asset;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.AssetRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.base.GetElementUseCase;

public class GetAssetUseCase extends GetElementUseCase<Asset> {

  private final AssetRepository assetRepository;

  public GetAssetUseCase(AssetRepository repository, DomainRepository domainRepository) {
    super(domainRepository, repository, Asset.class);
    this.assetRepository = repository;
  }

  @Override
  public GetElementUseCase.OutputData<Asset> execute(
      InputData input, UserAccessRights userAccessRights) {
    var asset =
        assetRepository
            .findById(input.elementId(), input.embedRisks(), userAccessRights)
            .orElseThrow(() -> new NotFoundException(input.elementId(), Asset.class));
    return new GetElementUseCase.OutputData<>(
        asset, getDomain(asset, input, userAccessRights).orElse(null));
  }
}
