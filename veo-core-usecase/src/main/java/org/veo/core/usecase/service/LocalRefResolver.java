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

import org.veo.core.entity.Identifiable;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.transform.IdentifiableFactory;

import lombok.RequiredArgsConstructor;

/** Resolves {@link ITypedId}s from an in-memory registry of entities. */
@RequiredArgsConstructor
public class LocalRefResolver implements IdRefResolver {
  private final IdentifiableFactory factory;
  protected final Map<ITypedId<?>, Identifiable> cache = new HashMap<>();

  public <TEntity extends Identifiable> TEntity resolve(ITypedId<TEntity> objectReference)
      throws NotFoundException {
    return resolve(Collections.singleton(objectReference)).iterator().next();
  }

  @Override
  public <TEntity extends Identifiable> Set<TEntity> resolve(
      Set<? extends ITypedId<TEntity>> objectReferences) {
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

  public <T extends Identifiable> T injectNewEntity(ITypedId<T> ref) {
    var entity = factory.create(ref.getType(), null);
    cache.put(ref, entity);
    return entity;
  }
}
