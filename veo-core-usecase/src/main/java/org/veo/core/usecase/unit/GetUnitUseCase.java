/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.core.usecase.unit;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EntityId;

/** Reinstantiate a persisted unit object. */
public class GetUnitUseCase implements TransactionalUseCase<EntityId, GetUnitUseCase.OutputData> {

  private final UnitRepository repository;

  public GetUnitUseCase(UnitRepository repository) {
    this.repository = repository;
  }

  /**
   * Find a persisted unit object and reinstantiate it. Throws a domain exception if the requested
   * unit object was not found in the repository.
   */
  @Override
  public OutputData execute(EntityId input, UserAccessRights userAccessRights) {
    Unit unit =
        repository
            .findById(input.id(), userAccessRights)
            .orElseThrow(() -> new NotFoundException(input.id(), Unit.class));
    return new OutputData(unit);
  }

  @Valid
  public record OutputData(@Valid Unit unit) implements UseCase.OutputData {}
}
