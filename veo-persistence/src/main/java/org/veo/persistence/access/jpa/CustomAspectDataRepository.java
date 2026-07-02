/*
 * verinice.veo
 * Copyright (C) 2026  Aziz Khalledi.
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
 */
package org.veo.persistence.access.jpa;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import org.veo.persistence.entity.jpa.CustomAspectData;

public interface CustomAspectDataRepository extends JpaRepository<CustomAspectData, UUID> {

  @Query(
      "select ca from custom_aspect ca "
          + "where ca.owner.owner.id = ?1 "
          + "and ca.domain.id = ?2 "
          + "and ca.type in ?3")
  @Transactional(readOnly = true)
  List<CustomAspectData> findByUnitDomainAndTypes(UUID unitId, UUID domainId, Set<String> types);
}
