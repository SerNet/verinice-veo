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
package org.veo.persistence.access.jpa;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import org.veo.persistence.entity.jpa.ClientData;

public interface ClientDataRepository extends IdentifiableVersionedDataRepository<ClientData> {

  @Query("select c from #{#entityName} c left join fetch c.domains where c.id = ?1")
  Optional<ClientData> findById(UUID id);

  @EntityGraph(attributePaths = {"domains.catalogItems"})
  Optional<ClientData> findWithCatalogsAndItemsById(UUID id);

  @EntityGraph(
      attributePaths = {"domains.catalogItems", "domains.catalogItems.tailoringReferences"})
  Optional<ClientData> findWithCatalogsAndItemsAndTailoringReferencesById(UUID id);

  @EntityGraph(attributePaths = {"domains.elementTypeDefinitions.translations"})
  Optional<ClientData> findWithTranslationsById(UUID id);

  @Query(
      """
                   select c from client c
                   where c.state = org.veo.core.entity.ClientState.ACTIVATED
                   and not exists (select d from domain d where d.owner = c and d.domainTemplate.id = ?1)
          """)
  Set<ClientData> findAllActiveWhereDomainTemplateNotApplied(UUID uuid);

  @Query(
      """
                       select c from client c
                       where c.state = org.veo.core.entity.ClientState.ACTIVATED
                       and not exists (select d from domain d where d.owner = c and d.domainTemplate.id = ?1)
                       and exists (select d from domain d where d.owner = c and d.domainTemplate.name = ?2)
              """)
  Set<ClientData> findAllActiveWhereDomainTemplateNotAppliedAndWithDomainTemplateOfName(
      UUID uuid, String name);
}
