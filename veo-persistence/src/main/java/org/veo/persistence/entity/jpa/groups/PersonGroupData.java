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

import org.veo.core.entity.Person;
import org.veo.core.entity.groups.PersonGroup;
import org.veo.persistence.entity.jpa.PersonData;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "person_group")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class PersonGroupData extends PersonData
        implements PersonGroup, EntityLayerSupertypeGroupData<PersonData> {

    @ManyToMany(targetEntity = PersonData.class,
                cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "person_group_members",
               joinColumns = @JoinColumn(name = "group_id"),
               inverseJoinColumns = @JoinColumn(name = "member_id"))
    private Set<Person> members;

    @Override
    public Set<Person> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<Person> members) {
        this.members = members;
    }

}
