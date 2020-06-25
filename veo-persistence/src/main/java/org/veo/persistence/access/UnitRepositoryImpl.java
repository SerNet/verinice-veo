/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.repository.UnitRepository;
import org.veo.persistence.access.jpa.UnitDataRepository;
import org.veo.persistence.entity.jpa.ModelObjectValidation;
import org.veo.persistence.entity.jpa.UnitData;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;

@Repository
@AllArgsConstructor
public class UnitRepositoryImpl implements UnitRepository {

    // public Collection<UnitData> findByNameContainingIgnoreCase(String search);

    private UnitDataRepository dataRepository;

    private ModelObjectValidation validation;

    @Override
    public Unit save(Unit unit, TransformEntityToTargetContext entityToDataContext,
            TransformTargetToEntityContext dataToEntityContext) {
        validation.validateModelObject(unit);
        return dataRepository.save(UnitData.from(unit, Optional.ofNullable(entityToDataContext)
                                                               .orElseGet(DataEntityToTargetContext::getCompleteTransformationContext)))
                             .toUnit(Optional.ofNullable(dataToEntityContext)
                                             .orElseGet(DataTargetToEntityContext::getCompleteTransformationContext));
    }

    @Override
    public Optional<Unit> findById(Key<UUID> id) {
        return findById(id, null);
    }

    @Override
    public Optional<Unit> findById(Key<UUID> id,
            TransformTargetToEntityContext dataToEntityContext) {
        TransformTargetToEntityContext context = Optional.ofNullable(dataToEntityContext)
                                                         .orElseGet(DataTargetToEntityContext::getCompleteTransformationContext);

        boolean fetchClient = ((DataTargetToEntityContext) context).getUnitClientFunction() != null;
        Optional<UnitData> dataObject;
        if (fetchClient) {
            dataObject = dataRepository.findByIdFetchClient(id.uuidValue());
        } else {
            dataObject = dataRepository.findById(id.uuidValue());
        }
        return dataObject.map(data -> data.toUnit(context));

    }

    @Override
    public List<Unit> findByName(String search) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(Unit entity) {
        dataRepository.delete(UnitData.from(entity));
    }

    @Override
    public void deleteById(Key<UUID> id) {
        dataRepository.deleteById(id.uuidValue());
    }

    @Override
    public boolean exists(Key<UUID> id) {
        return dataRepository.existsById(id.uuidValue());
    }

    @Override
    public Set<Unit> getByIds(Set<Key<UUID>> ids) {
        // TODO Auto-generated method stub
        return null;
    }

}
