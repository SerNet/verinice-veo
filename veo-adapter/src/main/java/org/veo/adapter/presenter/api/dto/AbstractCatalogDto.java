/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import javax.validation.constraints.NotNull;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceCatalogDomainTemplate;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.ModelObject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractCatalogDto extends AbstractVersionedDto implements NameableDto {
    @NotNull(message = "A name must be present.")
    @Schema(description = "The name for the Catalog.", required = true)
    private String name;
    @Schema(description = "The abbreviation for the Catalog.")
    private String abbreviation;
    @Schema(description = "The description for the Catalog.")
    private String description;
    @Schema(implementation = ModelObjectReferenceCatalogDomainTemplate.class)
    private ModelObjectReference<DomainTemplate> domainTemplate;

    @Override
    public Class<? extends ModelObject> getModelInterface() {
        return Catalog.class;
    }

}