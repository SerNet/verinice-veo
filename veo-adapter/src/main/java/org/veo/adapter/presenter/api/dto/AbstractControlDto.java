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

import java.util.List;
import java.util.Map;

import org.veo.core.entity.Control;
import org.veo.core.entity.Identifiable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Base transfer object for controls. Contains common data for all control DTOs.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(title = "control", description = "Schema for control")
public abstract class AbstractControlDto extends CompositeEntityDto<Control> {

    @Override
    @Schema(description = "The name for the control.", example = "Install sensors")
    public String getName() {
        return super.getName();
    }

    @Override
    @Schema(description = "The abbreviation for the control.", example = "Sensors")
    public String getAbbreviation() {
        return super.getAbbreviation();
    }

    @Override
    @Schema(description = "The description for the control.",
            example = "Install sensors. Sensors must be installed correctly.")
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    @Schema(description = "The links for the control.")
    public Map<String, List<CustomLinkDto>> getLinks() {
        return super.getLinks();
    }

    @Schema(description = "The customAspects for the control.")
    @Override
    public Map<String, CustomAspectDto> getCustomAspects() {
        return super.getCustomAspects();
    }

    @Override
    public Class<? extends Identifiable> getModelInterface() {
        return Control.class;
    }
}