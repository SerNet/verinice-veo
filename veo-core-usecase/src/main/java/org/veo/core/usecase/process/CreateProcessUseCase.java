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

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.validation.Valid;

import org.veo.core.entity.IUnitRepository;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.asset.Asset;
import org.veo.core.entity.asset.IAssetRepository;
import org.veo.core.entity.process.IProcessRepository;
import org.veo.core.entity.process.Process;
import org.veo.core.usecase.UseCase;

/**
 * Creates a persistent new process object.
 *
 * @author akoderman
 *
 */
public class CreateProcessUseCase
        extends UseCase<CreateProcessUseCase.InputData, CreateProcessUseCase.OutputData> {

    private IProcessRepository processRepository;
    private IAssetRepository assetRepository;
    private IUnitRepository unitRepository;

    public CreateProcessUseCase(IProcessRepository processRepository, IAssetRepository assetRepository, IUnitRepository unitRepository) {
        this.processRepository = processRepository;
        this.assetRepository = assetRepository;
        this.unitRepository = unitRepository;
    }

    @Override
    public OutputData execute(InputData input) {
        Process Process = createProcess(input);
        return new OutputData(processRepository.save(Process));
    }

    private Process createProcess(InputData input) {
        // todo use GetUnitByIdUsecase, that throws exception when unit not found. reused in other create use cases
        Process process = new Process(Key.undefined(), getUnit(input), input.getName());
        process.addAssets(assetRepository.getByIds(input.getAssetIds()));
        return process;
    }

    private Optional<Unit> getUnit(InputData input) {
        return unitRepository.findById(input.getKey());
    }

    // TODO: use lombok @Value instead?
    @Valid
    public static class InputData implements UseCase.InputData {

        private final Key<UUID> key;
        private final Key<UUID> unitId;
        private final String name;
        private final Set<Key<UUID>> assetIds;
        private final Date validUntil;
        private final Date validFrom;
        

        public Set<Key<UUID>> getAssetIds() {
            return Collections.unmodifiableSet(assetIds);
        }

        public Key<UUID> getKey() {
            return key;
        }

        public String getName() {
            return name;
        }
        

        public Date getValidUntil() {
            return validUntil;
        }

        public Date getValidFrom() {
            return validFrom;
        }

        public Key<UUID> getUnitId() {
            return unitId;
        }

        public InputData(Key<UUID> key, Key<UUID> unitId, String name, Set<Key<UUID>> assetIds, Date validFrom, Date validUntil) {
            this.key = key;
            this.unitId = unitId;
            this.name = name;
            this.assetIds = assetIds;
            this.validFrom = validFrom;
            this.validUntil = validUntil;
        }
    }

    // TODO: use lombok @Value instead?
    @Valid 
    public static class OutputData implements UseCase.OutputData {

        @Valid private Process process;

        public Process getProcess() {
            return process;
        }

        public OutputData(Process process) {
            this.process = process;
        }
    }
}
