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

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Scope;
import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.ScopeData;

public interface ScopeDataRepository extends EntityLayerSupertypeDataRepository<ScopeData> {

    <T extends EntityLayerSupertypeData> Set<Scope> findDistinctByMembersIn(Set<T> entities);

    @Query("select distinct e from #{#entityName} as e " + "inner join e.members m "
            + "where m.dbId IN ?1")
    Set<Scope> findDistinctByMemberIds(Set<String> dbIds);

    @Query("select e from #{#entityName} as e " + "left join fetch e.customAspects "
            + "left join fetch e.links " + "left join fetch e.subTypeAspects "
            + "left join fetch e.members " + "left join fetch e.domains "
            + "where e.owner.dbId IN ?1")
    @Transactional(readOnly = true)
    @Override
    Set<ScopeData> findByUnits(Set<String> unitIds);
}
