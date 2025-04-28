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

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Client;
import org.veo.core.entity.ClientOwned;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

/**
 * Common base class that can be used by all use cases. It includes a repository provider to
 * retrieve references to required repositories for each entity type as well as convenience methods
 * for entity retrieval and assertions that are used in most use cases.
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
   * @param authenticatedClient the client which must be the owner of the domains
   * @param domains the list of the domains for which to ensure the ownership
   * @throws ClientBoundaryViolationException when any of the domains does not belong to the client.
   */
  protected void checkDomainOwnership(Client authenticatedClient, Set<Domain> domains) {
    domains.forEach(domain -> checkSameClient(authenticatedClient, domain));
  }

  private <T extends Identifiable & ClientOwned> void checkSameClient(
      Client authenticatedClient, T entity) {
    if (!entity.getOwningClient().get().equals(authenticatedClient))
      throw new ClientBoundaryViolationException(entity, authenticatedClient);
  }

  protected <M extends Identifiable & ClientOwned> M getEntity(
      ITypedId<M> ref, UserAccessRights user) {
    if (Element.class.isAssignableFrom(
        ref.getType())) { // TODO: this is ugly and will be 'fixed' with verinice-veo#3950
      return (M)
          repositoryProvider
              .getElementRepositoryFor((Class<? extends Element>) ref.getType())
              .findById(ref.getId(), user)
              .orElseThrow(() -> new NotFoundException(ref.getId(), ref.getType()));
    }

    var e =
        repositoryProvider
            .getRepositoryFor(ref.getType())
            .findById(ref.getId())
            .orElseThrow(() -> new NotFoundException(ref.getId(), ref.getType()));
    user.checkClient(e);
    return e;
  }

  protected <M extends Identifiable> Optional<M> findEntity(Class<M> clazz, UUID id) {
    return repositoryProvider.getRepositoryFor(clazz).findById(id);
  }

  protected <M extends Element> Optional<M> findElement(
      Class<M> clazz, UUID id, UserAccessRights user) {
    return repositoryProvider.getElementRepositoryFor(clazz).findById(id, user);
  }

  protected <M extends Identifiable> Set<M> findEntities(Class<M> clazz, Set<UUID> ids) {
    return repositoryProvider.getRepositoryFor(clazz).findByIds(ids);
  }

  protected <M extends Element> Set<M> findElements(
      Class<M> clazz, Set<UUID> ids, UserAccessRights user) {
    return repositoryProvider.getElementRepositoryFor(clazz).findByIds(ids, user);
  }
}
