/*******************************************************************************
 * Copyright (c) 2019 Daniel Murygin.
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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Value;
import lombok.With;

import org.modelmapper.ModelMapper;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Transfer object for controls.
 *
 * Contains all information of the control.
 */
@Value
public class ControlDto {

    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
             flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "ID must either be null (for new processes) or a valid UUID string following RFC 4122. ")

    @NotNull
    @Schema(description = "The UUID of the control",
            example = "f35b982c-8ad4-4515-96ee-df5fdd4247b9",
            required = true)
    private String id;

    @NotNull(message = "A name must be present.")
    @NotBlank
    @Size(min = 1, max = 255, message = "The name must be between 1 and 255 characters long.")
    @With
    @Schema(description = "The name of the control", example = "Lock doors", required = true)
    private String name;

    public static ControlDto from(Object control) {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(control, ControlDto.class);
    }
}
