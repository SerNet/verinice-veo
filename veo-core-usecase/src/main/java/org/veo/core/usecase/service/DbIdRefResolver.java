/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.core.usecase.service;

import static java.lang.String.format;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.ReferenceTargetNotFoundException;
import org.veo.core.entity.ref.IEntityRef;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.EntitySpecifications;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.repository.Repository;
import org.veo.core.repository.RepositoryProvider;

/**
 * Resolves {@link ITypedId}s by fetching the target entity from a repository. Instances of this
 * class should NOT be long-lived, because it uses an entity cache.
 */
public class DbIdRefResolver extends LocalRefResolver {
  private final RepositoryProvider repositoryProvider;
  private final Client client;

  DbIdRefResolver(
      RepositoryProvider repositoryProvider, IdentifiableFactory factory, Client client) {
    super(factory);
    this.repositoryProvider = repositoryProvider;
    this.client = client;
  }

  /**
   * Resolves the given references by fetching the target entities from a cache or a repository.
   *
   * @param objectReferences referencing the desired entity
   * @param <TEntity> target entity type
   * @throws NotFoundException when one or more references cannot be resolved from the repository.
   * @throws ClientBoundaryViolationException when one or more entities do not belong to this
   *     resolver's client.
   */
  @Override
  public <TEntity extends Identifiable> Set<TEntity> resolve(
      Set<? extends ITypedId<TEntity>> objectReferences) {
    if (objectReferences.isEmpty()) {
      return Collections.emptySet();
    }
    HashSet<TEntity> result = new HashSet<>(objectReferences.size());
    HashSet<ITypedId<TEntity>> copyOfReferences = new HashSet<>(objectReferences);
    Iterator<ITypedId<TEntity>> it = copyOfReferences.iterator();
    while (it.hasNext()) {
      ITypedId<TEntity> ref = it.next();
      Identifiable cachedEntry = cache.get(ref);
      if (cachedEntry != null) {
        result.add((TEntity) cachedEntry);
        it.remove();
      }
    }
    if (copyOfReferences.isEmpty()) {
      return result;
    }

    Class<TEntity> entityType = objectReferences.iterator().next().getType();

    Repository<? extends Identifiable, Key<UUID>> entityRepository =
        repositoryProvider.getRepositoryFor(entityType);

    Set<? extends Identifiable> entities =
        entityRepository.findByIds(
            copyOfReferences.stream()
                .map(ITypedId::getId)
                .map(Key::uuidFrom)
                .collect(Collectors.toSet()));

    Map<String, ITypedId<TEntity>> copyOfReferencesById =
        copyOfReferences.stream().collect(Collectors.toMap(ITypedId::getId, Function.identity()));

    entities.forEach(
        entity -> {
          if (entity instanceof Unit unit) {
            unit.checkSameClient(client);
          }
          if (entity instanceof Element element) {
            element.checkSameClient(client);
          }
          if (entity instanceof Domain domain) {
            if (!(EntitySpecifications.hasSameClient(client)).isSatisfiedBy((domain).getOwner()))
              throw new ClientBoundaryViolationException(entity, client);
          }
          result.add((TEntity) entity);
          ITypedId<TEntity> reference = copyOfReferencesById.get(entity.getId().uuidValue());
          cache.put(reference, entity);
          copyOfReferences.remove(reference);
        });
    if (!copyOfReferences.isEmpty()) {
      throw new ReferenceTargetNotFoundException(
          format(
              "Reference target(s) not found: %s",
              copyOfReferences.stream()
                  .map(IEntityRef::toString)
                  .collect(Collectors.joining(", "))));
    }
    return result;
  }
}
