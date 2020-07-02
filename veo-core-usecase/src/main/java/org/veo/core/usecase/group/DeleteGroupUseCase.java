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

import java.util.UUID;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.Repository;
import org.veo.core.usecase.repository.RepositoryProvider;

public class DeleteGroupUseCase
        extends UseCase<DeleteGroupUseCase.InputData, DeleteGroupUseCase.OutputData> {

    private final RepositoryProvider repositoryProvider;

    public DeleteGroupUseCase(RepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public OutputData execute(InputData input) {

        Repository<? extends EntityLayerSupertype, Key<UUID>> repository = repositoryProvider.getRepositoryFor(input.groupType.entityClass);

        EntityLayerSupertype group = repository.findById(input.getId())
                                               .orElseThrow(() -> new NotFoundException(
                                                       "Group %s was not found.", input.getId()
                                                                                       .uuidValue()));
        // TODO VEO-124 this check should always be done implicitly by UnitImpl or
        // ModelValidator. Without this check, it would be possible to overwrite
        // objects from other clients with our own clientID, thereby hijacking these
        // objects!
        checkSameClient(input.authenticatedClient, group);
        // TODO VEO-127 also remove entity from references in bidirectional
        // relationships

        repository.deleteById(group.getId());
        return new OutputData(input.getId());

    }

    @Value
    public static class InputData implements UseCase.InputData {
        private final Key<UUID> id;
        private final GroupType groupType;
        private final Client authenticatedClient;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        private Key<UUID> id;
    }

}
