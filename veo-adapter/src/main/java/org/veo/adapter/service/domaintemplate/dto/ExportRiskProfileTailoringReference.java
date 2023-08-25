/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler.
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
package org.veo.adapter.service.domaintemplate.dto;

import java.util.Collections;
import java.util.Map;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.AbstractProfileTailoringReferenceDto;
import org.veo.adapter.presenter.api.openapi.IdRefCatalogReferenceCatalogItem;
import org.veo.core.entity.ProfileItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This DTO is used to represent an ExternalTailoringReference. It is primarily used in the
 * construction and serialization of a domain template.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
class ExportRiskProfileTailoringReference extends AbstractProfileTailoringReferenceDto {

  @Schema(implementation = IdRefCatalogReferenceCatalogItem.class)
  private IdRef<ProfileItem> mitigation;

  @Schema(implementation = IdRefCatalogReferenceCatalogItem.class)
  private IdRef<ProfileItem> riskOwner;

  @Schema(implementation = IdRefCatalogReferenceCatalogItem.class)
  private IdRef<ProfileItem> catalogItem;

  @Schema(description = "The properties of the risk element.", example = " name: 'value'")
  private Map<String, Object> riskAspects = Collections.emptyMap();
}
