/*******************************************************************************
 * Copyright (c) 2021 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.access.jpa;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;

@NoRepositoryBean
public interface CompositeEntityDataRepository<T extends EntityLayerSupertypeData>
        extends EntityLayerSupertypeDataRepository<T> {

    //@formatter:off
    @Query("select distinct e from #{#entityName} as e "
            + "inner join e.parts p "
            + "where p.dbId IN ?1")
    //@formatter:on
    List<T> findDistinctByParts_DbId_In(Set<String> dbIds);

    //@formatter:off
    @Query("select e from #{#entityName} as e "
            + "left join fetch e.customAspects "
            + "left join fetch e.links "
            + "left join fetch e.subTypeAspects "
            + "left join fetch e.parts "
            + "left join fetch e.domains "
            + "where e.owner.dbId IN ?1")
    //@formatter:on
    @Transactional(readOnly = true)
    @Override
    Set<T> findByUnits(Set<String> unitIds);
}
