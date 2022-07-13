/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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
package org.veo.persistence.access;

import java.util.List;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.entity.jpa.AssetData;

public class AssetQueryImpl extends CompositeElementQueryImpl<Asset, AssetData> {

  private final AssetDataRepository assetRepository;

  public AssetQueryImpl(AssetDataRepository repo, Client client) {
    super(repo, client);
    this.assetRepository = repo;
  }

  @Override
  protected List<AssetData> fullyLoadItems(List<String> ids) {
    var items = super.fullyLoadItems(ids);

    if (fetchRisks) {
      assetRepository.findAllWithRisksByDbIdIn(ids);
    }
    return items;
  }
}
