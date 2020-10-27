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

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeGroupDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.ModelGroup;

public interface FullEntityLayerSupertypeGroupDto<T extends EntityLayerSupertype>
        extends EntityLayerSupertypeGroupDto<T>, IdentifiableDto {

    static FullEntityLayerSupertypeGroupDto<?> from(ModelGroup<?> group,
            ReferenceAssembler referenceAssembler) {

        return EntityToDtoTransformer.transformGroup2Dto(referenceAssembler, group);
    }
}
