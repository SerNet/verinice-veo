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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceEntityLayerSupertypeDomains;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceEntityLayerSupertypeOwner;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Unit;

/**
 * Transfer object for complete EntityLayerSupertypes.
 *
 * Contains all information of the EntityLayerSupertype.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class EntityLayerSupertypeDto extends BaseModelObjectDto implements NameAbleDto {

    @NotNull(message = "A name must be present.")
    @Schema(description = "The name for the EntityLayerSupertype.",
            example = "Lock doors",
            required = true)
    @ToString.Include
    private String name;

    @Schema(description = "The abbreviation for the EntityLayerSupertype.",
            example = "Lock doors",
            required = false)
    private String abbreviation;

    @Schema(description = "The description for the EntityLayerSupertype.",
            example = "Lock doors",
            required = false)
    private String description;

    @ArraySchema(schema = @Schema(implementation = ModelObjectReferenceEntityLayerSupertypeDomains.class))
    @Valid
    private Set<ModelObjectReference<Domain>> domains = Collections.emptySet();

    @Schema(name = "links",
            description = "Custom relations which do not affect the behavior.",
            title = "CustomLink")
    @Valid
    private Set<CustomLinkDto> links = Collections.emptySet();

    @Schema(description = "A custom property which is determined by the requested entity schema - see '/schemas'",
            name = "customAspects",
            title = "CustomAspect")
    @Valid
    private Set<CustomPropertiesDto> customAspects = Collections.emptySet();

    @NotNull(message = "A owner must be present.")
    @Schema(required = true, implementation = ModelObjectReferenceEntityLayerSupertypeOwner.class)
    private ModelObjectReference<Unit> owner;

    public Collection<ModelObjectReference<? extends ModelObject>> getReferences() {
        return Stream.concat(Stream.concat(getDomains().stream(), Stream.of(getOwner())),
                             getLinks().stream()
                                       .map(CustomLinkDto::getTarget))
                     .collect(Collectors.toList());
    }

}
