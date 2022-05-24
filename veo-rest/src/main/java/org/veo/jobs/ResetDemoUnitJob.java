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
package org.veo.jobs;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.veo.core.entity.Client;
import org.veo.core.entity.exception.ModelConsistencyException;
import org.veo.core.repository.ClientRepository;
import org.veo.core.usecase.unit.CreateDemoUnitUseCase;
import org.veo.core.usecase.unit.DeleteDemoUnitUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs a scheduled task that resets a "demo unit" with example content in the database at the
 * specified time interval for each client. If no demo unit exists, it will be created. If more than
 * one "demo unit" exists, this client will be skipped.
 *
 * <p>The point in time needs to be specified with the corresponding settings. The default is to
 * reset the example content at 03:00 am (UTC) every day.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ResetDemoUnitJob {

  private final ClientRepository clientRepository;

  private final DeleteDemoUnitUseCase deleteUseCase;

  private final CreateDemoUnitUseCase createUseCase;

  @Scheduled(cron = "${veo.scheduler.resetAllDemoUnits.cron}", zone = "UTC")
  public void resetAllDemoUnits() {
    log.info("Running scheduled job to reset demo units for all clients.");
    clientRepository.findAll().forEach(this::resetDemoUnit);
  }

  private void resetDemoUnit(final Client client) {
    AsSystemUser.runInClient(
        client,
        () -> {
          try {
            log.info("Resetting demo unit for client {}", client.getIdAsString());
            deleteUseCase.execute(new DeleteDemoUnitUseCase.InputData(client.getId()));
            createUseCase.execute(new CreateDemoUnitUseCase.InputData(client.getId()));
          } catch (ModelConsistencyException e) {
            log.error("Skipping client {}", client.getIdAsString(), e);
          }
        });
  }
}
