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
import java.util.UUID;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.ClientRepository;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;

/**
 * Reinstantiate persisted entity objects.
 */
public abstract class GetEntitiesUseCase<T extends EntityLayerSupertype>
        extends UseCase<GetEntitiesUseCase.InputData, List<T>> {

    private final EntityLayerSupertypeRepository<T> repository;
    private final ClientRepository clientRepository;

    public GetEntitiesUseCase(ClientRepository clientRepository,
            EntityLayerSupertypeRepository<T> repository) {
        this.clientRepository = clientRepository;
        this.repository = repository;
    }

    /**
     * Find persisted control objects and reinstantiate them. Throws a domain
     * exception if the (optional) requested parent unit was not found in the
     * repository.
     */
    @Override
    @Transactional(TxType.REQUIRED)
    public List<T> execute(InputData input) {
        Client client = clientRepository.findById(input.getAuthenticatedClient()
                                                       .getId())
                                        .orElseThrow(() -> new NotFoundException(
                                                "Invalid client ID"));
        if (input.getUnitUuid()
                 .isEmpty()) {
            return repository.findByClient(client, false);
        } else {
            Key<UUID> parentId = Key.uuidFrom(input.getUnitUuid()
                                                   .get());
            Unit owner = client.getUnit(parentId)
                               .orElseThrow(() -> new NotFoundException("Invalid parent ID: %s",
                                       input.getUnitUuid()
                                            .get()));
            return repository.findByUnit(owner, false);
        }

    }

    @Valid
    @Value
    public static class InputData {
        private final Client authenticatedClient;
        private final Optional<String> unitUuid;
    }
}