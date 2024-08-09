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

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefDomains;
import org.veo.adapter.presenter.api.openapi.IdRefUnitParent;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;
import org.veo.core.entity.state.UnitState;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** Base transfer object for Units. Contains common data for all Unit DTOs. */
@Data
public abstract class AbstractUnitDto extends AbstractVersionedSelfReferencingDto
    implements NameableDto, UnitState {

  @Schema(description = "The name for the Unit.", example = "My unit", requiredMode = REQUIRED)
  private String name;

  @Schema(description = "The abbreviation for the Unit.", example = "U-96")
  private String abbreviation;

  @Schema(
      description = "The description for the Unit.",
      example = "This is currently the main and only unit for our organization.")
  private String description;

  @Schema(description = "The sub units for the Unit.", accessMode = Schema.AccessMode.READ_ONLY)
  private Set<IdRef<Unit>> units = Collections.emptySet();

  @Schema(implementation = IdRefUnitParent.class)
  private IdRef<Unit> parent;

  @ArraySchema(schema = @Schema(implementation = IdRefDomains.class))
  private Set<IdRef<Domain>> domains = Collections.emptySet();

  @Override
  public Class<Unit> getModelInterface() {
    return Unit.class;
  }

  @Override
  @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
  public UUID getSelfId() {
    return null;
  }
}
