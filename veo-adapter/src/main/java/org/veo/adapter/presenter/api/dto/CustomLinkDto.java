/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import javax.validation.constraints.NotNull;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefCustomLinkTarget;
import org.veo.core.entity.EntityLayerSupertype;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Transfer object for CustomLinks.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class CustomLinkDto extends CustomPropertiesDto {

    @NotNull(message = "A target must be present.")
    @ToString.Include
    @Schema(required = true, implementation = IdRefCustomLinkTarget.class)
    private IdRef<EntityLayerSupertype> target;

}
