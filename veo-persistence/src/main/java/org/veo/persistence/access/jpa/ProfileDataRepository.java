/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler.
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

import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.ProfileData;

public interface ProfileDataRepository extends IdentifiableVersionedDataRepository<ProfileData> {

  @Query(
      """
                  select i from profile_item i
                     left join fetch i.tailoringReferences tr
                    left join fetch i.appliedCatalogItem
                    left join fetch i.owner p
                    left join fetch p.domain
                    left join fetch p.domainTemplate
                    where i.dbId in ?1
                """)
  Iterable<ProfileItem> findAllByIdsFetchDomainAndTailoringReferences(Iterable<String> ids);

  @Query(
      """
                  select i from profile_item i
                    left join fetch i.tailoringReferences tr
                    left join fetch i.appliedCatalogItem
                    left join fetch i.owner p
                    left join fetch p.domain
                    left join fetch p.domainTemplate
                    where i.owner.dbId = ?1
                """)
  Iterable<ProfileItem> findAllByIdsFetchDomainAndTailoringReferences(String profileId);

  @Query("select ci from #{#entityName} ci where ci.domain = ?1")
  Set<Profile> findAllByDomain(DomainData domain);
}
