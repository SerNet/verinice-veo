/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
package org.veo.core.usecase.control;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.ControlRepository;
import org.veo.core.usecase.repository.UnitRepository;

import lombok.Value;

public class CreateControlUseCase<R>
        extends UseCase<CreateControlUseCase.InputData, CreateControlUseCase.OutputData, R> {

    private final UnitRepository unitRepository;
    private final ControlRepository controlRepository;

    public CreateControlUseCase(UnitRepository unitRepository,
            ControlRepository controlRepository) {
        this.unitRepository = unitRepository;
        this.controlRepository = controlRepository;
    }

    @Override
    public OutputData execute(InputData input) {
        Unit unit = unitRepository.findById(input.getControl()
                                                 .getOwner()
                                                 .getId())
                                  .orElseThrow(() -> new NotFoundException("Unit %s not found.",
                                          input.getControl()
                                               .getOwner()
                                               .getId()
                                               .uuidValue()));// the unit is already loaded
        unit.checkSameClient(input.authenticatedClient);
        input.getControl()
             .setId(Key.newUuid());
        input.control.version(input.username, null);
        return new OutputData(controlRepository.save(input.getControl()));
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Control control;
        Client authenticatedClient;
        String username;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        Control control;
    }
}
