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

import org.veo.core.entity.groups.PersonGroup;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetTransformer;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityTransformer;

@Entity(name = "person_group")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class PersonGroupData extends PersonData
        implements EntityLayerSupertypeGroupData<PersonData> {

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "person_group_members",
               joinColumns = @JoinColumn(name = "group_id"),
               inverseJoinColumns = @JoinColumn(name = "member_id"))
    private Set<PersonData> members;

    @Override
    public Set<PersonData> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<PersonData> members) {
        this.members = members;
    }

    /**
     * transform the given entity 'PersonGroup' to the corresponding
     * 'PersonGroupData' with the
     * DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public static PersonGroupData from(@Valid PersonGroup personGroup) {
        return from(personGroup, DataEntityToTargetContext.getCompleteTransformationContext());
    }

    /**
     * Transform the given data object 'PersonGroupData' to the corresponding
     * 'PersonGroup' entity with the
     * DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public PersonGroup toPersonGroup() {
        return toPersonGroup(DataTargetToEntityContext.getCompleteTransformationContext());
    }

    public static PersonGroupData from(@Valid PersonGroup personGroup,
            TransformEntityToTargetContext tcontext) {
        if (tcontext instanceof DataEntityToTargetContext) {
            return DataEntityToTargetTransformer.transformPersonGroup2Data((DataEntityToTargetContext) tcontext,
                                                                           personGroup);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

    public PersonGroup toPersonGroup(TransformTargetToEntityContext tcontext) {
        if (tcontext instanceof DataTargetToEntityContext) {
            return DataTargetToEntityTransformer.transformData2PersonGroup((DataTargetToEntityContext) tcontext,
                                                                           this);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

}
