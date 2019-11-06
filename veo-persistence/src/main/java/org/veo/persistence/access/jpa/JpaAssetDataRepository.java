/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
 *
 * Contributors:
 *     Alexander Koderman <ak@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.persistence.access.jpa;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.veo.core.entity.Key;
import org.veo.persistence.entity.jpa.AssetData;

/**
 * A CRUD repository that will be implemented at runtime by magic and/or Spring Boot.
 * 
 */
public interface JpaAssetDataRepository extends CrudRepository<AssetData, Key<UUID>> {

    @Query("SELECT p.assets FROM Process p where p.id = :processId")
    public Set<AssetData> findByProcessId(Key<UUID> processId);
    
    Collection<AssetData> findByNameContainingIgnoreCase(String search);


}
