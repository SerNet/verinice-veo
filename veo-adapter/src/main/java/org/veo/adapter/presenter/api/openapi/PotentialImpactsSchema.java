/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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
package org.veo.adapter.presenter.api.openapi;

import javax.validation.constraints.Min;

import org.veo.core.entity.Constraints;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PotentialImpacts",
        description = "Potential impacts for a set of risk categories",
        example = "{\"C\":2,\n\"I\":3}")
public interface PotentialImpactsSchema {
    @Schema(description = "A category ID as defined in the risk definition",
            minLength = 1,
            maxLength = Constraints.DEFAULT_CONSTANT_MAX_LENGTH)
    String getKey();

    @Schema(description = "The potential impact ID.")
    @Min(0)
    Integer getValue();
}