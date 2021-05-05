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

import javax.annotation.Nonnull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;

public interface EntityLayerSupertypeDataRepository<T extends EntityLayerSupertypeData>
        extends JpaRepository<T, String>, JpaSpecificationExecutor<T> {

    @Query("select e from #{#entityName} as e " + "left join fetch e.customAspects "
            + "left join fetch e.domains " + "left join fetch e.subTypeAspects "
            + "left join fetch e.appliedCatalogItems " + "left join fetch e.links "
            + "where e.dbId = ?1")
    @Override
    @Nonnull
    Optional<T> findById(@Nonnull String id);

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    @EntityGraph(EntityLayerSupertypeData.FULL_AGGREGATE_GRAPH)
    List<T> findAllById(Iterable<String> ids);

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    // TODO use a projection to return only the IDs once
    // https://github.com/spring-projects/spring-data-jpa/issues/1378 is fixed
    Page<T> findAll(Specification<T> specification, Pageable pageable);

    /**
     * Find all entities of the repository's type in the given units. (This includes
     * composites.)
     *
     * @param unitIds
     *            a list of units' UUIDs
     */
    //@formatter:off
    @Query("select e from #{#entityName} as e "
            + "left join fetch e.customAspects "
            + "left join fetch e.links "
            + "left join fetch e.appliedCatalogItems "
            + "left join fetch e.subTypeAspects "
            + "where e.owner.dbId IN ?1")
    //@formatter:on
    @Transactional(readOnly = true)
    Set<T> findByUnits(Set<String> unitIds);

}
