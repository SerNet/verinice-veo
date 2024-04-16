/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.repository;

import java.util.Optional;
import java.util.Set;

import org.veo.core.entity.Client;
import org.veo.core.entity.Entity;
import org.veo.core.entity.ref.IEntityRef;

public interface RepositoryBase<T extends Entity, TRef extends IEntityRef<T>> {
  Set<T> findAllByRefs(Set<TRef> refs, Client client);

  default Optional<T> findByRef(TRef ref, Client client) {
    return findAllByRefs(Set.of(ref), client).stream().findFirst();
  }
}
