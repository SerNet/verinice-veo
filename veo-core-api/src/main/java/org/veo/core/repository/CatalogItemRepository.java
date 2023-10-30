/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler
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

import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;

/**
 * A repository for <code>CatalogItem</code> entities.
 *
 * <p>Implements basic CRUD operations from the superinterface and extends them with more specific
 * methods - i.e. queries based on particular fields.
 */
public interface CatalogItemRepository
    extends IdentifiableVersionedRepository<CatalogItem>,
        AbstractTemplateItemRepository<CatalogItem> {
  Set<CatalogItem> findAllByIdsFetchDomainAndTailoringReferences(Set<Key<UUID>> ids, Client client);

  CatalogItem getByIdInDomain(Key<UUID> catalogItemId, Domain domain);

  Set<CatalogItem> findAllByDomain(Domain domain);

  Set<SubTypeCount> getCountsBySubType(Domain domain);

  CatalogItemQuery query(Domain domain);
}
