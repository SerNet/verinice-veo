/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.core.usecase.control;

import javax.validation.Valid;

import org.veo.core.entity.Control;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.ControlRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.IdAndClient;

import lombok.Value;

/**
 * Reinstantiate a persisted control object.
 */
public class GetControlUseCase
        implements TransactionalUseCase<IdAndClient, GetControlUseCase.OutputData> {

    private final ControlRepository repository;

    public GetControlUseCase(ControlRepository repository) {
        this.repository = repository;
    }

    @Override
    public OutputData execute(IdAndClient input) {
        Control control = repository.findById(input.getId())
                                    .orElseThrow(() -> new NotFoundException(input.getId()
                                                                                  .uuidValue()));
        control.checkSameClient(input.getAuthenticatedClient());
        return new OutputData(control);
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        Control control;
    }
}
