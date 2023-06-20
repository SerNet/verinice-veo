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

import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.AssetRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.base.GetElementUseCase;

import lombok.EqualsAndHashCode;
import lombok.Value;

public class GetAssetUseCase extends GetElementUseCase<Asset> {

  private final AssetRepository assetRepository;

  public GetAssetUseCase(AssetRepository repository, DomainRepository domainRepository) {
    super(domainRepository, repository, Asset.class);
    this.assetRepository = repository;
  }

  public GetElementUseCase.OutputData<Asset> execute(InputData input) {
    var asset =
        assetRepository
            .findById(input.getId(), input.embedRisks)
            .orElseThrow(() -> new NotFoundException(input.getId(), Asset.class));
    asset.checkSameClient(input.getAuthenticatedClient());
    return new GetElementUseCase.OutputData<>(asset, getDomain(asset, input).orElse(null));
  }

  @Value
  @EqualsAndHashCode(callSuper = true)
  @Valid
  public static class InputData extends GetElementUseCase.InputData {
    boolean embedRisks;

    public InputData(
        Key<UUID> id, Client authenticatedClient, Key<UUID> domainId, boolean embedRisks) {
      super(id, authenticatedClient, domainId);
      this.embedRisks = embedRisks;
    }

    public InputData(Key<UUID> id, Client authenticatedClient, boolean embedRisks) {
      super(id, authenticatedClient);
      this.embedRisks = embedRisks;
    }
  }
}
