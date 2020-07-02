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
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.ControlRepository;

/**
 * Reinstantiate a persisted control object.
 */
public class GetControlUseCase extends UseCase<GetControlUseCase.InputData, Control> {

    private final ControlRepository repository;
    private final TransformContextProvider transformContextProvider;

    public GetControlUseCase(ControlRepository repository,
            TransformContextProvider transformContextProvider) {
        this.repository = repository;
        this.transformContextProvider = transformContextProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public Control execute(InputData input) {
        TransformTargetToEntityContext dataTargetToEntityContext = transformContextProvider.createTargetToEntityContext()
                                                                                           .partialDomain()
                                                                                           .partialClient();
        Control control = repository.findById(input.getId(), dataTargetToEntityContext)
                                    .orElseThrow(() -> new NotFoundException(input.getId()
                                                                                  .uuidValue()));
        // TODO VEO-124 this check should always be done implicitly by UnitImpl or
        // ModelValidator. Without this check, it would be possible to overwrite
        // objects from other clients with our own clientID, thereby hijacking these
        // objects!
        checkSameClient(input.authenticatedClient, control);
        return control;
    }

    @Valid
    @Value
    public static class InputData {
        private final Key<UUID> id;
        private final Client authenticatedClient;
    }
}
