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

import org.veo.core.entity.Person;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Base transfer object for Persons. Contains common data for all Person DTOs.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(title = "Person", description = "Schema for Person")
public abstract class AbstractPersonDto extends CompositeEntityDto<Person> {

    @Override
    @Schema(description = "The name for the Person.", example = "Mia Musterfrau")
    public String getName() {
        return super.getName();
    }

    @Override
    @Schema(description = "The abbreviation for the Person.", example = "Mrs. M.M.")
    public String getAbbreviation() {
        return super.getAbbreviation();
    }

    @Override
    @Schema(description = "The description for the Person.",
            example = "Mia Musterfrau is a fictional character and is not related to any real person with that name.")
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    @Schema(description = "The links for the Person.")
    public Map<String, List<CustomLinkDto>> getLinks() {
        return super.getLinks();
    }

    @Schema(description = "The customAspects for the Person.")
    @Override
    public Map<String, CustomPropertiesDto> getCustomAspects() {
        return super.getCustomAspects();
    }
}