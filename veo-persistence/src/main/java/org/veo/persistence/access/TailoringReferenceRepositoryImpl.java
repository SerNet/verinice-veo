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
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.TailoringReference;
import org.veo.core.usecase.repository.TailoringReferenceRepository;
import org.veo.persistence.access.jpa.TailoringReferenceDataRepository;
import org.veo.persistence.entity.jpa.ModelObjectValidation;
import org.veo.persistence.entity.jpa.TailoringReferenceData;

@Repository
public class TailoringReferenceRepositoryImpl
        extends AbstractModelObjectRepository<TailoringReference, TailoringReferenceData>
        implements TailoringReferenceRepository {

    private TailoringReferenceDataRepository dataRepository;

    public TailoringReferenceRepositoryImpl(TailoringReferenceDataRepository dataRepository,
            ModelObjectValidation validator) {
        super(dataRepository, validator);
        this.dataRepository = dataRepository;

    }

    @Override
    public List<TailoringReference> findbyCatalogitem(CatalogItem catalogItem) {
        List<TailoringReferenceData> list = dataRepository.findByCatalogItem_DbId(catalogItem.getDbId());
        return list.stream()
                   .map(t -> (TailoringReference) t)
                   .collect(Collectors.toList());
    }
}
