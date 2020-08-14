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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.ClientRepository;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;
import org.veo.core.usecase.repository.RepositoryProvider;
import org.veo.core.usecase.repository.UnitRepository;

import lombok.Value;

/**
 * Reinstantiate persisted group objects.
 */
public class GetGroupsUseCase<T extends ModelGroup<? extends EntityLayerSupertype>, R>
        extends UseCase<GetGroupsUseCase.InputData, GetGroupsUseCase.OutputData<T>, R> {

    private final RepositoryProvider repositoryProvider;
    private final ClientRepository clientRepository;
    private final UnitRepository unitRepository;

    public GetGroupsUseCase(ClientRepository clientRepository, UnitRepository unitRepository,
            RepositoryProvider repositoryProvider) {
        this.clientRepository = clientRepository;
        this.repositoryProvider = repositoryProvider;
        this.unitRepository = unitRepository;
    }

    /**
     * Find persisted group objects and reinstantiate them. Throws a domain
     * exception if the (optional) requested parent unit was not found in the
     * repository.
     */
    @Override
    public OutputData<T> execute(InputData input) {
        Client client = clientRepository.findById(input.getAuthenticatedClient()
                                                       .getId())
                                        .orElseThrow(() -> new NotFoundException(
                                                "Invalid client ID"));
        EntityLayerSupertypeRepository<? extends EntityLayerSupertype> groupRepository = repositoryProvider.getEntityLayerSupertypeRepositoryFor(input.groupType.entityClass);

        if (input.getUnitUuid()
                 .isEmpty()) {
            return new OutputData<T>((List<T>) groupRepository.findGroupsByClient(client));
        } else {
            Key<UUID> parentId = Key.uuidFrom(input.getUnitUuid()
                                                   .get());
            Unit owner = unitRepository.findById(parentId)
                                       .orElseThrow(() -> new NotFoundException(
                                               "Invalid parent ID: %s", input.getUnitUuid()
                                                                             .get()));
            return new OutputData<T>((List<T>) groupRepository.findGroupsByUnit(owner));
        }
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Client authenticatedClient;
        GroupType groupType;
        Optional<String> unitUuid;
    }

    @Valid
    @Value
    public static class OutputData<T> implements UseCase.OutputData {
        @Valid
        List<T> groups;
    }
}