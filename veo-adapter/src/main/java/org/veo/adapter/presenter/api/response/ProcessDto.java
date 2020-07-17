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
package org.veo.adapter.presenter.api.response;

import java.util.Set;

import javax.validation.Valid;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceProcessDomains;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceProcessOwner;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Transfer object for complete Processs.
 *
 * Contains all information of the Process.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(title = "Process", description = "Schema for Process")
public class ProcessDto extends EntityLayerSupertypeDto {
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
    @Schema(implementation = ModelObjectReferenceProcessOwner.class)
    public ModelObjectReference<Unit> getOwner() {
        return super.getOwner();
    }

    public static ProcessDto from(@Valid Process process, EntityToDtoContext tcontext) {
        return EntityToDtoTransformer.transformProcess2Dto(tcontext, process);
    }

    public Process toProcess(DtoToEntityContext tcontext) {
        return DtoToEntityTransformer.transformDto2Process(tcontext, this);
    }
}
