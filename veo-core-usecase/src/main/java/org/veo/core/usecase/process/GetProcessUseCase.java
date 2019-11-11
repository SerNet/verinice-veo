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
import javax.validation.Valid;

import org.veo.core.entity.Key;
import org.veo.core.entity.process.IProcessRepository;
import org.veo.core.entity.process.Process;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.NotFoundException;

/**
 * Reinstantiate a persisted process object.
 *
 * @author akoderman
 *
 */
public class GetProcessUseCase
        extends UseCase<GetProcessUseCase.InputData, GetProcessUseCase.OutputData> {

    private final IProcessRepository repository;

    public GetProcessUseCase(IProcessRepository repository) {
        this.repository = repository;
    }

    @Override
    public OutputData execute(InputData input) {
        // @formatter:off
        return repository
                .findById(input.getId())
                .map(OutputData::new)
                .orElseThrow(() -> new NotFoundException(input.getId().uuidValue()));
        // @formatter:on
    }

    @Valid
    public static class InputData implements UseCase.InputData {
        private final Key<UUID> id;

        protected Key<UUID> getId() {
            return id;
        }

        public InputData(Key<UUID> id) {
            this.id = id;
        }
    }
    @Valid
    public static class OutputData implements UseCase.OutputData {
        @Valid private final Process process;

        public Process getProcess() {
            return process;
        }

        public OutputData(Object process) {
            this.process = (Process) process;
        }
    }
}
