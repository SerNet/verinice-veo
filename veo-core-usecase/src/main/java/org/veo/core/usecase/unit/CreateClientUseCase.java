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
package org.veo.core.usecase.unit;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.Unit;
import org.veo.core.entity.UnitRepository;
import org.veo.core.usecase.UseCase;

/**
 * Create a new client and unit.
 *
 * @author akoderman
 *
 */
public class CreateClientUseCase
        extends UseCase<CreateClientUseCase.InputData, CreateClientUseCase.OutputData> {

    private final UnitRepository unitRepository;

    public CreateClientUseCase(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public OutputData execute(InputData input) {
        Unit unit = createUnit(input);
        return new OutputData(unitRepository.save(unit));
    }

    private Unit createUnit(InputData input) {
        return Unit.newUnitBelongingToClient(Client.newClient(input.getClientName()),
                                             input.getName());
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        private final String clientName;
        private final String name;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        private final Unit unit;
    }
}
