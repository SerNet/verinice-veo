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

import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullTailoringReferenceDto;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceCatalogItemCatalog;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Catalogable;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Incident;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * Base transfer object for CatalogItems. Contains common data for all
 * CatalogItem DTOs.
 */
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class AbstractCatalogItemDto implements VersionedDto, ModelDto {

    @NotNull(message = "A catalog must be present.")
    @Schema(implementation = ModelObjectReferenceCatalogItemCatalog.class)
    private ModelObjectReference<Catalog> catalog;
    @Schema(description = "The tailoring references of this catalog item.")
    private Set<FullTailoringReferenceDto> tailoringReferences;
    @Schema(description = "The actual model instance of this catalog item.",
            required = false,
            oneOf = { AbstractEntityLayerSupertypeDto.class })
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = FullAssetDto.class, name = Asset.SINGULAR_TERM),
            @JsonSubTypes.Type(value = FullControlDto.class, name = Control.SINGULAR_TERM),
            @JsonSubTypes.Type(value = FullDocumentDto.class, name = Document.SINGULAR_TERM),
            @JsonSubTypes.Type(value = FullIncidentDto.class, name = Incident.SINGULAR_TERM),
            @JsonSubTypes.Type(value = FullPersonDto.class, name = Person.SINGULAR_TERM),
            @JsonSubTypes.Type(value = FullProcessDto.class, name = Process.SINGULAR_TERM),
            @JsonSubTypes.Type(value = FullScenarioDto.class, name = Scenario.SINGULAR_TERM) })
    private ModelObjectReference<Catalogable> element;
    // TODO: VEO-504 Implement UpdateReference
    @ToString.Include
    @Schema(description = "The namespace for the catalogitem.", example = "A1.B2")
    private String namespace;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this entity was created.",
            example = "1990-12-31T23:59:60Z",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Pattern(regexp = Patterns.DATETIME)
    private String createdAt;

    @Schema(description = "The username of the user who created this object.",
            example = "jane_doe",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this entity was created.",
            example = "1990-12-31T23:59:60Z",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Pattern(regexp = Patterns.DATETIME)
    private String updatedAt;

    @Schema(description = "The username of the user who last updated this object.",
            example = "jane_doe",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String updatedBy;

    @JsonIgnore
    private long version;

    @Override
    public Class<? extends ModelObject> getModelInterface() {
        return CatalogItem.class;
    }

}