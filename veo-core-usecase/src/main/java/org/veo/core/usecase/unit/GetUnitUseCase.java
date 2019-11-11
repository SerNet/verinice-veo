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

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.veo.core.entity.IUnitRepository;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.NotFoundException;

/**
 * Reinstantiate a persisted unit object.
 *
 *
 */
public class GetUnitUseCase
        extends UseCase<GetUnitUseCase.InputData, GetUnitUseCase.OutputData> {

    private final IUnitRepository repository;

    public GetUnitUseCase(IUnitRepository repository) {
        this.repository = repository;
    }

    /**
     * Find a persisted unit object and reinstantiate it.
     * Throws a domain exception if the requested unit object was not found in the repository.
     */
    @Override
    @Transactional(TxType.SUPPORTS)
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
        @Valid private final Unit unit;

        public Unit getUnit() {
            return unit;
        }

        public OutputData(Object unit) {
            this.unit = (Unit) unit;
        }
    }
}
