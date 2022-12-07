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
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.persistence.entity.jpa.DomainData;

public interface DomainDataRepository extends IdentifiableVersionedDataRepository<DomainData> {

  @Query(
      "select e from #{#entityName} as e join e.catalogs as c join c.catalogItems as i where i.dbId = ?1")
  Optional<DomainData> findByCatalogsCatalogItemsId(String catalogItemId);

  @Query("select e from #{#entityName} as e join e.domainTemplate as t where t.dbId = ?1")
  Collection<Domain> findAllByDomainTemplateId(String domainTemplateId);

  @Query(
      "select d from #{#entityName} d left join fetch d.elementTypeDefinitions where d.owner.id = ?1 and d.active = true")
  Set<DomainData> findAllActiveByClient(String clientId);

  @Query("select d from #{#entityName} d " + "where d.dbId = ?1 and d.owner.dbId = ?2")
  Optional<Domain> findById(String domainId, String clientId);

  @Query(
      """
        select d from #{#entityName} d
          join fetch d.profileSet
          join fetch d.riskDefinitionSet
          where d.owner.dbId = ?1 and d.active = true
      """)
  Set<DomainData> findActiveDomainsWithProfilesAndRiskDefinitions(String clientId);

  @Query(
      """
        select d from #{#entityName} d
          join fetch d.profileSet
          join fetch d.riskDefinitionSet
          where d.dbId = ?1 and d.owner.dbId = ?2
      """)
  Optional<DomainData> findByIdWithProfilesAndRiskDefinitions(String id, String clientId);

  @Query(
      """
        select d from #{#entityName} d
          left join fetch d.elementTypeDefinitions
          left join fetch d.riskDefinitionSet
          where d.owner.id = ?1
      """)
  Set<Domain> findAllByClientWithEntityTypeDefinitionsAndRiskDefinitions(String clientId);

  @Query("select count(d.id) > 0 from domain d where d.name = ?1 and d.owner = ?2")
  boolean nameExistsInClient(String name, Client client);
}
