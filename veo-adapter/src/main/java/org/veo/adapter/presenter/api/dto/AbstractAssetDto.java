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
package org.veo.adapter.presenter.api.dto;

import java.util.List;
import java.util.Map;

import org.veo.core.entity.Asset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Base transfer object for Assets. Contains common data for all Asset DTOs.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(title = "Asset", description = "Schema for Asset")
public abstract class AbstractAssetDto extends CompositeEntityDto<Asset> {

    @Override
    @Schema(description = "The name for the Asset.", example = "Mail Server")
    public String getName() {
        return super.getName();
    }

    @Override
    @Schema(description = "The abbreviation for the Asset.", example = "MS")
    public String getAbbreviation() {
        return super.getAbbreviation();
    }

    @Override
    @Schema(description = "The description for the Asset.", example = "A server handling e-mail.")
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    @Schema(description = "The links for the Asset.")
    public Map<String, List<CustomLinkDto>> getLinks() {
        return super.getLinks();
    }

    @Schema(description = "The customAspects for the Asset.")
    @Override
    public Map<String, CustomPropertiesDto> getCustomAspects() {
        return super.getCustomAspects();
    }
}