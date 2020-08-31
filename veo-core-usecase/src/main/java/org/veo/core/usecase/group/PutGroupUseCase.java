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

import java.time.Instant;
import java.util.Optional;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.repository.Repository;
import org.veo.core.usecase.repository.RepositoryProvider;

public class PutGroupUseCase<R> extends UpdateGroupUseCase<R> {

    public PutGroupUseCase(RepositoryProvider repositoryProvider) {
        super(repositoryProvider);
    }

    @Override
    protected ModelGroup<?> update(InputData input) {
        ModelGroup<?> group = input.getGroup();
        group.setValidFrom(Instant.now());
        Repository repository = repositoryProvider.getRepositoryFor(input.getGroup()
                                                                         .getClass());
        Optional<ModelGroup<?>> existingGroup = repository.findById(group.getId());
        if (existingGroup.isEmpty()) {
            throw new NotFoundException("Group %s was not found.", group.getId()
                                                                        .uuidValue());
        }

        Client authenticatedClient = input.getAuthenticatedClient();
        checkSameClient(authenticatedClient, (EntityLayerSupertype) existingGroup.get());

        return (ModelGroup<?>) repository.save(group);
    }

}
