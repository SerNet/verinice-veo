/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
package org.veo.jobs;

import java.util.Optional;

import org.veo.core.entity.Client;
import org.veo.core.usecase.common.NameableInputData;
import org.veo.core.usecase.unit.CreateDemoUnitUseCase;
import org.veo.core.usecase.unit.CreateUnitUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class CreateClientUnitsJob {
  private final CreateDemoUnitUseCase createDemoUnitUseCase;
  private final CreateUnitUseCase createUnitUseCase;

  public void createUnitsForClient(Client client) {
    log.info("create unit and demo unit for client: {}/{}", client, client.getMaxUnits());
    AsSystemUser.runInClient(
        client,
        () -> {
          createUnitUseCase.execute(
              new CreateUnitUseCase.InputData(
                  new NameableInputData(Optional.empty(), "Unit 1", "", ""),
                  client.getId(),
                  Optional.empty(),
                  client.getMaxUnits()));
          createDemoUnitUseCase.execute(new CreateDemoUnitUseCase.InputData(client.getId()));
        });
  }
}