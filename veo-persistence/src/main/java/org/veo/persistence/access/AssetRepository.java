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
package org.veo.persistence.access;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Repository;
import org.veo.core.entity.Key;
import org.veo.core.entity.asset.Asset;
import org.veo.core.entity.asset.IAssetRepository;
import org.veo.persistence.access.jpa.JpaAssetDataRepository;
import org.veo.persistence.entity.jpa.AssetData;
import org.veo.persistence.entity.jpa.SimpleKey;

/**
 * An implementation of repository interface that converts between entities
 * and their JPA-annotated representations. 
 * 
 * @author akoderman
 *
 */
@Repository
public class AssetRepository implements IAssetRepository {

    private JpaAssetDataRepository jpaRepository;
    
    
    public AssetRepository(JpaAssetDataRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Asset save(Asset asset) {
        return jpaRepository
                .save(AssetData.from(asset))
                .toAsset();
    }

    @Override
    public Optional<Asset> findById(Key<UUID> id) {
        return jpaRepository
                    .findById(SimpleKey.from(id))
                    .map(AssetData::toAsset);
    }

    @Override
    public void delete(Asset asset) {
        jpaRepository.delete(AssetData.from(asset));
    }

    @Override
    public void deleteById(Key<UUID> id) {
        jpaRepository.deleteById(SimpleKey.from(id));
    }

    @Override
    public List<Asset> findByName(String search) {
        return jpaRepository
                .findByNameContainingIgnoreCase(search)
                .stream()
                .map(AssetData::toAsset)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Asset> getByIds(Set<Key<UUID>> ids) {
        Iterable<AssetData> allById = jpaRepository.findAllById(SimpleKey.from(ids));
        return StreamSupport
                .stream(allById.spliterator(), false)
                .map(AssetData::toAsset)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Asset> getByProcessId(Key<UUID> processId) {
        return jpaRepository
                .findByProcessId(SimpleKey.from(processId))
                .stream()
                .map(AssetData::toAsset)
                .collect(Collectors.toSet());
    }
}
