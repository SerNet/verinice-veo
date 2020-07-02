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

import org.veo.core.entity.groups.DocumentGroup;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.persistence.entity.jpa.DocumentData;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetTransformer;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityTransformer;

@Entity(name = "document_group")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class DocumentGroupData extends DocumentData
        implements EntityLayerSupertypeGroupData<DocumentData> {

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "document_group_members",
               joinColumns = @JoinColumn(name = "group_id"),
               inverseJoinColumns = @JoinColumn(name = "member_id"))
    private Set<DocumentData> members;

    @Override
    public Set<DocumentData> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<DocumentData> members) {
        this.members = members;
    }

    /**
     * transform the given entity 'DocumentGroup' to the corresponding
     * 'DocumentGroupData' with the
     * DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public static DocumentGroupData from(@Valid DocumentGroup documentGroup) {
        return from(documentGroup, DataEntityToTargetContext.getCompleteTransformationContext());
    }

    /**
     * Transform the given data object 'DocumentGroupData' to the corresponding
     * 'DocumentGroup' entity with the
     * DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public DocumentGroup toDocumentGroup() {
        return toDocumentGroup(DataTargetToEntityContext.getCompleteTransformationContext());
    }

    public static DocumentGroupData from(@Valid DocumentGroup documentGroup,
            TransformEntityToTargetContext tcontext) {
        if (tcontext instanceof DataEntityToTargetContext) {
            return DataEntityToTargetTransformer.transformDocumentGroup2Data((DataEntityToTargetContext) tcontext,
                                                                             documentGroup);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

    public DocumentGroup toDocumentGroup(TransformTargetToEntityContext tcontext) {
        if (tcontext instanceof DataTargetToEntityContext) {
            return DataTargetToEntityTransformer.transformData2DocumentGroup((DataTargetToEntityContext) tcontext,
                                                                             this);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

}
