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
package org.veo.adapter.presenter.api.request;

import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceProcessDomains;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceProcessOwner;
import org.veo.adapter.presenter.api.response.CustomLinkDto;
import org.veo.adapter.presenter.api.response.CustomPropertiesDto;
import org.veo.adapter.presenter.api.response.ProcessDto;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;

@Data
@EqualsAndHashCode(callSuper = true)
public final class CreateProcessDto extends ProcessDto {

    public CreateProcessDto() {
        super();
        setId(Key.NIL_UUID.uuidValue());
    }

    @Override
    @Schema(description = "The name for the Process.", example = "Two-factor authentication")
    public String getName() {
        return super.getName();
    }

    @Override
    @Schema(description = "The abbreviation for the Process.", example = "2FA")
    public String getAbbreviation() {
        return super.getAbbreviation();
    }

    @Override
    @Schema(description = "The description for the Process.",
            example = "Implement 2FA where possible.")
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    @ArraySchema(schema = @Schema(implementation = ModelObjectReferenceProcessDomains.class))
    public Set<ModelObjectReference<Domain>> getDomains() {
        return super.getDomains();
    }

    @Override
    @Schema(description = "The links for the Process.")
    public Set<CustomLinkDto> getLinks() {
        return super.getLinks();
    }

    @Schema(description = "The customAspects for the Process.")
    @Override
    public Set<CustomPropertiesDto> getCustomAspects() {
        return super.getCustomAspects();
    }

    @Override
    @Schema(implementation = ModelObjectReferenceProcessOwner.class)
    public ModelObjectReference<Unit> getOwner() {
        return super.getOwner();
    }
}
