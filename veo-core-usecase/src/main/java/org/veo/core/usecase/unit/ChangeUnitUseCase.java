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

import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Client;
import org.veo.core.entity.Unit;
import org.veo.core.entity.state.UnitState;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.ETag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract superclass for all operations that change an unit. The <code>update()</code> method must
 * be overwritten to make all necessary changes to the unit.
 */
@RequiredArgsConstructor
@Slf4j
public abstract class ChangeUnitUseCase
    implements TransactionalUseCase<ChangeUnitUseCase.InputData, ChangeUnitUseCase.OutputData> {
  private final UnitRepository unitRepository;
  private final UnitValidator unitValidator;
  private final ClientRepository clientRepository;

  /**
   * Find a persisted unit object and reinstantiate it. Throws a domain exception if the requested
   * unit object was not found in the repository.
   */
  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    log.info("Updating unit with id {}", input.id);

    userAccessRights.checkUnitUpdateAllowed();
    var storedUnit = unitRepository.getById(input.id, userAccessRights);
    var client = clientRepository.getById(userAccessRights.getClientId());
    // check client and read unit access is done by the repository, write access by userAccessRights
    ETag.validate(input.eTag, storedUnit);
    unitValidator.validateUpdate(input.changedUnit, storedUnit);
    var updatedUnit = update(storedUnit, input.changedUnit, client);
    save(updatedUnit, client);
    return output(unitRepository.getById(input.id, userAccessRights));
  }

  protected abstract Unit update(Unit storedUnit, UnitState changedUnit, Client client);

  protected Unit save(Unit unit, Client client) {
    // Notice: by changing the context here it would be possible to change the view
    // of the entity that is being
    // returned after the save.
    // i.e. to exclude all references and collections:
    // "dataToEntityContext.partialUnit();"
    unit.setClient(client);
    return this.unitRepository.save(unit);
  }

  private OutputData output(Unit unit) {
    return new OutputData(unit);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(UUID id, UnitState changedUnit, String eTag)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid Unit unit) implements UseCase.OutputData {}
}
