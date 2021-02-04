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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.ClientRepository;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;

import lombok.Value;

/**
 * Reinstantiate persisted entity objects.
 */
public abstract class GetEntitiesUseCase<T extends EntityLayerSupertype>
        extends UseCase<GetEntitiesUseCase.InputData, GetEntitiesUseCase.OutputData<T>> {

    private final EntityLayerSupertypeRepository<T> repository;
    private final ClientRepository clientRepository;
    private final UnitHierarchyProvider unitHierarchyProvider;

    public GetEntitiesUseCase(ClientRepository clientRepository,
            EntityLayerSupertypeRepository<T> repository,
            UnitHierarchyProvider unitHierarchyProvider) {
        this.clientRepository = clientRepository;
        this.repository = repository;
        this.unitHierarchyProvider = unitHierarchyProvider;
    }

    /**
     * Find persisted control objects and reinstantiate them. Throws a domain
     * exception if the (optional) requested parent unit was not found in the
     * repository.
     */
    @Override
    public OutputData<T> execute(InputData input) {
        Client client = clientRepository.findById(input.getAuthenticatedClient()
                                                       .getId())
                                        .orElseThrow(() -> new NotFoundException(
                                                "Invalid client ID"));
        var query = repository.query(client);

        if (input.getUnitUuid() != null) {
            query.whereUnitIn(input.getUnitUuid()
                                   .getValues()
                                   .stream()
                                   .flatMap((
                                           Key<UUID> rootUnitId) -> unitHierarchyProvider.findAllInRoot(rootUnitId)
                                                                                         .stream())
                                   .collect(Collectors.toSet()));
        }
        if (input.getSubType() != null) {
            query.whereSubTypeIn(input.getSubType()
                                      .getValues());
        }

        var result = query.execute();

        if (input.getDisplayName() != null) {
            result = filterByDisplayName(result, input.getDisplayName()
                                                      .getValues());
        }

        return new OutputData<>(result);
    }

    private List<T> filterByDisplayName(List<T> modelObjects, Set<String> displayNames) {
        return modelObjects.stream()
                           .filter(mo -> displayNames.stream()
                                                     .anyMatch(dn -> matchesDisplayName(mo, dn)))
                           .collect(Collectors.toList());
    }

    private boolean matchesDisplayName(T t, String displayName) {
        return t.getDisplayName()
                .toUpperCase()
                .contains(displayName.toUpperCase());
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Client authenticatedClient;
        QueryCondition<Key<UUID>> unitUuid;
        QueryCondition<String> displayName;
        QueryCondition<String> subType;
    }

    @Valid
    @Value
    public static class OutputData<T> implements UseCase.OutputData {
        @Valid
        List<T> entities;
    }
}