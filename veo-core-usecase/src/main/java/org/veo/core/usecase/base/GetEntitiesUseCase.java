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
import java.util.Optional;
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
public abstract class GetEntitiesUseCase<T extends EntityLayerSupertype, R>
        extends UseCase<GetEntitiesUseCase.InputData, GetEntitiesUseCase.OutputData<T>, R> {

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
        if (input.getUnitUuid()
                 .isEmpty()) {
            return new OutputData<>(filterByDisplayName(repository.findByClient(client, false),
                                                        input.getDisplayName()));
        } else {
            var units = unitHierarchyProvider.findAllInRoot(Key.uuidFrom(input.getUnitUuid()
                                                                              .get()));
            return new OutputData<>(
                    filterByDisplayName(repository.findByUnits(units), input.getDisplayName()));
        }
    }

    private List<T> filterByDisplayName(List<T> modelObjects, Optional<String> displayName) {
        if (displayName.isEmpty())
            return modelObjects;
        return modelObjects.stream()
                           .filter(t -> matchesDisplayName(t, displayName))
                           .collect(Collectors.toList());
    }

    private boolean matchesDisplayName(T t, Optional<String> displayName) {
        return t.getDisplayName()
                .toUpperCase()
                .contains(displayName.get()
                                     .toUpperCase());
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Client authenticatedClient;
        Optional<String> unitUuid;
        Optional<String> displayName;
    }

    @Valid
    @Value
    public static class OutputData<T> implements UseCase.OutputData {
        @Valid
        List<T> entities;
    }
}