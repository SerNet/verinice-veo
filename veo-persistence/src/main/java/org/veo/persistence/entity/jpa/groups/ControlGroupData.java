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

import org.veo.core.entity.groups.ControlGroup;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetTransformer;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityTransformer;

@Entity(name = "control_group")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class ControlGroupData extends ControlData
        implements EntityLayerSupertypeGroupData<ControlData> {

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "control_group_members",
               joinColumns = @JoinColumn(name = "group_id"),
               inverseJoinColumns = @JoinColumn(name = "member_id"))
    private Set<ControlData> members;

    @Override
    public Set<ControlData> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<ControlData> members) {
        this.members = members;
    }

    /**
     * transform the given entity 'ControlGroup' to the corresponding
     * 'ControlGroupData' with the
     * DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public static ControlGroupData from(@Valid ControlGroup controlGroup) {
        return from(controlGroup, DataEntityToTargetContext.getCompleteTransformationContext());
    }

    /**
     * Transform the given data object 'ControlGroupData' to the corresponding
     * 'ControlGroup' entity with the
     * DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public ControlGroup toControlGroup() {
        return toControlGroup(DataTargetToEntityContext.getCompleteTransformationContext());
    }

    public static ControlGroupData from(@Valid ControlGroup controlGroup,
            TransformEntityToTargetContext tcontext) {
        if (tcontext instanceof DataEntityToTargetContext) {
            return DataEntityToTargetTransformer.transformControlGroup2Data((DataEntityToTargetContext) tcontext,
                                                                            controlGroup);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

    public ControlGroup toControlGroup(TransformTargetToEntityContext tcontext) {
        if (tcontext instanceof DataTargetToEntityContext) {
            return DataTargetToEntityTransformer.transformData2ControlGroup((DataTargetToEntityContext) tcontext,
                                                                            this);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

}
