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

import java.time.Instant;

import org.veo.core.entity.Process;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.repository.ProcessRepository;

/**
 * Update a persisted process object.
 */
public class UpdateProcessUseCase<R> extends ModifyEntityUseCase<Process, R> {

    private final ProcessRepository processRepository;

    public UpdateProcessUseCase(ProcessRepository processRepository) {
        super();
        this.processRepository = processRepository;
    }

    @Override
    public OutputData<Process> performModification(InputData<Process> input) {
        Process process = input.getEntity();
        process.setValidFrom(Instant.now());
        return new OutputData<>(processRepository.save(process));

    }
}
