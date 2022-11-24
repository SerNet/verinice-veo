/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman
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

import static org.veo.core.usecase.unit.CreateDemoUnitUseCase.DEMO_UNIT_NAME;

import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.veo.core.entity.Key;
import org.veo.core.entity.exception.ModelConsistencyException;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.client.GetClientUseCase;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Delete the unit called 'Demo' from a client. This name is reserved for units filled with example
 * content. If the user has created a unit called "Demo" by themselves (or if there is more than one
 * "Demo"-unit present for whatever reason) this function will refuse to work and log an error.
 */
// FIXME VEO-805 use a flag instead of name for demo units
@Slf4j
@RequiredArgsConstructor
public class DeleteDemoUnitUseCase
    implements TransactionalUseCase<DeleteDemoUnitUseCase.InputData, UseCase.EmptyOutput> {

  private final DeleteUnitUseCase deleteUnitUseCase;
  private final GetClientUseCase getClientUseCase;
  private final GetUnitsUseCase getUnitsUseCase;

  @Transactional
  @Override
  public EmptyOutput execute(DeleteDemoUnitUseCase.InputData input) {
    var client =
        getClientUseCase.execute(new GetClientUseCase.InputData(input.getClientId())).getClient();
    var demoUnits =
        getUnitsUseCase
            .execute(new GetUnitsUseCase.InputData(client, Optional.empty()))
            .getUnits()
            .stream()
            .filter(unit -> unit.getName().equals(DEMO_UNIT_NAME))
            .toList();

    if (demoUnits.size() > 1)
      throw new ModelConsistencyException(
          "Client %s should contain 1 demo unit, but %d were found",
          client.getIdAsString(), demoUnits.size());
    if (demoUnits.isEmpty()) {
      log.warn("Client {} contained no demo unit - nothing to delete.", client.getIdAsString());
      return EmptyOutput.INSTANCE;
    }

    log.info("Deleting demo unit {}", demoUnits.get(0).getIdAsString());
    deleteUnitUseCase.execute(new DeleteUnitUseCase.InputData(demoUnits.get(0).getId(), client));

    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Key<UUID> clientId;
  }
}
