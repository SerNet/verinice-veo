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

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefCatalogDomainTemplate;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Identifiable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Deprecated() // TODO #2301 remove
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractCatalogDto extends AbstractVersionedSelfReferencingDto
    implements NameableDto {
  @Schema(description = "The name for the Catalog.", requiredMode = REQUIRED)
  private String name;

  @Schema(description = "The abbreviation for the Catalog.")
  private String abbreviation;

  @Schema(description = "The description for the Catalog.")
  private String description;

  @Schema(implementation = IdRefCatalogDomainTemplate.class)
  private IdRef<DomainBase> domainTemplate;

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return Domain.class;
  }
}
