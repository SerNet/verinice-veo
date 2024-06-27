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

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;

import org.veo.core.entity.Client;
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
                          left join fetch p.domain d
                          left join fetch p.domainTemplate
                          where i.symbolicDbId = ?2 and p.dbId= ?1 and d.owner.dbId = ?3
                      """)
  Optional<ProfileItem> findProfileItemByIdFetchTailoringReferences(
      String profileId, String itemId, String clientId);

  @Query(
      """
                                  select p from profile p
                                    left join fetch p.items i
                                    left join fetch i.tailoringReferences tr
                                    left join fetch i.appliedCatalogItem
                                    left join fetch p.domain d
                                    where p.dbId= ?1 and d.owner.dbId = ?2
                                """)
  Optional<Profile> findProfileByIdFetchTailoringReferences(String profileId, String clientId);

  @Query(
      """
                        select i from profile_item i
                           left join fetch i.tailoringReferences tr
                          left join fetch i.appliedCatalogItem
                          where i.symbolicDbId in ?1 and i.owner.domain.owner = ?2
                      """)
  Iterable<ProfileItem> findItemsByIdsFetchDomainAndTailoringReferences(
      Iterable<String> ids, Client client);

  @Query(
      """
                  select i from profile_item i
                    left join fetch i.tailoringReferences tr
                    left join fetch i.appliedCatalogItem
                    where i.owner.dbId = ?1 and i.owner.domain.owner = ?2
                """)
  Iterable<ProfileItem> findItemsByProfileIdFetchDomainAndTailoringReferences(
      String profileId, Client client);

  @Query("select ci from #{#entityName} ci where ci.domain = ?1")
  Set<Profile> findAllByDomain(DomainData domain);

  @Query("select ci from #{#entityName} ci where ci.domain.owner.dbId = ?1 and ci.domain.dbId = ?2")
  Set<Profile> findAllByDomainId(String clientId, String domainId);

  @Query("select ci from #{#entityName} ci where ci.domain.owner.dbId = ?1 and ci.dbId = ?2")
  Optional<Profile> findById(String clientId, String profileId);
}
