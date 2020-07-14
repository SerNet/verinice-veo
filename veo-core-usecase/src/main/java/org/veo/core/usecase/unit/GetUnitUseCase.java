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

import java.util.UUID;

import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.UnitRepository;
import org.veo.core.usecase.unit.GetUnitsUseCase.InputData;
import org.veo.core.usecase.unit.GetUnitsUseCase.OutputData;

/**
 * Reinstantiate a persisted unit object.
 */
public class GetUnitUseCase<R>
        extends UseCase<GetUnitUseCase.InputData, GetUnitUseCase.OutputData, R> {

    private final UnitRepository repository;

    public GetUnitUseCase(UnitRepository repository) {
        this.repository = repository;
    }

    /**
     * Find a persisted unit object and reinstantiate it. Throws a domain exception
     * if the requested unit object was not found in the repository.
     */
    @Override
    public OutputData execute(InputData input) {
        Unit unit = repository.findById(input.getUnitId())
                              .orElseThrow(() -> new NotFoundException(input.getUnitId()
                                                                            .uuidValue()));
        checkSameClient(input.authenticatedClient, unit, unit);
        return new OutputData(unit);
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Key<UUID> unitId;
        Client authenticatedClient;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        Unit unit;

    }
}
