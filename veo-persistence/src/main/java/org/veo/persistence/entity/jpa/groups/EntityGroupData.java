/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.ModelObject;
import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "entitygroup")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class EntityGroupData<T extends EntityLayerSupertype>
        extends EntityLayerSupertypeData implements ModelGroup<T> {

    @Override
    public Class<? extends ModelObject> getModelInterface() {
        return ModelGroup.class;
    }

    @ManyToMany(targetEntity = EntityLayerSupertypeData.class,
                cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "group_members",
               joinColumns = @JoinColumn(name = "group_id"),
               inverseJoinColumns = @JoinColumn(name = "member_id"))
    private final Set<T> members = new HashSet<>();

    @Override
    public Set<T> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<T> members) {
        this.members.clear();
        this.members.addAll(members);
    }

}
