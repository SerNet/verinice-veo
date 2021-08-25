/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.presenter.api.dto;

import java.util.Collections;
import java.util.Set;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefPart;
import org.veo.core.entity.EntityLayerSupertype;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author urszeidler
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class CompositeEntityDto<T extends EntityLayerSupertype>
        extends AbstractEntityLayerSupertypeDto {

    private Set<IdRef<T>> parts = Collections.emptySet();

    @ArraySchema(schema = @Schema(implementation = IdRefPart.class))
    public Set<IdRef<T>> getParts() {
        return parts;
    }

}
