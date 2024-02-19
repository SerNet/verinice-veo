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

import org.springframework.data.jpa.repository.Query;

import org.veo.core.entity.Client;
import org.veo.persistence.entity.jpa.ProfileItemData;
import org.veo.persistence.entity.jpa.ProfileTailoringReferenceData;

public interface ProfileItemDataRepository
    extends IdentifiableVersionedDataRepository<ProfileItemData> {

  @Query(
      """
         select pi from #{#entityName} pi
           where pi.dbId in ?1 and pi.owner.domain.owner = ?2
         """)
  Set<ProfileItemData> findAllByIds(Iterable<String> ids, Client client);

  @Query(
      """
         select tr from profile_tailoring_reference tr
           left join fetch tr.owner
           left join fetch tr.target
           where tr.dbId in ?1 and tr.owner.owner.domain.owner = ?2
         """)
  Set<ProfileTailoringReferenceData> findTailoringReferencesByIds(
      Iterable<String> ids, Client client);

  @Query(
      """
             select pi from #{#entityName} pi
               left join fetch pi.tailoringReferences
               join pi.owner as p
               join p.domainTemplate as dt
               where dt.dbId = ?1
             """)
  Set<ProfileItemData> findAllByDomainTemplateFetchTailoringReferences(String s);
}
