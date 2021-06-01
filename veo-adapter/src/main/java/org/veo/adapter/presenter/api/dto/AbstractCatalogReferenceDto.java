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

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefCatalogReferenceCatalogItem;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.UpdateReference;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * The base dto class for all catalog references like {@link TailoringReference}
 * or {@link UpdateReference}. As long as we do not have an editor, (see
 * VEO-399) it is only used internally. Therefore all schema annotations are
 * quite useless as these are not part of any client api.
 */
@Data
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class AbstractCatalogReferenceDto implements VersionedDto {

    @Schema(implementation = IdRefCatalogReferenceCatalogItem.class)
    private IdRef<CatalogItem> catalogItem;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this entity was created.",
            example = "1990-12-31T23:59:60Z",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Pattern(regexp = Patterns.DATETIME)
    private String createdAt;

    @Schema(description = "The username of the user who created this object.",
            example = "jane_doe",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this version of the entity was saved.",
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

}