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

import org.veo.core.entity.groups.ProcessGroup;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.persistence.entity.jpa.ProcessData;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetTransformer;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityTransformer;

@Entity(name = "process_group")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class ProcessGroupData extends ProcessData
        implements EntityLayerSupertypeGroupData<ProcessData> {

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "process_group_members",
               joinColumns = @JoinColumn(name = "group_id"),
               inverseJoinColumns = @JoinColumn(name = "member_id"))
    private Set<ProcessData> members;

    @Override
    public Set<ProcessData> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<ProcessData> members) {
        this.members = members;
    }

    /**
     * transform the given entity 'ProcessGroup' to the corresponding
     * 'ProcessGroupData' with the
     * DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public static ProcessGroupData from(@Valid ProcessGroup processGroup) {
        return from(processGroup, DataEntityToTargetContext.getCompleteTransformationContext());
    }

    /**
     * Transform the given data object 'ProcessGroupData' to the corresponding
     * 'ProcessGroup' entity with the
     * DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public ProcessGroup toProcessGroup() {
        return toProcessGroup(DataTargetToEntityContext.getCompleteTransformationContext());
    }

    public static ProcessGroupData from(@Valid ProcessGroup processGroup,
            TransformEntityToTargetContext tcontext) {
        if (tcontext instanceof DataEntityToTargetContext) {
            return DataEntityToTargetTransformer.transformProcessGroup2Data((DataEntityToTargetContext) tcontext,
                                                                            processGroup);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

    public ProcessGroup toProcessGroup(TransformTargetToEntityContext tcontext) {
        if (tcontext instanceof DataTargetToEntityContext) {
            return DataTargetToEntityTransformer.transformData2ProcessGroup((DataTargetToEntityContext) tcontext,
                                                                            this);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

}
