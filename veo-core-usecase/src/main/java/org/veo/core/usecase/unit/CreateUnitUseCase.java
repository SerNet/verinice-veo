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
import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.IClientRepository;
import org.veo.core.entity.IUnitRepository;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.usecase.UseCase;

/**
 * Create a new unit underneath an existing unit.
 * 
 * @author akoderman
 *
 */
public class CreateUnitUseCase
        extends UseCase<CreateUnitUseCase.InputData, CreateUnitUseCase.OutputData> {

    private final IUnitRepository unitRepository;

    public CreateUnitUseCase(IUnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @Override
    @Transactional
    public OutputData execute(InputData input) {
        Unit unit = createUnit(input);
        return new OutputData(unitRepository.save(unit));
    }

    private Unit createUnit(InputData input) {
        return new Unit(Key.newUuid(), input.getName(), input.getUnit().getClient());
    }
   

    // TODO: use lombok @Value instead?
    @Valid
    public static class InputData implements UseCase.InputData {

        @Valid private final Key<UUID> key;
        private final String name;
        @Valid private final Unit unit;

        public Key getKey() {
            return key;
        }

        public String getName() {
            return name;
        }
        

        public Unit getUnit() {
            return unit;
        }

        public InputData(Key<UUID> key, String name, Unit unit) {
            this.key = key;
            this.name = name;
            this.unit = unit;
        }
    }
    

    // TODO: use lombok @Value instead?
    @Valid
    public static class OutputData implements UseCase.OutputData {

        @Valid private final Unit unit;

        public Unit getUnit() {
            return unit;
        }

        public OutputData(Unit unit) {
            this.unit = unit;
        }
    }
}
