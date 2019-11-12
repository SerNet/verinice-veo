/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
 *
 * Contributors:
 *     Alexander Koderman <ak@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.core.entity.group;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.asset.Asset;
import org.veo.core.entity.specification.IEntitySpecification;

/**
 * A group of entity objects of the same type.
 * A group may contain other groups.
 * An entity can be present in more than one group.
 * A group helps to organize entities and makes it easier to work with multiple
 * entities at the same time.
 * A group of objects can be used in business use cases instead of working with single elements.
 * 
 * @author akoderman
 *
 */
public class EntityGroup<T extends EntityLayerSupertype> extends EntityLayerSupertype {

    @NotNull
    @NotBlank
    private String name;

    @NotNull
    @Size(min=0, max=1000000)
    private Set<T> groupMembers;

    private EntityGroup(Key id, Unit unit, String name) {
        super(id, unit, EntityLayerSupertype.Lifecycle.CREATING, Instant.now(), null, 0L);
        this.name=name;
        this.groupMembers = new HashSet<>();
    }
    
    public static <T extends EntityLayerSupertype> EntityGroup<T> newGroup(Unit unit, String name) {
        return new EntityGroup<>(Key.newUuid(), unit, name);
    }
    
    public static <T extends EntityLayerSupertype> EntityGroup<T> existingGroup(Key<UUID> id, Unit unit, String name) {
        return new EntityGroup<>(id, unit, name);
    }
    
    public Set<T> getGroupMembers() {
        return groupMembers;
    }
    
    public void addGroupMember(T member) {
        checkSameClient(member);
        this.groupMembers.add(member);
    }

    public void setGroupMembers(Set<T> groupMembers) {
        checkSameClients(groupMembers);
        this.groupMembers = groupMembers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns a new set containing just the elements that fulfill
     * the given specification.
     * 
     */
    public Set<T> findMembersFulfilling(IEntitySpecification<T> spec) {
        return spec.selectSatisfyingElementsFrom(groupMembers);
    }
    
}
