/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
package org.veo.adapter.presenter.api.response.groups;

import java.util.Collections;
import java.util.Set;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.response.AssetDto;
import org.veo.core.entity.Asset;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Transfer object for complete AssetGroup.
 *
 * Contains all information of the AssetGroup.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class AssetGroupDto extends AssetDto implements EntityLayerSupertypeGroupDto<Asset> {

    private Set<ModelObjectReference<Asset>> members = Collections.emptySet();

    @Override
    public Set<ModelObjectReference<Asset>> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<ModelObjectReference<Asset>> members) {
        this.members = members;
    }

}
