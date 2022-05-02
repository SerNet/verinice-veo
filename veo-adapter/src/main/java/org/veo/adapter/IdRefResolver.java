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
package org.veo.adapter;

import java.util.Set;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.exception.NotFoundException;

/**
 * Resolves references to {@link Identifiable} entities ({@link IdRef}s) by returning the target
 * entity.
 */
public interface IdRefResolver {

  /**
   * Resolves the given reference by fetching the target entity from a cache or a repository.
   *
   * @param objectReference referencing the desired entity
   * @param <TEntity> target entity type
   * @throws NotFoundException when entity does not exist in the repository.
   * @throws org.veo.core.entity.specification.ClientBoundaryViolationException when entity does not
   *     belong to this resolver's client.
   */
  <TEntity extends Identifiable> TEntity resolve(IdRef<TEntity> objectReference)
      throws NotFoundException;

  /**
   * Resolves the given references by fetching the target entities from a cache or a repository.
   *
   * @param objectReferences referencing the desired entity
   * @param <TEntity> target entity type
   * @throws NotFoundException when one or more references cannot be resolved from the repository.
   * @throws org.veo.core.entity.specification.ClientBoundaryViolationException when one or more
   *     entities do not belong to this resolver's client.
   */
  <TEntity extends Identifiable> Set<TEntity> resolve(Set<IdRef<TEntity>> objectReferences);
}
