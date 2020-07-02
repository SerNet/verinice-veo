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

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.Repository;
import org.veo.core.usecase.repository.RepositoryProvider;

public class DeleteGroupUseCase extends UseCase<DeleteGroupUseCase.InputData, Void> {

    private final RepositoryProvider repositoryProvider;

    public DeleteGroupUseCase(RepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public Void execute(InputData input) {

        Repository<? extends EntityLayerSupertype, Key<UUID>> repository = repositoryProvider.getRepositoryFor(input.groupType.entityClass);

        EntityLayerSupertype group = repository.findById(input.getId())
                                               .orElseThrow(() -> new NotFoundException(
                                                       "Group %s was not found.", input.getId()
                                                                                       .uuidValue()));
        // TODO VEO-124 this check should always be done implicitly by UnitImpl
        // or ModelValidator. Without this check, it would be possible to
        // overwrite objects from other clients with our own clientID, thereby
        // hijacking these objects!
        checkSameClient(input.authenticatedClient, group);
        // TODO VEO-161 also remove entity from links pointing to it

        repository.deleteById(group.getId());
        return null;

    }

    @Value
    public static class InputData {
        Key<UUID> id;
        GroupType groupType;
        Client authenticatedClient;
    }

}
