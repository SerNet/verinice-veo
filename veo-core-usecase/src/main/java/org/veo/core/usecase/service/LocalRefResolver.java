/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Entity;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.ref.IEntityRef;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.entity.transform.IdentifiableFactory;

import lombok.RequiredArgsConstructor;

/** Resolves {@link ITypedId}s from an in-memory registry of entities. */
@RequiredArgsConstructor
public class LocalRefResolver implements IdRefResolver {
  private final IdentifiableFactory factory;
  protected final Map<IEntityRef<?>, Entity> cache = new HashMap<>();

  @Override
  public <TEntity extends Entity> TEntity resolve(IEntityRef<TEntity> objectReference)
      throws NotFoundException {
    return resolve(Collections.singleton(objectReference)).iterator().next();
  }

  @Override
  public <TEntity extends Entity, TRef extends IEntityRef<TEntity>> Set<TEntity> resolve(
      Set<? extends TRef> objectReferences) {
    return objectReferences.stream()
        .map(
            ref ->
                Optional.ofNullable((TEntity) cache.get(ref))
                    .orElseThrow(
                        () ->
                            new UnprocessableDataException(
                                "%s not found".formatted(ref.toString()))))
        .collect(Collectors.toSet());
  }

  public <T extends Entity> T injectNewEntity(IEntityRef<T> ref) {
    var entity = factory.create(ref.getType());
    inject(entity, ref);
    return entity;
  }

  public <T extends Entity> void inject(T entity, IEntityRef<T> ref) {
    if (cache.containsKey(ref)) {
      throw new UnprocessableDataException("Duplicate key: %s".formatted(ref));
    }
    cache.put(ref, entity);
    // #2834 avoid this mess
    if (ref instanceof ITypedSymbolicId<?, ?> symId) {
      if (symId.getNamespaceType().equals(Domain.class)) {
        cache.put(
            TypedSymbolicId.from(
                symId.getSymbolicId(),
                (Class) symId.getType(),
                TypedId.from(symId.getNamespaceId(), DomainTemplate.class)),
            entity);
      }
      if (symId.getNamespaceType().equals(DomainTemplate.class)) {
        cache.put(
            TypedSymbolicId.from(
                symId.getSymbolicId(),
                (Class) symId.getType(),
                TypedId.from(symId.getNamespaceId(), Domain.class)),
            entity);
      }
    }
  }
}
