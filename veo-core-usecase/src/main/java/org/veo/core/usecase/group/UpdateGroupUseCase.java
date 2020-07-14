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

import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.ModelGroup;
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
public abstract class UpdateGroupUseCase<R>
        extends UseCase<UpdateGroupUseCase.InputData, UpdateGroupUseCase.OutputData, R> {

    protected final RepositoryProvider repositoryProvider;

    public UpdateGroupUseCase(RepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    public OutputData execute(InputData input) {
        return new OutputData(update(input));
    }

    protected abstract ModelGroup<?> update(InputData input);

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        @Valid
        ModelGroup<?> group;
        Client authenticatedClient;

    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        ModelGroup<?> group;

    }
}
