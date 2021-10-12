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
import javax.validation.constraints.Size;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefDomainCatalogs;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;

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
public abstract class AbstractDomainDto extends AbstractVersionedSelfReferencingDto
        implements NameableDto {

    @NotNull(message = "A name must be present.")
    @Schema(description = "The name for the Domain.", example = "Data protection", required = true)
    @Size(max = Nameable.NAME_MAX_LENGTH)
    @ToString.Include
    private String name;

    @Schema(description = "The abbreviation for the Domain.", example = "Data prot.")
    @Size(max = Nameable.ABBREVIATION_MAX_LENGTH)
    private String abbreviation;

    @Schema(description = "The description for the Domain.",
            example = "Everything around data protection.")
    @Size(max = Nameable.DESCRIPTION_MAX_LENGTH)
    private String description;

    @NotNull(message = "An authority must be present.")
    @Schema(description = "The orgnization that published a standard",
            example = "ISO",
            required = true,
            accessMode = Schema.AccessMode.READ_ONLY)
    private String authority;

    @NotNull(message = "A templateVersion must be present.")
    @Schema(description = "Template version in semver format",
            example = "1.0",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String templateVersion;

    @NotNull(message = "A revision must be present.")
    @Schema(description = "Addition version info",
            example = "latest",
            required = true,
            accessMode = Schema.AccessMode.READ_ONLY)
    private String revision;

    @ArraySchema(schema = @Schema(implementation = IdRefDomainCatalogs.class))
    private Set<IdRef<Catalog>> catalogs = new HashSet<>();

    private IdRef<DomainTemplate> domainTemplate;

    @Override
    public Class<? extends Identifiable> getModelInterface() {
        return Domain.class;
    }
}
