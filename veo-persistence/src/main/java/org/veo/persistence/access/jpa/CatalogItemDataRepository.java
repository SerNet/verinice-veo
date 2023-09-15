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

import java.util.Set;

import jakarta.annotation.Nonnull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.CatalogItem;
import org.veo.core.repository.SubTypeCount;
import org.veo.persistence.entity.jpa.CatalogItemData;
import org.veo.persistence.entity.jpa.DomainData;

public interface CatalogItemDataRepository
    extends IdentifiableVersionedDataRepository<CatalogItemData> {

  @Query(
      """
            select ci from #{#entityName} ci
              left join fetch ci.domain
              left join fetch ci.domainTemplate
              left join fetch ci.tailoringReferences tr
              left join fetch tr.target
              where ci.dbId in ?1
          """)
  Iterable<CatalogItemData> findAllByIdsFetchDomainAndTailoringReferences(Iterable<String> ids);

  @Query("select ci from #{#entityName} ci where ci.domain = ?1")
  Set<CatalogItem> findAllByDomain(DomainData domain);

  @Query(
      """
            select new org.veo.core.repository.SubTypeCount(ci.elementType ,ci.subType, count(ci.subType))
            from #{#entityName} as ci
            where ci.domain.dbId = ?1
            group by ci.elementType, ci.subType
""")
  Set<SubTypeCount> getCountsBySubType(String domainId);

  @Nonnull
  @Transactional(readOnly = true)
  Page<CatalogItemData> findAll(Specification<CatalogItemData> specification, Pageable pageable);
}
