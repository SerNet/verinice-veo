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

import java.util.List;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Unit;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

/** Reinstantiate a persisted unit object. */
public class GetUnitsUseCase
    implements TransactionalUseCase<UseCase.EmptyInput, GetUnitsUseCase.OutputData> {

  private final UnitRepository unitRepository;

  public GetUnitsUseCase(ClientRepository repository, UnitRepository unitRepository) {
    this.unitRepository = unitRepository;
  }

  /**
   * Find a persisted unit object and reinstantiate it. Throws a domain exception if the requested
   * unit object was not found in the repository.
   */
  @Override
  public OutputData execute(EmptyInput input, UserAccessRights userAccessRights) {
    return new OutputData(unitRepository.findByUser(userAccessRights));
  }

  @Valid
  public record OutputData(@Valid List<Unit> units) implements UseCase.OutputData {}
}
