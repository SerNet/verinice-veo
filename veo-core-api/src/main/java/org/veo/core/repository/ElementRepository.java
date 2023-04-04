/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;

public interface ElementRepository<T extends Element> extends IdentifiableVersionedRepository<T> {

  Set<T> findByUnit(Unit owner);

  ElementQuery<T> query(Client client);

  Set<T> findByDomain(Domain domain);

  Set<SubTypeStatusCount> getCountsBySubType(Unit u, Domain domain);

  void deleteAll(Set<T> entities);

  Optional<T> findById(Key<UUID> id, Key<UUID> clientId);

  T getById(Key<UUID> id, Key<UUID> clientId);
}
