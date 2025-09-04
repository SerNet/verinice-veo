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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;

import org.veo.persistence.entity.jpa.UnitData;

public interface UnitDataRepository extends IdentifiableVersionedDataRepository<UnitData> {

  @Query("select e from #{#entityName} as e where e.client.id = ?1 and (?2 = false or e.id in ?3)")
  List<UnitData> findByClientId(UUID clientId, boolean restrictUnitAccess, Set<UUID> access);

  @Query(
      "select e from #{#entityName} as e where  e.id = ?1 and e.client.id = ?2 and (?3 = false or e.id in ?4)")
  Optional<UnitData> findById(UUID id, UUID clientId, boolean restrictUnitAccess, Set<UUID> access);

  @Query(
      """
          select e from #{#entityName} as e
          left join fetch e.client
          where e.id = ?1 and e.client.id = ?2  and (?3 = false or e.id in ?4)
          """)
  Optional<UnitData> findWithClientById(
      UUID uuidValue, UUID clientId, boolean restrictUnitAccess, Set<UUID> access);

  List<UnitData> findByDomainsId(UUID domainId);
}
