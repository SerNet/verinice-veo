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
package org.veo.adapter.presenter.api.dto.create;

import org.veo.adapter.presenter.api.dto.AbstractCustomLinkDto;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.core.entity.CustomLink;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateCustomLinkDto extends AbstractCustomLinkDto {
    public CustomLink toEntity(DtoToEntityContext tcontext) {
        return DtoToEntityTransformer.transformDto2CustomLink(tcontext, this);
    }
}
