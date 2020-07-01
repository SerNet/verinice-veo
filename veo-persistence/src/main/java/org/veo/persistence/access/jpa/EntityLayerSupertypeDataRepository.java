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

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import org.veo.persistence.entity.jpa.AssetData;
import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.groups.EntityLayerSupertypeGroupData;

public interface EntityLayerSupertypeDataRepository<T extends EntityLayerSupertypeData>
        extends CrudRepository<T, String> {

    Collection<AssetData> findByNameContainingIgnoreCase(String search);

    List<T> findByOwnerId(String ownerId);

    List<T> findByOwner_ClientId(String clientId); // NOPMD

    @Query("select e from #{#entityName} as e where e.owner.id = ?1 and type(e) = #{#entityName}")
    @Deprecated
    // FIXME this method should be removed, it was only added because
    // findByOwnerId also returns groups, which it shouldn't. We probably need
    // to fix our JPA mapping in that regard
    List<T> findEntitiesByOwnerId(String ownerId);

    @Query("select e from #{#entityName} as e where e.owner.client.id = ?1 and type(e) = #{#entityName}")
    @Deprecated
    // FIXME this method should be removed, it was only added because
    // findByOwnerId also returns groups, which it shouldn't. We probably need
    // to fix our JPA mapping in that regard
    List<T> findEntitiesByOwner_ClientId(String uuidValue);

    List<T> findByLinks_TargetId(String uuidValue);

    List<? extends EntityLayerSupertypeGroupData<T>> findGroupsByOwner_ClientId(String uuidValue);

    List<? extends EntityLayerSupertypeGroupData<T>> findGroupsByOwnerId(String uuidValue);

    void deleteByOwnerId(String uuidValue);

}
