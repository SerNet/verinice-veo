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
package org.veo.persistence.access.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.persistence.entity.jpa.DomainData;

public interface DomainDataRepository extends IdentifiableVersionedDataRepository<DomainData> {

  @Query("select e.id from #{#entityName} as e join e.domainTemplate as t where t.id = ?1")
  Collection<String> findIdsByDomainTemplateId(UUID domainTemplateId);

  @Query(
      "select d from #{#entityName} d left join fetch d.elementTypeDefinitions where d.owner.id = ?1 and d.active = true")
  Set<DomainData> findAllActiveByClient(UUID clientId);

  @Query(
      "select d from #{#entityName} d left join fetch d.elementTypeDefinitions where d.owner.id = ?1")
  Set<DomainData> findAllByClient(UUID clientId);

  @Query("select d from #{#entityName} d " + "where d.id = ?1 and d.owner.id = ?2")
  Optional<Domain> findById(UUID domainId, UUID clientId);

  @Query("select d from #{#entityName} d where d.id in ?1 and d.owner.id = ?2")
  Set<Domain> findAllByIdInAndOwnerIdIs(Collection<UUID> domainIds, UUID clientId);

  @Query(
      """
        select d from #{#entityName} d
          join fetch d.decisionSet
          join fetch d.inspectionSet
          where d.id = ?1 and d.owner.id = ?2
    """)
  Optional<DomainData> findByIdWithDecisionsAndInspections(UUID domainId, UUID clientId);

  @Query(
      """
        select d from #{#entityName} d
          left join fetch d.profiles
          join fetch d.riskDefinitionSet
          where d.owner.id = ?1 and d.active = true
      """)
  Set<DomainData> findActiveDomainsWithProfilesAndRiskDefinitions(UUID clientId);

  @Query(
      """
        select d from #{#entityName} d
          left join fetch d.profiles
          join fetch d.riskDefinitionSet
          where d.id = ?1 and d.owner.id = ?2
      """)
  Optional<DomainData> findByIdWithProfilesAndRiskDefinitions(UUID id, UUID clientId);

  @Query(
      """
        select d from #{#entityName} d
          left join fetch d.elementTypeDefinitions
          left join fetch d.riskDefinitionSet
          where d.id in ?1 and d.owner.id = ?2 and d.active = true
      """)
  Set<Domain> findActiveByIdsAndClientWithEntityTypeDefinitionsAndRiskDefinitions(
      List<UUID> ids, UUID clientId);

  @Query("select count(d.id) > 0 from domain d where d.name = ?1 and d.owner = ?2")
  boolean nameExistsInClient(String name, Client client);
}
