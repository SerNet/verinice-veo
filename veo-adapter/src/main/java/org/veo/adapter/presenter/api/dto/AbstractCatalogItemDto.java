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

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefCatalogItemCatalog;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Identifiable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractCatalogItemDto extends AbstractVersionedSelfReferencingDto
    implements ModelDto {
  @NotNull(message = "A catalog must be present.")
  @Schema(implementation = IdRefCatalogItemCatalog.class)
  private IdRef<Catalog> catalog;

  @Schema(description = "The tailoring references of this catalog item.")
  private Set<AbstractTailoringReferenceDto> tailoringReferences = new HashSet<>();

  @ToString.Include
  @Schema(description = "The namespace for the catalogitem.", example = "A1.B2")
  @Size(max = CatalogItem.NAMESPACE_MAX_LENGTH)
  private String namespace;

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return CatalogItem.class;
  }
}
