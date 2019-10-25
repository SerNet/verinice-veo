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
import java.util.Set;

import org.veo.core.entity.Key;
import org.veo.core.entity.asset.Asset;
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

    private IProcessRepository ProcessRepository;

    public CreateProcessUseCase(IProcessRepository ProcessRepository) {
        this.ProcessRepository = ProcessRepository;
    }

    @Override
    public OutputData execute(InputData input) {
        Process Process = createProcess(input);
        return new OutputData(ProcessRepository.store(Process));
    }

    private Process createProcess(InputData input) {
        Process process = new Process(Key.undefined(), input.getName());
        process.addAssets(input.getAssets());
        return process;
    }

    // TODO: use lombok @Value instead?
    public static class InputData implements UseCase.InputData {

        private final Key key;
        private final String name;
        private final Set<Asset> assets;

        public Set<Asset> getAssets() {
            return Collections.unmodifiableSet(assets);
        }

        public Key getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public InputData(Key key, String name, Set<Asset> assets) {
            this.key = key;
            this.name = name;
            this.assets = assets;
        }
    }

    // TODO: use lombok @Value instead?
    public static class OutputData implements UseCase.OutputData {

        private final Process Process;

        public Process getProcess() {
            return Process;
        }

        public OutputData(Process Process) {
            this.Process = Process;
        }
    }
}
