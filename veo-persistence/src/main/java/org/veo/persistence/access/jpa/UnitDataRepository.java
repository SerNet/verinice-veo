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
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import org.veo.persistence.entity.jpa.UnitData;

public interface UnitDataRepository extends IdentifiableVersionedDataRepository<UnitData> {

  @Query("select e from #{#entityName} as e where e.parent.dbId = ?1")
  List<UnitData> findByParentId(UUID parentId);

  @Query("select e from #{#entityName} as e where e.client.dbId = ?1")
  List<UnitData> findByClientId(UUID clientId);

  @EntityGraph(attributePaths = {"client"})
  Optional<UnitData> findWithClientByDbId(UUID uuidValue);

  List<UnitData> findByDomainsId(UUID domainId);
}
