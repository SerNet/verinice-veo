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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.aspects.SubTypeAspect;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractCatalogItemDto extends AbstractVersionedSelfReferencingDto
    implements NameableDto {
  @NotNull private String name;

  private String abbreviation;

  private String description;

  @NotNull
  @Schema(description = "Entity type identifier", example = "person")
  @Size(min = 1, max = 32)
  private String elementType;

  @NotNull
  @Schema(description = "The subtype for the Element.", example = "PER")
  @Size(min = 1, max = SubTypeAspect.SUB_TYPE_MAX_LENGTH)
  private String subType;

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return CatalogItem.class;
  }
}
