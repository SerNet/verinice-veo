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
package org.veo.persistence.entity.jpa.groups;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.validation.Valid;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.veo.core.entity.groups.AssetGroup;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.persistence.entity.jpa.AssetData;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetTransformer;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityTransformer;

@Entity(name = "asset_group")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class AssetGroupData extends AssetData implements EntityLayerSupertypeGroupData<AssetData> {

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "asset_group_members",
               joinColumns = @JoinColumn(name = "group_id"),
               inverseJoinColumns = @JoinColumn(name = "member_id"))
    private Set<AssetData> members;

    @Override
    public Set<AssetData> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<AssetData> members) {
        this.members = members;
    }

    /**
     * transform the given entity 'AssetGroup' to the corresponding 'AssetGroupData'
     * with the DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public static AssetGroupData from(@Valid AssetGroup assetGroup) {
        return from(assetGroup, DataEntityToTargetContext.getCompleteTransformationContext());
    }

    /**
     * Transform the given data object 'AssetGroupData' to the corresponding
     * 'AssetGroup' entity with the
     * DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public AssetGroup toAssetGroup() {
        return toAssetGroup(DataTargetToEntityContext.getCompleteTransformationContext());
    }

    public static AssetGroupData from(@Valid AssetGroup assetGroup,
            TransformEntityToTargetContext tcontext) {
        if (tcontext instanceof DataEntityToTargetContext) {
            return DataEntityToTargetTransformer.transformAssetGroup2Data((DataEntityToTargetContext) tcontext,
                                                                          assetGroup);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

    public AssetGroup toAssetGroup(TransformTargetToEntityContext tcontext) {
        if (tcontext instanceof DataTargetToEntityContext) {
            return DataTargetToEntityTransformer.transformData2AssetGroup((DataTargetToEntityContext) tcontext,
                                                                          this);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

}
