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
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.ProcessRepository;

/**
 * Reinstantiate a persisted process object.
 */
public class GetProcessUseCase extends UseCase<GetProcessUseCase.InputData, Process> {

    private final ProcessRepository repository;

    public GetProcessUseCase(ProcessRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public Process execute(InputData input) {
        Process process = repository.findById(input.getId(), null)
                                    .orElseThrow(() -> new NotFoundException(input.getId()
                                                                                  .uuidValue()));
        checkSameClient(input.getAuthenticatedClient(), process);
        return process;
    }

    @Valid
    @Value
    public static class InputData {
        private final Key<UUID> id;
        private final Client authenticatedClient;
    }
}
