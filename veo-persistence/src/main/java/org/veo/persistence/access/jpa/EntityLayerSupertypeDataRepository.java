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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.groups.EntityLayerSupertypeGroupData;

public interface EntityLayerSupertypeDataRepository<T extends EntityLayerSupertypeData>
        extends JpaRepository<T, String> {

    Collection<T> findByNameContainingIgnoreCase(String search);

    List<T> findByOwner_Client_DbId(String clientId); // NOPMD

    @Query("select e from #{#entityName} as e " + "left join fetch e.customAspects "
            + "left join fetch e.links " + "where e.dbId = ?1")
    @Override
    Optional<T> findById(String id);

    /**
     * Find all entities of the repository's type in the given units. This includes
     * normal and group types, i.e. 'Person' as well as 'PersonGroup' instances.
     *
     * @param unitIds
     *            a list of units' UUIDs
     */
    @Query("select e from #{#entityName} as e " + "left join fetch e.customAspects "
            + "left join fetch e.links " + "where e.owner.dbId IN ?1")
    @Transactional(readOnly = true)
    Set<T> findByUnits(Set<String> unitIds);

    /**
     * Find all entities of the repository's type in the given units. Groups will
     * not be included in the result. I.e. 'Person' will be returned, but not
     * 'PersonGroup' instances.
     *
     * @param unitIds
     *            a list of units' UUIDs
     */
    @Query("select e from #{#entityName} as e " + "left join fetch e.customAspects "
            + "left join fetch e.links " + "where e.owner.dbId IN ?1  and type(e) = #{#entityName}")
    @Transactional(readOnly = true)
    // Using Set as return type here makes it unnecessary to add the DISTINCT
    // keyword to the query
    // which saves an additional hash aggregate operation in the database.
    Set<T> findEntitiesByUnits(Set<String> unitIds);

    /**
     * Find all entity groups of the repository's type in the given units. Only
     * groups will be included in the result. I.e. 'Person' will not be returned,
     * but only 'PersonGroup' instances.
     *
     * @param unitIds
     *            a list of units' UUIDs
     */
    @Query("select e from #{#entityName} as e " + "left join fetch e.customAspects "
            + "left join fetch e.links "
            // + "left join fetch e.members "
            + "where e.owner.dbId IN ?1 and type(e) != #{#entityName}")
    @Transactional(readOnly = true)
    Set<? extends EntityLayerSupertypeGroupData<T>> findGroupsByUnits(Set<String> unitIds);

    /**
     * Find only entities of the repository's type for a client. This method does
     * not return groups of the specific type. I.e. it will return all 'Person'
     * instances that are not a 'PersonGroup'.
     *
     * @param clientId
     *            The UUID of the client
     */
    @Query("select e from #{#entityName} as e where e.owner.client.dbId = ?1 and type(e)"
            + " = #{#entityName}")
    @Transactional(readOnly = true)
    // Using Set as return type here makes it unnecessary to add the DISTINCT
    // keyword to the query
    // which saves an additional hash aggregate operation in the database.
    Set<T> findEntitiesByOwner_Client_DbId(String clientId);

    /**
     * Find all groups of the repository's type for a client.
     *
     * @param clientId
     *            the UUID of the client
     */
    @Transactional(readOnly = true)
    Set<? extends EntityLayerSupertypeGroupData<T>> findGroupsByOwner_Client_DbId(String clientId);

    /**
     * Returns all entities of the repository's type that have a link with the given
     * UUID as target.
     *
     * @param uuidValue
     *            the UUID value of the targetted entity
     */
    @Transactional(readOnly = true)
    List<T> findByLinks_Target_DbId(String uuidValue);

}
