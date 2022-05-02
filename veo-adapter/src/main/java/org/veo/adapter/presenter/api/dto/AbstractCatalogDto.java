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
import javax.validation.constraints.Size;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefCatalogDomainTemplate;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractCatalogDto extends AbstractVersionedSelfReferencingDto
    implements NameableDto {
  @NotNull(message = "A name must be present.")
  @Schema(description = "The name for the Catalog.", required = true)
  @Size(max = Nameable.NAME_MAX_LENGTH)
  private String name;

  @Schema(description = "The abbreviation for the Catalog.")
  @Size(max = Nameable.ABBREVIATION_MAX_LENGTH)
  private String abbreviation;

  @Schema(description = "The description for the Catalog.")
  @Size(max = Nameable.DESCRIPTION_MAX_LENGTH)
  private String description;

  @Schema(implementation = IdRefCatalogDomainTemplate.class)
  private IdRef<DomainTemplate> domainTemplate;

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return Catalog.class;
  }
}
