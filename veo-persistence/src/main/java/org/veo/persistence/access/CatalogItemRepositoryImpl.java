/*******************************************************************************
 * Copyright (c) 2021 Urs Zeidler.
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
package org.veo.persistence.access;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Key;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.persistence.access.jpa.CatalogItemDataRepository;
import org.veo.persistence.entity.jpa.CatalogItemData;
import org.veo.persistence.entity.jpa.ModelObjectValidation;

@Repository
public class CatalogItemRepositoryImpl
        extends AbstractModelObjectRepository<CatalogItem, CatalogItemData>
        implements CatalogItemRepository {

    private final CatalogItemDataRepository dataRepository;

    public CatalogItemRepositoryImpl(CatalogItemDataRepository dataRepository,
            ModelObjectValidation validator) {
        super(dataRepository, validator);
        this.dataRepository = dataRepository;
    }

    @Override
    public List<CatalogItem> findByCatalog(Catalog catalog) {
        return dataRepository.findByCatalog_DbId(catalog.getDbId());
    }

    @Override
    public Set<CatalogItem> getByIds(Set<Key<UUID>> ids) {
        var idStrings = ids.stream()
                           .map(Key::uuidValue)
                           .collect(Collectors.toList());
        return StreamSupport.stream(dataRepository.findAllById(idStrings)
                                                  .spliterator(),
                                    false)
                            .map(e -> (CatalogItem) e)
                            .collect(Collectors.toSet());
    }
}
