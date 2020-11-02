/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.groups.EntityLayerSupertypeGroupData;

public interface EntityLayerSupertypeDataRepository<T extends EntityLayerSupertypeData>
        extends CrudRepository<T, String> {

    Collection<T> findByNameContainingIgnoreCase(String search);

    List<T> findByOwner_Client_DbId(String clientId); // NOPMD

    @Query("select e from #{#entityName} as e " + "left join fetch e.customAspects "
            + "left join fetch e.links " + "where e.dbId = ?1")
    @Override
    Optional<T> findById(String id);

    @Query("select e from #{#entityName} as e where e.owner.dbId IN ?1  and type(e) = #{#entityName}")
    List<T> findByUnits(Set<String> unitIds);

    @Query("select e from #{#entityName} as e where e.owner.dbId IN ?1 and type(e) != #{#entityName}")
    List<? extends EntityLayerSupertypeGroupData<T>> findGroupsByUnits(Set<String> unitIds);

    @Query("select e from #{#entityName} as e where e.owner.dbId = ?1 and type(e) = #{#entityName}")
    @Deprecated
    // FIXME this method should be removed, it was only added because
    // findByOwnerId
    // also returns groups, which it shouldn't. We probably need to fix our JPA
    // mapping in that regard
    List<T> findEntitiesByOwner_DbId(String ownerId);

    @Query("select e from #{#entityName} as e where e.owner.client.dbId = ?1 and type(e) = #{#entityName}")
    @Deprecated
    // FIXME this method should be removed, it was only added because
    // findByOwnerId
    // also returns groups, which it shouldn't. We probably need to fix our JPA
    // mapping in that regard
    List<T> findEntitiesByOwner_Client_DbId(String uuidValue);

    List<? extends EntityLayerSupertypeGroupData<T>> findGroupsByOwner_Client_DbId(
            String uuidValue);

    List<T> findByLinks_Target_DbId(String uuidValue);

    void deleteByOwner_DbId(String uuidValue);
}
