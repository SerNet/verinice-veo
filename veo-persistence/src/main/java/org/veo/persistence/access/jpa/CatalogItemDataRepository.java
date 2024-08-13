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
import java.util.UUID;

import jakarta.annotation.Nonnull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.repository.SubTypeCount;
import org.veo.persistence.entity.jpa.CatalogItemData;
import org.veo.persistence.entity.jpa.CatalogTailoringReferenceData;
import org.veo.persistence.entity.jpa.DomainData;

public interface CatalogItemDataRepository extends CrudRepository<CatalogItemData, Key<UUID>> {

  @Query("select e from #{#entityName} e where e.symbolicDbId = ?1 and e.domain = ?2")
  Optional<CatalogItem> findByIdInDomain(UUID id, DomainData domain);

  @Query(
      """
         select ci from #{#entityName} ci
           left join fetch ci.domain
           left join fetch ci.domainTemplate
           where ci.symbolicDbId in ?1 and ci.domain.owner = ?2
         """)
  Set<CatalogItemData> findAllByIdsFetchDomain(Iterable<UUID> ids, Client client);

  @Query(
      """
            select ci from #{#entityName} ci
              left join fetch ci.domain
              left join fetch ci.domainTemplate
              left join fetch ci.tailoringReferences tr
              left join fetch tr.target
              where ci.symbolicDbId in ?1 and ci.domain = ?2
          """)
  Set<CatalogItemData> findAllByIdsFetchTailoringReferences(Iterable<UUID> symIds, Domain domain);

  @Query("select ci from #{#entityName} ci where ci.domain = ?1")
  Set<CatalogItem> findAllByDomain(DomainData domain);

  @Query(
      """
            select new org.veo.core.repository.SubTypeCount(ci.elementType ,ci.subType, count(ci.subType))
            from #{#entityName} as ci
            where ci.domain.dbId = ?1
            group by ci.elementType, ci.subType
""")
  Set<SubTypeCount> getCountsBySubType(UUID domainId);

  @Nonnull
  @Transactional(readOnly = true)
  Page<CatalogItemData> findAll(Specification<CatalogItemData> specification, Pageable pageable);

  @Query(
      """
         select tr from catalog_tailoring_reference tr
           left join fetch tr.owner
           left join fetch tr.target
           where tr.dbId in ?1 and tr.owner.domain.owner = ?2
         """)
  Set<CatalogTailoringReferenceData> findTailoringReferencesByIds(
      Iterable<String> ids, Client client);

  @Query(
      """
                select ci from #{#entityName} ci
                  left join fetch ci.tailoringReferences
                  join ci.domainTemplate as dt
                  where dt.dbId = ?1
              """)
  Set<CatalogItemData> findAllByDomainTemplateFetchTailoringReferences(UUID domainTemplateId);

  @Query(
      """
    select ci from catalogitem ci
        where ci.symbolicDbId in ?1 and ci.domain.dbId = ?2 and ci.domain.owner.dbId = ?3
  """)
  Set<CatalogItem> findAllByIdsAndDomain(
      Collection<UUID> symbolicIds, UUID domainId, UUID clientId);

  @Query(
      """
    select ci from catalogitem ci
        where ci.symbolicDbId in ?1 and ci.domainTemplate.dbId = ?2
  """)
  Set<CatalogItem> findAllByIdsAndDomainTemplate(Set<UUID> symbolicIds, UUID domainTemplateId);
}
