/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
import org.veo.core.entity.Key;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;

/**
 * A repository for <code>CatalogItem</code> entities.
 *
 * <p>Implements basic CRUD operations from the superinterface and extends them with more specific
 * methods - i.e. queries based on particular fields.
 */
public interface ProfileRepository extends IdentifiableVersionedRepository<Profile> {
  Set<ProfileItem> findItemsByIdsFetchDomainAndTailoringReferences(
      Set<Key<UUID>> profileItemIds, Client client);

  Set<ProfileItem> findItemsByProfileIdFetchDomainAndTailoringReferences(
      Key<UUID> profileId, Client client);

  Set<Profile> findAllByDomain(Domain domain);

  Set<Profile> findAllByDomainId(Key<UUID> clientId, Key<UUID> domainId);

  Optional<ProfileItem> findProfileItemByIdFetchTailoringReferences(
      Key<UUID> profileId, Key<UUID> itemId, Key<UUID> clientId);

  Optional<Profile> findById(Key<UUID> clientId, Key<UUID> profileId);
}
