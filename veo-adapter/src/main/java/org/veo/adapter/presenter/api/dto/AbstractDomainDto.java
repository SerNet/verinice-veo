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

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceDomainCatalogs;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ModelObject;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Base transfer object for Domains. Contains common data for all Domain DTOs.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class AbstractDomainDto extends AbstractVersionedDto implements NameableDto {

    @NotNull(message = "A name must be present.")
    @Schema(description = "The name for the Domain.", example = "Data protection", required = true)
    @ToString.Include
    private String name;

    @Schema(description = "The abbreviation for the Domain.",
            example = "Data prot.",
            required = false)
    private String abbreviation;

    @Schema(description = "The description for the Domain.",
            example = "Everything around data protection.",
            required = false)
    private String description;

    @ArraySchema(schema = @Schema(implementation = ModelObjectReferenceDomainCatalogs.class))
    private Set<ModelObjectReference<Catalog>> catalogs = new HashSet<>();

    @Override
    public Class<? extends ModelObject> getModelInterface() {
        return Domain.class;
    }
}
