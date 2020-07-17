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
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceControlDomains;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceControlOwner;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Transfer object for complete Controls.
 *
 * Contains all information of the Control.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(title = "Control", description = "Schema for Control")
public class ControlDto extends EntityLayerSupertypeDto {

    @Override
    @Schema(description = "The name for the Control.", example = "Install sensors")
    public String getName() {
        return super.getName();
    }

    @Override
    @Schema(description = "The abbreviation for the Control.", example = "Sensors")
    public String getAbbreviation() {
        return super.getAbbreviation();
    }

    @Override
    @Schema(description = "The description for the Control.",
            example = "Install sensors. Sensors must be installed correctly.")
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    @ArraySchema(schema = @Schema(implementation = ModelObjectReferenceControlDomains.class))
    public Set<ModelObjectReference<Domain>> getDomains() {
        return super.getDomains();
    }

    @Override
    @Schema(implementation = ModelObjectReferenceControlOwner.class)
    public ModelObjectReference<Unit> getOwner() {
        return super.getOwner();
    }

    public static ControlDto from(@Valid Control control, EntityToDtoContext tcontext) {
        return EntityToDtoTransformer.transformControl2Dto(tcontext, control);
    }

    public Control toControl(DtoToEntityContext tcontext) {
        return DtoToEntityTransformer.transformDto2Control(tcontext, this);
    }
}
