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
import java.util.stream.Collectors;

import org.veo.core.entity.Client;
import org.veo.core.entity.Identifiable;
import org.veo.core.usecase.common.NameableInputData;
import org.veo.core.usecase.unit.CreateUnitUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class CreateClientUnitsJob {
  private final CreateUnitUseCase createUnitUseCase;

  public void createUnitsForClient(Client client) {
    log.info("create initial unit for client: {}/{}", client, client.getMaxUnits());
    AsSystemUser.runInClient(
        client,
        () -> {
          createUnit(client, "Unit 1");
        });
  }

  private void createUnit(Client client, String name) {
    createUnitUseCase.execute(
        new CreateUnitUseCase.InputData(
            new NameableInputData(Optional.empty(), name, "", ""),
            client.getId(),
            Optional.empty(),
            client.getMaxUnits(),
            client.getDomains().stream().map(Identifiable::getId).collect(Collectors.toSet())));
  }
}
