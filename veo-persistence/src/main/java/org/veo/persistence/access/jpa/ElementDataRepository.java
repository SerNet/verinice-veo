/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

import jakarta.annotation.Nonnull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.repository.SubTypeStatusCount;
import org.veo.persistence.entity.jpa.ElementData;

public interface ElementDataRepository<T extends ElementData>
    extends JpaRepository<T, String>,
        JpaSpecificationExecutor<T>,
        IdentifiableVersionedDataRepository<T> {

  @Query(
      "select e from #{#entityName} as e "
          + "left join fetch e.customAspects "
          + "left join fetch e.domains "
          + "left join fetch e.decisionResultsAspects "
          + "left join fetch e.subTypeAspects "
          + "left join fetch e.appliedCatalogItems "
          + "left join fetch e.links "
          + "where e.dbId = ?1")
  @Override
  @Nonnull
  Optional<T> findById(@Nonnull String id);

  @Query(
      "select e from #{#entityName} as e "
          + "left join fetch e.customAspects "
          + "left join fetch e.domains "
          + "left join fetch e.decisionResultsAspects "
          + "left join fetch e.subTypeAspects "
          + "left join fetch e.appliedCatalogItems "
          + "left join fetch e.links "
          + "where e.dbId = ?1 and e.owner.client.dbId = ?2")
  @Nonnull
  Optional<T> findById(@Nonnull String id, @Nonnull String clientId);

  @Nonnull
  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = {"domains", "links", "decisionResultsAspects"})
  List<T> findAllWithDomainsLinksDecisionsByDbIdIn(Iterable<String> ids);

  @Nonnull
  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = {"customAspects", "customAspects.domain"})
  List<T> findAllWithCustomAspectsByDbIdIn(Iterable<String> ids);

  @Nonnull
  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = "subTypeAspects")
  List<T> findAllWithSubtypeAspectsByDbIdIn(Iterable<String> ids);

  @Nonnull
  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = "appliedCatalogItems")
  List<T> findAllWithAppliedCatalogItemsByDbIdIn(Iterable<String> ids);

  @Override
  @Nonnull
  @Transactional(readOnly = true)
  // TODO use a projection to return only the IDs once
  // https://github.com/spring-projects/spring-data-jpa/issues/1378 is fixed
  Page<T> findAll(Specification<T> specification, Pageable pageable);

  @Query(
      "SELECT new org.veo.core.repository.SubTypeStatusCount(e.elementType, a.subType, a.status, count(a.status)) from #{#entityName} as e "
          + "inner join e.subTypeAspects a "
          + "where e.owner.dbId = ?1 "
          + "and a.domain.id = ?2 "
          + "group by e.elementType, a.subType, a.status")
  Set<SubTypeStatusCount> getCountsBySubType(String unitId, String domainId);

  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = {"scopes", "scopes.members"})
  List<T> findAllWithScopesAndScopeMembersByDbIdIn(List<String> ids);
}
