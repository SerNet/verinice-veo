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

import java.util.UUID;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.impl.ControlImpl;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.ControlRepository;
import org.veo.core.usecase.repository.UnitRepository;

public class CreateControlUseCase extends UseCase<CreateControlUseCase.InputData, Control> {

    private final UnitRepository unitRepository;
    private final ControlRepository controlRepository;
    private final TransformContextProvider transformContextProvider;

    public CreateControlUseCase(UnitRepository unitRepository, ControlRepository controlRepository,
            TransformContextProvider transformContextProvider) {
        this.unitRepository = unitRepository;
        this.controlRepository = controlRepository;
        this.transformContextProvider = transformContextProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public Control execute(InputData input) {
        TransformTargetToEntityContext dataTargetToEntityContext = transformContextProvider.createTargetToEntityContext()
                                                                                           .partialClient()
                                                                                           .partialDomain();

        Unit unit = unitRepository.findById(input.getUnitId(), dataTargetToEntityContext)
                                  .orElseThrow(() -> new NotFoundException("Unit %s not found.",
                                          input.getUnitId()
                                               .uuidValue()));
        checkSameClient(input.authenticatedClient, unit, unit);
        Control control = new ControlImpl(Key.newUuid(), input.getName(), unit);

        return controlRepository.save(control);
    }

    @Valid
    @Value
    public static class InputData {
        Key<UUID> unitId;
        String name;
        Client authenticatedClient;
    }
}
