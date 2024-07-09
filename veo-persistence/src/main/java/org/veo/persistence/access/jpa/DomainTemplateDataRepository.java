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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;

import org.veo.persistence.entity.jpa.DomainTemplateData;

public interface DomainTemplateDataRepository
    extends IdentifiableVersionedDataRepository<DomainTemplateData> {

  @Query(
      value =
          "select db_id from domaintemplate where name = ?1 order by templateVersion desc limit 1",
      nativeQuery = true)
  Optional<UUID> findLatestTemplateIdByName(String name);

  @Query(value = "select id from domaintemplate d where name = ?1 order by templateVersion")
  List<UUID> findTemplateIdsByName(String name);

  @Query(
      value = "SELECT MAX(templateversion) from domaintemplate where name = ?1",
      nativeQuery = true)
  Optional<String> findCurrentTemplateVersion(String templateName);

  @Query(
      """
        select dt from #{#entityName} dt
          left join fetch dt.profiles
          join fetch dt.riskDefinitionSet
          where dt.dbId = ?1
      """)
  Optional<DomainTemplateData> findByIdWithProfilesAndRiskDefinitions(UUID id);
}
