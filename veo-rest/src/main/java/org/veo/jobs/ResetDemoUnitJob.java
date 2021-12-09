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

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ResetDemoUnitJob {

    private ClientRepository clientRepository;

    private DeleteDemoUnitUseCase deleteUseCase;

    private CreateDemoUnitUseCase createUseCase;

    public ResetDemoUnitJob(ClientRepository clientRepository, DeleteDemoUnitUseCase deleteUseCase,
            CreateDemoUnitUseCase createUseCase) {
        this.clientRepository = clientRepository;
        this.deleteUseCase = deleteUseCase;
        this.createUseCase = createUseCase;
    }

    @Scheduled(cron = "${veo.scheduler.resetAllDemoUnits.cron}", zone = "UTC")
    public void resetAllDemoUnits() {
        log.info("Running scheduled job to reset demo units for all clients.");
        clientRepository.findAll()
                        .forEach(this::resetDemoUnit);
    }

    private void resetDemoUnit(Client client) {
        try {
            log.info("Resetting demo unit for client {}", client.getIdAsString());
            deleteUseCase.execute(new DeleteDemoUnitUseCase.InputData(client.getId()));
            createUseCase.execute(new CreateDemoUnitUseCase.InputData(client.getId()));
        } catch (ModelConsistencyException e) {
            log.error("Skipping client {}", client.getIdAsString(), e);
        }
    }
}
