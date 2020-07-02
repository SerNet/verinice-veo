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
package org.veo.core.usecase.base;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.usecase.UseCase;

public abstract class ModifyEntityUseCase<T extends EntityLayerSupertype>
        extends UseCase<ModifyEntityUseCase.InputData<T>, ModifyEntityUseCase.OutputData<T>> {
    protected final TransformContextProvider transformContextProvider;

    protected ModifyEntityUseCase(TransformContextProvider transformContextProvider) {
        this.transformContextProvider = transformContextProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public OutputData<T> execute(InputData<T> input) {
        T entity = input.getEntity();
        Client authenticatedClient = input.getAuthenticatedClient();
        checkSameClient(authenticatedClient, entity);
        return performModification(input);
    }

    protected abstract OutputData<T> performModification(InputData<T> input);

    @Valid
    @Value
    public static class InputData<T> implements UseCase.InputData {

        @Valid
        private final T entity;

        @Valid
        private final Client authenticatedClient;

    }

    @Valid
    @Value
    public static class OutputData<T> implements UseCase.OutputData {

        @Valid
        private final T entity;

    }
}
