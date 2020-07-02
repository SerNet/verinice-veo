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
package org.veo.core.usecase.group;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.impl.BaseModelGroup;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.RepositoryProvider;

/**
 * Abstract superclass for all operations that change a group. The
 * <code>update()</code> method must be overwritten to make all necessary
 * changes to the group.
 *
 * Note: incrementing the version number of the key here will lead to a new
 * group being saved since the version is part of the entity ID together with
 * the UUID. In almost all cases changing the version number should be left to
 * the repository.
 */
public abstract class UpdateGroupUseCase
        extends UseCase<UpdateGroupUseCase.InputData, BaseModelGroup<?>> {

    protected final RepositoryProvider repositoryProvider;
    protected final TransformContextProvider transformContextProvider;

    public UpdateGroupUseCase(RepositoryProvider repositoryProvider,
            TransformContextProvider transformContextProvider) {
        this.repositoryProvider = repositoryProvider;
        this.transformContextProvider = transformContextProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public BaseModelGroup<?> execute(InputData input) {
        return update(input);
    }

    protected abstract BaseModelGroup<?> update(InputData input);

    @Valid
    @Value
    public static class InputData {
        @Valid
        private final BaseModelGroup<?> group;
        private final Client authenticatedClient;

    }
}
