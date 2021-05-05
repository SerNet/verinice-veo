/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.core.usecase.process;

import javax.validation.Valid;

import org.veo.core.entity.Process;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.IdAndClient;

import lombok.Value;

/**
 * Reinstantiate a persisted process object.
 */
public class GetProcessUseCase
        implements TransactionalUseCase<IdAndClient, GetProcessUseCase.OutputData> {

    private final ProcessRepository repository;

    public GetProcessUseCase(ProcessRepository repository) {
        this.repository = repository;
    }

    @Override
    public OutputData execute(IdAndClient input) {
        Process process = repository.findById(input.getId())
                                    .orElseThrow(() -> new NotFoundException(input.getId()
                                                                                  .uuidValue()));
        process.checkSameClient(input.getAuthenticatedClient());
        return new OutputData(process);
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        Process process;
    }
}
