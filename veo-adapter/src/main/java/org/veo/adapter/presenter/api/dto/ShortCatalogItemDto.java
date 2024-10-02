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

import java.util.UUID;

import jakarta.validation.Valid;

import javax.annotation.Nullable;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.DomainBase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Partial catalog item */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShortCatalogItemDto extends AbstractTemplateItemDto<CatalogItem, DomainBase> {

  @ToString.Include private UUID id;

  @Valid
  @Nullable
  @Schema(
      description =
          "A subset of the catalog item's custom aspects - only those explicitly requested. If no custom aspects are explicitly requested, this is null. Custom aspects are groups of customizable attributes, see /domains/{domainId} to find the available custom aspects for an element type.",
      title = "CustomAspect")
  private CustomAspectMapDto customAspects = null;

  @Override
  public Class<CatalogItem> getModelInterface() {
    return CatalogItem.class;
  }
}
