/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import org.veo.core.entity.Client;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.Profile;
import org.veo.persistence.entity.jpa.ProfileItemData;
import org.veo.persistence.entity.jpa.ProfileTailoringReferenceData;

public interface ProfileItemDataRepository extends CrudRepository<ProfileItemData, UUID> {

  @Query(
      """
         select pi from #{#entityName} pi
           where pi.owner.id = ?2 and pi.symbolicDbId in ?1 and pi.owner.domain.owner = ?3
         """)
  Set<ProfileItemData> findAllByIds(Iterable<UUID> symIds, UUID profileId, Client client);

  @Query(
      """
         from profile_tailoring_reference tr
           left join fetch tr.target
           where tr.id in ?1 and tr.owner.owner.domain.owner = ?2
         """)
  Set<ProfileTailoringReferenceData> findTailoringReferencesByIds(
      Iterable<String> ids, Client client);

  @Query(
      """
             select pi from #{#entityName} pi
               left join fetch pi.tailoringReferences
               join pi.owner as p
               join p.domainTemplate as dt
               where dt.id = ?1
             """)
  Set<ProfileItemData> findAllByDomainTemplateFetchTailoringReferences(UUID s);

  @Query("select pi from #{#entityName} pi where pi.owner = ?1 and pi.elementType= ?2")
  Set<ProfileItemData> findAllByProfile(Profile profile, ElementType type);
}
