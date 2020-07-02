/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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

import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.usecase.repository.UnitRepository;

public class UpdateUnitUseCase extends ChangeUnitUseCase {

    public UpdateUnitUseCase(UnitRepository repository,
            TransformContextProvider transformContextProvider) {
        super(repository, transformContextProvider);
    }

    @Override
    @Transactional(TxType.REQUIRED)
    protected Unit update(Unit storedUnit, ChangeUnitUseCase.InputData input) {
        // replace stored unit with changed unit:
        return input.getChangedUnit();
    }

}
