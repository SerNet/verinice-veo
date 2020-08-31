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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceUnitDomains;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceUnitParent;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Unit;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Base transfer object for Units. Contains common data for all Unit DTOs.
 */
@Data
abstract public class AbstractUnitDto implements NameAbleDto, VersionedDto {

    @NotNull(message = "A name must be present.")
    @Schema(description = "The name for the Unit.", example = "My unit", required = true)
    private String name;

    @Schema(description = "The abbreviation for the Unit.", example = "U-96")
    private String abbreviation;

    @Schema(description = "The description for the Unit.",
            example = "This is currently the main and only unit for our organization.")
    private String description;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this version of the entity was saved.",
            example = "1990-12-31T23:59:60Z")
    @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2}[Tt]\\d{2}:\\d{2}:\\d{2}(\\.\\d{0,2})?([zZ]|[+-]\\d{2}:\\d{2}))")
    private String validFrom;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private ModelObjectReference<Client> client;

    @Schema(description = "The units for the Unit.")
    private Set<AbstractUnitDto> units = Collections.emptySet();

    @Schema(implementation = ModelObjectReferenceUnitParent.class)
    private ModelObjectReference<Unit> parent;

    @ArraySchema(schema = @Schema(implementation = ModelObjectReferenceUnitDomains.class))
    private Set<ModelObjectReference<Domain>> domains = Collections.emptySet();

    @com.fasterxml.jackson.annotation.JsonIgnore
    private long version;

    public Collection<ModelObjectReference<? extends ModelObject>> getReferences() {
        List<ModelObjectReference<? extends ModelObject>> list = new ArrayList<>();
        list.add(getClient());
        Optional.ofNullable(getParent())
                .ifPresent(list::add);
        list.addAll(getDomains());
        return list;
    }

    public abstract Unit toEntity(DtoToEntityContext context);
}
