/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
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
package org.veo.adapter.presenter.api.dto;

import java.util.Set;

import javax.validation.constraints.Pattern;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.dto.full.FullEntityLayerSupertypeGroupDto;
import org.veo.core.entity.EntityLayerSupertype;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.ToString;

/**
 * A generic data transfer object for groups. This for the OpenApi documentation
 * only. It can be used to describe responses on endpoints that may return
 * several types of {@link EntityLayerSupertypeGroupDto}.
 */
abstract public class FullGroupDto<T extends EntityLayerSupertype>
        extends AbstractEntityLayerSupertypeDto implements FullEntityLayerSupertypeGroupDto<T> {

    @Pattern(regexp = Patterns.UUID,
             flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "ID must be a valid UUID string following RFC 4122.")
    @Schema(description = "ID must be a valid UUID string following RFC 4122.",
            example = "adf037f1-0089-48ad-9177-92269918758b")
    @ToString.Include
    abstract public String getId();

    abstract public void setId(String id);

    @Override
    @Schema(required = true, description = "The group's members")
    abstract public Set<ModelObjectReference<T>> getMembers();
}
