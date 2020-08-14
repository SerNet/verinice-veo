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
import org.veo.adapter.presenter.api.response.PersonDto;
import org.veo.core.entity.Person;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Transfer object for complete PersonGroup.
 *
 * Contains all information of the PersonGroup.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class PersonGroupDto extends PersonDto implements EntityLayerSupertypeGroupDto<Person> {

    private Set<ModelObjectReference<Person>> members = Collections.emptySet();

    @Override
    public Set<ModelObjectReference<Person>> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<ModelObjectReference<Person>> members) {
        this.members = members;
    }

}
