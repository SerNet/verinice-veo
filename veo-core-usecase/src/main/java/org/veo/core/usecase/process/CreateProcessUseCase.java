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

import java.util.UUID;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.impl.ProcessImpl;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.ProcessRepository;
import org.veo.core.usecase.repository.UnitRepository;

/**
 * Creates a persistent new process object.
 */
public class CreateProcessUseCase
        extends UseCase<CreateProcessUseCase.InputData, CreateProcessUseCase.OutputData> {

    private final UnitRepository unitRepository;
    private final TransformContextProvider transformContextProvider;
    private final ProcessRepository processRepository;

    public CreateProcessUseCase(UnitRepository unitRepository, ProcessRepository processRepository,
            TransformContextProvider transformContextProvider) {
        this.unitRepository = unitRepository;
        this.processRepository = processRepository;
        this.transformContextProvider = transformContextProvider;
    }

    @Transactional(TxType.REQUIRED)
    @Override
    public OutputData execute(InputData input) {
        TransformTargetToEntityContext dataTargetToEntityContext = transformContextProvider.createTargetToEntityContext()
                                                                                           .partialClient()
                                                                                           .partialDomain();
        Unit unit = unitRepository.findById(input.getUnitId(), dataTargetToEntityContext)
                                  .orElseThrow(() -> new NotFoundException("Unit %s not found.",
                                          input.getUnitId()
                                               .uuidValue()));
        checkSameClient(input.authenticatedClient, unit, unit);
        Process process = new ProcessImpl(Key.newUuid(), input.getName(), unit);
        return new OutputData(processRepository.save(process));
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        private final Key<UUID> unitId;
        private final String name;
        private final Client authenticatedClient;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        private Process process;
    }
}
