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
import java.util.UUID;

import jakarta.annotation.Nonnull;

import org.springframework.context.annotation.Primary;
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

@Primary
public interface ElementDataRepository<T extends ElementData>
    extends JpaRepository<T, UUID>,
        JpaSpecificationExecutor<T>,
        IdentifiableVersionedDataRepository<T> {

  @Query(
      "select e from #{#entityName} as e "
          + "left join fetch e.customAspects "
          + "left join fetch e.decisionResultsAspects "
          + "left join fetch e.domainAssociations da "
          + "left join fetch da.domain "
          + "left join fetch e.links "
          + "where e.id = ?1")
  @Override
  @Nonnull
  Optional<T> findById(@Nonnull UUID id);

  @Query(
      "select e from #{#entityName} as e "
          + "left join fetch e.customAspects "
          + "left join fetch e.decisionResultsAspects "
          + "left join fetch e.domainAssociations da "
          + "left join fetch da.domain "
          + "left join fetch e.links "
          + "where e.id = ?1 and e.owner.client.id = ?2 and  (?3 = false or e.owner.id in ?4)")
  @Nonnull
  Optional<T> findById(
      @Nonnull UUID id, @Nonnull UUID clientId, boolean restrictUnitAccess, Set<UUID> allowedUnits);

  @Query(
      "select e from #{#entityName} as e "
          + "left join fetch e.customAspects "
          + "left join fetch e.decisionResultsAspects "
          + "left join fetch e.domainAssociations da "
          + "left join fetch da.domain "
          + "left join fetch e.links "
          + "where e.id in ?1 and e.owner.client.id = ?2 and  (?3 = false or e.owner.id in ?4)")
  @Nonnull
  Set<T> findByIds(
      @Nonnull Set<UUID> ids,
      @Nonnull UUID clientId,
      boolean restrictUnitAccess,
      Set<UUID> allowedUnits);

  @Query(
      "select e from #{#entityName} as e "
          + "left join fetch e.customAspects "
          + "left join fetch e.decisionResultsAspects "
          + "left join fetch e.domainAssociations da "
          + "left join fetch da.domain "
          + "left join fetch e.links "
          + "where e.id = ?1 and e.owner.client.id = ?2")
  @Nonnull
  Optional<T> findById(@Nonnull UUID id, @Nonnull UUID clientId);

  @Nonnull
  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = {"links", "decisionResultsAspects"})
  List<T> findAllWithLinksDecisionsByIdIn(Iterable<UUID> ids);

  @Nonnull
  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = {"customAspects", "customAspects.domain"})
  List<T> findAllWithCustomAspectsByIdIn(Iterable<UUID> ids);

  @Nonnull
  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = {"domainAssociations", "domainAssociations.domain"})
  List<T> findAllWithDomainAssociationsByIdIn(Iterable<UUID> ids);

  @Nonnull
  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = "domainAssociations.appliedCatalogItem")
  List<T> findAllWithAppliedCatalogItemsByIdIn(Iterable<UUID> ids);

  @Override
  @Nonnull
  @Transactional(readOnly = true)
  Page<T> findAll(Specification<T> specification, Pageable pageable);

  @Query(
      "SELECT new org.veo.core.repository.SubTypeStatusCount(e.elementType, a.subType, a.status, count(a.status)) from #{#entityName} as e "
          + "inner join e.domainAssociations a "
          + "where e.owner.id = ?1 "
          + "and a.domain.id = ?2 "
          + "group by e.elementType, a.subType, a.status")
  Set<SubTypeStatusCount> getCountsBySubType(UUID unitId, UUID uuid);

  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = {"scopes", "scopes.members"})
  List<T> findAllWithScopesAndScopeMembersByIdIn(List<UUID> ids);
}
