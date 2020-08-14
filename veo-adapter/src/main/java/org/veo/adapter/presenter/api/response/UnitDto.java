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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceUnitDomains;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceUnitParent;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Unit;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Transfer object for complete Units.
 *
 * Contains all information of the Unit.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UnitDto extends BaseModelObjectDto implements NameAbleDto {

    @NotNull(message = "A name must be present.")
    @Schema(description = "The name for the Unit.", example = "My unit", required = true)
    private String name;

    @Schema(description = "The abbreviation for the Unit.", example = "U-96")
    private String abbreviation;

    @Schema(description = "The description for the Unit.",
            example = "This is currently the main and only unit for our organization.",
            required = false)
    private String description;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private ModelObjectReference<Client> client;

    @Schema(description = "The units for the Unit.")
    private Set<UnitDto> units = Collections.emptySet();

    @Schema(implementation = ModelObjectReferenceUnitParent.class)
    private ModelObjectReference<Unit> parent;

    @ArraySchema(schema = @Schema(implementation = ModelObjectReferenceUnitDomains.class))

    private Set<ModelObjectReference<Domain>> domains = Collections.emptySet();

    public Collection<ModelObjectReference<? extends ModelObject>> getReferences() {
        List<ModelObjectReference<? extends ModelObject>> list = new ArrayList<>();
        list.add(getClient());
        Optional.ofNullable(getParent())
                .ifPresent(list::add);
        list.addAll(getDomains());
        return list;
    }

    public static UnitDto from(@Valid Unit unit, EntityToDtoContext tcontext) {
        return EntityToDtoTransformer.transformUnit2Dto(tcontext, unit);
    }

    public Unit toUnit(DtoToEntityContext tcontext) {
        return DtoToEntityTransformer.transformDto2Unit(tcontext, this);
    }
}
