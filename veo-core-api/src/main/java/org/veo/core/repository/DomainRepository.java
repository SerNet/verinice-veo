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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;

/**
 * A repository for <code>Domain</code> entities.
 *
 * <p>Implements basic CRUD operations from the superinterface and extends them with more specific
 * methods - i.e. queries based on particular fields.
 */
public interface DomainRepository extends IdentifiableVersionedRepository<Domain> {

  Set<Domain> findAllByClient(Key<UUID> clientId);

  Set<Domain> findActiveDomainsWithProfilesAndRiskDefinitions(Key<UUID> clientId);

  Set<Domain> findAllByClientWithEntityTypeDefinitionsAndRiskDefinitions(Key<UUID> clientId);

  Set<Domain> findAllByTemplateId(Key<UUID> domainTemplateId);

  Optional<Domain> findByCatalogItem(CatalogItem catalogItem);

  Optional<Domain> findById(Key<UUID> domainId, Key<UUID> clientId);

  Optional<Domain> findByIdWithProfilesAndRiskDefinitions(Key<UUID> id, Key<UUID> clientId);
}
