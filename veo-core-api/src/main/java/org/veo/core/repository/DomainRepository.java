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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.exception.NotFoundException;

import lombok.NonNull;

/**
 * A repository for <code>Domain</code> entities.
 *
 * <p>Implements basic CRUD operations from the superinterface and extends them with more specific
 * methods - i.e. queries based on particular fields.
 */
public interface DomainRepository extends IdentifiableVersionedRepository<Domain> {

  Set<Domain> findAllActiveByClient(UUID clientId);

  Set<Domain> findActiveDomainsWithProfilesAndRiskDefinitions(UUID clientId);

  Set<Domain> findActiveByIdsAndClientWithEntityTypeDefinitionsAndRiskDefinitions(
      Collection<UUID> domainIds, UUID clientId);

  Set<UUID> findIdsByTemplateId(UUID domainTemplateId);

  Optional<Domain> findById(UUID domainId, UUID clientId);

  Domain getActiveById(UUID domainId, UUID clientId);

  Domain getById(@NonNull UUID domainId, @NonNull UUID clientId);

  Set<Domain> findByIds(@NonNull Set<UUID> ids, @NonNull UUID clientId);

  Set<Domain> getByIds(@NonNull Set<@NonNull UUID> domainIds, @NonNull UUID clientId);

  Optional<Domain> findByIdWithProfilesAndRiskDefinitions(UUID id, UUID clientId);

  default Domain getById(UUID id) {
    return findById(id).orElseThrow(() -> new NotFoundException(id, Domain.class));
  }

  boolean nameExistsInClient(String name, Client client);

  Domain getByIdWithDecisionsAndInspections(UUID domainId, UUID clientId);

  default Domain getActiveByIdWithElementTypeDefinitionsAndRiskDefinitions(
      UUID domainId, UUID clientId) {
    return findActiveByIdsAndClientWithEntityTypeDefinitionsAndRiskDefinitions(
            List.of(domainId), clientId)
        .stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException(domainId, Domain.class));
  }
}
