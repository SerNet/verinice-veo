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

import java.util.UUID;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;
import org.veo.core.usecase.repository.RepositoryProvider;

public class DeleteEntityUseCase extends UseCase<DeleteEntityUseCase.InputData, EmptyOutput> {

    private final RepositoryProvider repositoryProvider;

    public DeleteEntityUseCase(RepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public EmptyOutput execute(InputData input) {
        EntityLayerSupertypeRepository<? extends EntityLayerSupertype> repository = repositoryProvider.getEntityLayerSupertypeRepositoryFor(input.entityClass);
        EntityLayerSupertype entity = (EntityLayerSupertype) repository.findById(input.getId())
                                                                       .orElseThrow(() -> new NotFoundException(
                                                                               "%s %s was not found.",
                                                                               input.entityClass.getSimpleName(),
                                                                               input.getId()
                                                                                    .uuidValue()));
        checkSameClient(input.authenticatedClient, entity);
        // TODO VEO-127 also remove entity from references in bidirectional
        // relationships
        repository.deleteById(entity.getId());
        return EmptyOutput.INSTANCE;
    }

    @Value
    public static class InputData implements UseCase.InputData {
        private Class<? extends EntityLayerSupertype> entityClass;
        private final Key<UUID> id;
        private final Client authenticatedClient;

    }

}
