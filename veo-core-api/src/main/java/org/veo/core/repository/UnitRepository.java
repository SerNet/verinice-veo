/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;

/**
 * A repository for <code>Unit</code> entities.
 *
 * <p>Implements basic CRUD operations from the superinterface and extends them with more specific
 * methods - i.e. queries based on particular fields.
 */
public interface UnitRepository extends IdentifiableVersionedRepository<Unit> {

  List<Unit> findByClient(Client client);

  List<Unit> findByParent(Unit parent);

  Optional<Unit> findByIdFetchClient(Key<UUID> id);

  default Unit getById(Key<UUID> unitId) {
    return findById(unitId).orElseThrow(() -> new NotFoundException(unitId, Unit.class));
  }

  default Unit getByIdFetchClient(Key<UUID> unitId) {
    return findByIdFetchClient(unitId).orElseThrow(() -> new NotFoundException(unitId, Unit.class));
  }

  List<Unit> findByDomain(Key<UUID> domainId);
}
