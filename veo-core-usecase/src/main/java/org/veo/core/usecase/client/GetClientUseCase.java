/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
package org.veo.core.usecase.client;

import java.util.UUID;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.ClientRepository;

/**
 * Reinstantiate a persisted client object.
 */
public class GetClientUseCase extends UseCase<Key<UUID>, Client> {

    private final ClientRepository repository;

    public GetClientUseCase(ClientRepository repository) {
        this.repository = repository;
    }

    /**
     * Find a persisted client object and reinstantiate it. Throws a domain
     * exception if the requested client object was not found in the repository.
     */
    @Override
    @Transactional(TxType.SUPPORTS)
    public Client execute(Key<UUID> id) {
        return repository.findById(id)
                         .orElseThrow(() -> new NotFoundException(id.uuidValue()));
    }
}
