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
package org.veo.adapter.presenter.api.dto.full;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.AbstractVersionedSelfReferencingDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.adapter.presenter.api.dto.TailoringReferenceDto;
import org.veo.adapter.presenter.api.openapi.IdRefCatalogItemElement;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.VeoConstants;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@Deprecated // TODO #2301 remove
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class LegacyCatalogItemDto extends AbstractVersionedSelfReferencingDto
    implements NameableDto, IdentifiableDto {

  @Pattern(regexp = Patterns.UUID, message = VeoConstants.UUID_MESSAGE)
  @Schema(
      description = VeoConstants.UUID_MESSAGE,
      example = "adf037f1-0089-48ad-9177-92269918758b",
      format = "uuid")
  @ToString.Include
  private String id;

  @NotNull
  @Schema(description = "The name for the CatalogItem.")
  private String name;

  @Schema(description = "The abbreviation for the CatalogItem.")
  private String abbreviation;

  @Schema(description = "The actual element's description", accessMode = AccessMode.READ_ONLY)
  private String description;

  @ToString.Include
  @Schema(description = "The namespace for the catalogitem.", example = "A1.B2")
  @Size(max = CatalogItem.NAMESPACE_MAX_LENGTH)
  private String namespace;

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return CatalogItem.class;
  }

  @Schema(implementation = IdRefCatalogItemElement.class)
  private IdRef<Element> element;

  @Schema(description = "The tailoring references of this catalog item.")
  private Set<TailoringReferenceDto<CatalogItem>> tailoringReferences = new HashSet<>();
}
