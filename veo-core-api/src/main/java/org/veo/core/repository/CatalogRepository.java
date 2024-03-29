/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import java.util.UUID;

import org.veo.core.entity.Catalog;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;

/**
 * A repository for <code>Catalog</code> entities.
 *
 * <p>Implements basic CRUD operations from the superinterface and extends them with more specific
 * methods - i.e. queries based on particular fields.
 */
public interface CatalogRepository extends IdentifiableVersionedRepository<Catalog> {
  default Catalog getById(Key<UUID> id) {
    return findById(id).orElseThrow(() -> new NotFoundException(id, Catalog.class));
  }
}
