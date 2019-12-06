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

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.EntityLayerSupertype.Lifecycle;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.asset.AssetRepository;
import org.veo.core.entity.process.Process;
import org.veo.core.entity.process.ProcessRepository;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.unit.GetUnitUseCase;

/**
 * Creates a persistent new process object.
 *
 *
 */
public class CreateProcessUseCase
        extends UseCase<CreateProcessUseCase.InputData, CreateProcessUseCase.OutputData> {

    private final ProcessRepository processRepository;
    private final AssetRepository assetRepository;
    private final GetUnitUseCase getUnitUseCase;

    public CreateProcessUseCase(ProcessRepository processRepository,
            AssetRepository assetRepository, GetUnitUseCase getUnitUseCase) {
        this.processRepository = processRepository;
        this.assetRepository = assetRepository;
        this.getUnitUseCase = getUnitUseCase;
    }

    @Override
    public OutputData execute(InputData input) {
        return new OutputData(createProcess(input, getUnit(input)));
    }

    @Transactional(TxType.REQUIRED)
    private Process createProcess(InputData input, Unit unit) {
        Process process = Process.newProcess(unit, input.getName());
        process.addAssets(assetRepository.getByIds(input.getAssetIds()));

        // change state from CREATING to STORED_CURRENT:
        process.setState(Lifecycle.STORED_CURRENT);

        // process with STORED_CURRENT state will only be returned if could be
        // persisted
        // otherwise an exception is thrown and the object discarded.
        return processRepository.save(process);
    }

    @Transactional(TxType.SUPPORTS)
    private Unit getUnit(InputData input) {
        GetUnitUseCase.InputData inputData = new GetUnitUseCase.InputData(input.getUnitId());
        return getUnitUseCase.execute(inputData)
                             .getUnit();
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        private final Key<UUID> id;
        private final Key<UUID> unitId;
        private final String name;
        private final Set<Key<UUID>> assetIds;
        private final Date validUntil;
        private final Date validFrom;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        private Process process;
    }
}
