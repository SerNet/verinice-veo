/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.usecase.base;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

/**
 * Common base class that can be used by all use cases. It includes a repository
 * provider to retrieve references to required repositories for each entity type
 * as well as convenience methods for entity retrieval and assertions that are
 * used in most use cases.
 */
public abstract class AbstractUseCase<I extends UseCase.InputData, O extends UseCase.OutputData>
        implements TransactionalUseCase<I, O> {

    protected final RepositoryProvider repositoryProvider;

    public AbstractUseCase(RepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    /**
     * Makes sure that all domains belong to the given Client.
     *
     * @param authenticatedClient
     *            the client which must be the owner of the domains
     * @param domains
     *            the list of the domains for which to ensure the ownership
     * @throws ClientBoundaryViolationException
     *             when any of the domains does not belong to the client.
     */
    protected void checkDomainOwnership(Client authenticatedClient, Set<Domain> domains) {
        domains.forEach(domain -> checkSameClient(authenticatedClient, domain));
    }

    private void checkSameClient(Client authenticatedClient, Domain domain) {
        if (!domain.getOwner()
                   .equals(authenticatedClient))
            throw new ClientBoundaryViolationException(domain, authenticatedClient);
    }

    protected <M extends ModelObject> Optional<M> findEntity(Class<M> clazz, Key<UUID> id) {
        return repositoryProvider.getRepositoryFor(clazz)
                                 .findById(id);
    }

    protected <M extends ModelObject> Set<M> findEntities(Class<M> clazz, Set<Key<UUID>> ids) {
        return repositoryProvider.getRepositoryFor(clazz)
                                 .getByIds(ids);
    }
}
