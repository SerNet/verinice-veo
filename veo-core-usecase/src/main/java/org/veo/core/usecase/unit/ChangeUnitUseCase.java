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

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.state.UnitState;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.common.ETagMismatchException;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract superclass for all operations that change an asset. The <code>update()</code> method
 * must be overwritten to make all necessary changes to the asset.
 */
@RequiredArgsConstructor
@Slf4j
public abstract class ChangeUnitUseCase
    implements TransactionalUseCase<ChangeUnitUseCase.InputData, ChangeUnitUseCase.OutputData> {
  private final UnitRepository unitRepository;
  private final UnitValidator unitValidator;

  /**
   * Find a persisted unit object and reinstantiate it. Throws a domain exception if the requested
   * unit object was not found in the repository.
   */
  @Override
  public OutputData execute(InputData input) {
    log.info("Updating unit with id {}", input.getId());

    var storedUnit = unitRepository.getById(Key.uuidFrom(input.getId()));
    checkSameClient(storedUnit, input);
    checkETag(storedUnit, input);
    unitValidator.validateUpdate(input.changedUnit, storedUnit);
    var updatedUnit = update(storedUnit, input);
    save(updatedUnit, input);
    return output(unitRepository.getById(Key.uuidFrom(input.getId())));
  }

  protected abstract Unit update(Unit storedUnit, InputData input);

  protected Unit save(Unit unit, InputData input) {
    // Notice: by changing the context here it would be possible to change the view
    // of the entity that is being
    // returned after the save.
    // i.e. to exclude all references and collections:
    // "dataToEntityContext.partialUnit();"
    unit.setClient(input.getAuthenticatedClient());
    return this.unitRepository.save(unit);
  }

  private OutputData output(Unit unit) {
    return new OutputData(unit);
  }

  /**
   * Without this check, it would be possible to overwrite objects from other clients with our own
   * clientID, thereby hijacking these objects!
   *
   * @throws ClientBoundaryViolationException, if the client in the input and in the stored unit is
   *     not the same as in the authentication object
   */
  private void checkSameClient(Unit storedUnit, InputData input) {
    log.info(
        "Comparing clients {} and {}",
        input.getAuthenticatedClient().getId().uuidValue(),
        storedUnit.getClient().getId().uuidValue());
    storedUnit.checkSameClient(input.getAuthenticatedClient());
  }

  private void checkETag(Unit storedUnit, InputData input) {
    if (!ETag.matches(storedUnit.getId().uuidValue(), storedUnit.getVersion(), input.getETag())) {
      throw new ETagMismatchException(
          String.format(
              "The eTag does not match for the unit with the ID %s",
              storedUnit.getId().uuidValue()));
    }
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    String id;
    UnitState changedUnit;
    Client authenticatedClient;
    String eTag;
    String username;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid Unit unit;
  }
}
