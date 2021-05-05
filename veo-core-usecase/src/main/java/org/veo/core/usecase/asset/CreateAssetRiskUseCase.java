/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.risk.CreateRiskUseCase;

public class CreateAssetRiskUseCase extends CreateRiskUseCase<Asset, AssetRisk> {

    public CreateAssetRiskUseCase(RepositoryProvider repositoryProvider) {
        super(Asset.class, repositoryProvider);
    }
}
