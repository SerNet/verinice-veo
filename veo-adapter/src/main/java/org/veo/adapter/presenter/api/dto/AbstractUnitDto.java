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

import java.util.Collections;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefDomains;
import org.veo.adapter.presenter.api.openapi.IdRefUnitParent;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Unit;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** Base transfer object for Units. Contains common data for all Unit DTOs. */
@Data
public abstract class AbstractUnitDto extends AbstractVersionedSelfReferencingDto
    implements NameableDto {

  @NotNull(message = "A name must be present.")
  @Schema(description = "The name for the Unit.", example = "My unit", required = true)
  @Size(max = Nameable.NAME_MAX_LENGTH)
  private String name;

  @Schema(description = "The abbreviation for the Unit.", example = "U-96")
  @Size(max = Nameable.ABBREVIATION_MAX_LENGTH)
  private String abbreviation;

  @Schema(
      description = "The description for the Unit.",
      example = "This is currently the main and only unit for our organization.")
  @Size(max = Nameable.DESCRIPTION_MAX_LENGTH)
  private String description;

  @JsonIgnore private IdRef<Client> client;

  @Schema(description = "The sub units for the Unit.", accessMode = Schema.AccessMode.READ_ONLY)
  private Set<IdRef<Unit>> units = Collections.emptySet();

  @Schema(implementation = IdRefUnitParent.class)
  private IdRef<Unit> parent;

  @ArraySchema(schema = @Schema(implementation = IdRefDomains.class))
  private Set<IdRef<Domain>> domains = Collections.emptySet();

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return Unit.class;
  }
}
