/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.usecase.process;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.ProcessRepository;
import org.veo.core.usecase.repository.UnitRepository;

import lombok.Value;

/**
 * Creates a persistent new process object.
 */
public class CreateProcessUseCase<R>
        extends UseCase<CreateProcessUseCase.InputData, CreateProcessUseCase.OutputData, R> {

    private final UnitRepository unitRepository;
    private final ProcessRepository processRepository;
    private final EntityFactory entityFactory;

    public CreateProcessUseCase(UnitRepository unitRepository, ProcessRepository processRepository,
            EntityFactory entityFactory) {
        this.unitRepository = unitRepository;
        this.processRepository = processRepository;
        this.entityFactory = entityFactory;
    }

    @Override
    public OutputData execute(InputData input) {
        Unit unit = unitRepository.findById(input.getProcess()
                                                 .getOwner()
                                                 .getId())
                                  .orElseThrow(() -> new NotFoundException("Unit %s not found.",
                                          input.getProcess()
                                               .getOwner()
                                               .getId()));// the unit is already loaded
        unit.checkSameClient(input.authenticatedClient);
        input.getProcess()
             .setId(Key.newUuid());
        verifyInput(input.getProcess());
        return new OutputData(processRepository.save(input.getProcess()));
    }

    private void verifyInput(Process process) {
        // This needs to be done for all where we accecpt complete entities

    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        // private final Key<UUID> unitId;
        // private final String name;
        Process process;
        Client authenticatedClient;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        Process process;
    }
}
