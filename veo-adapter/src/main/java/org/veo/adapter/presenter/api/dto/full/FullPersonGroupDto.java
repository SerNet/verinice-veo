/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
package org.veo.adapter.presenter.api.dto.full;

import java.util.Collections;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.groups.PersonGroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
public class FullPersonGroupDto extends FullPersonDto
        implements FullEntityLayerSupertypeGroupDto<Person> {

    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
             flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "ID must be a valid UUID string following RFC 4122.")
    @Schema(description = "ID must be a valid UUID string following RFC 4122.",
            example = "adf037f1-0089-48ad-9177-92269918758b")
    @ToString.Include
    private String id;

    private Set<ModelObjectReference<Person>> members = Collections.emptySet();

    @Override
    public Set<ModelObjectReference<Person>> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<ModelObjectReference<Person>> members) {
        this.members = members;
    }

    public static FullPersonGroupDto from(@Valid PersonGroup processGroup,
            EntityToDtoContext tcontext) {
        return EntityToDtoTransformer.transformPersonGroup2Dto(tcontext, processGroup);
    }

    public PersonGroup toEntity(DtoToEntityContext tcontext) {
        return DtoToEntityTransformer.transformDto2PersonGroup(tcontext, this, Key.uuidFrom(id));
    }
}
