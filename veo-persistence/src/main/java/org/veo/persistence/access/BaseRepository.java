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
package org.veo.persistence.access;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.impl.BaseModelGroup;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;
import org.veo.persistence.access.jpa.EntityLayerSupertypeDataRepository;
import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.ModelObjectValidation;
import org.veo.persistence.entity.jpa.groups.EntityLayerSupertypeGroupData;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;

@RequiredArgsConstructor
public abstract class BaseRepository<T extends EntityLayerSupertype, S extends EntityLayerSupertypeData>
        implements EntityLayerSupertypeRepository<T> {

    protected final EntityLayerSupertypeDataRepository<S> dataRepository;

    protected final ModelObjectValidation validation;

    private final BiFunction<T, TransformEntityToTargetContext, S> entityToDataMapper;
    private final BiFunction<S, TransformTargetToEntityContext, T> dataToEntityMapper;
    private final Function<EntityLayerSupertypeGroupData<S>, BaseModelGroup<T>> dataToGroupMapper;

    // public Collection<S> findByNameContainingIgnoreCase(String search);

    @Override
    public Optional<T> findById(Key<UUID> id) {
        return findById(id, null);
    }

    @Override
    public T save(T entity, TransformEntityToTargetContext entityToDataContext,
            TransformTargetToEntityContext dataToEntityContext) {
        validation.validateModelObject(entity);
        return dataToEntityMapper.apply(dataRepository.save(entityToDataMapper.apply(entity,
                                                                                     Optional.ofNullable(entityToDataContext)
                                                                                             .orElseGet(DataEntityToTargetContext::getCompleteTransformationContext))),
                                        Optional.ofNullable(dataToEntityContext)
                                                .orElseGet(DataTargetToEntityContext::getCompleteTransformationContext));
    }

    @Override
    public Optional<T> findById(Key<UUID> id, TransformTargetToEntityContext dataToEntityContext) {
        TransformTargetToEntityContext context = Optional.ofNullable(dataToEntityContext)
                                                         .orElseGet(DataTargetToEntityContext::getCompleteTransformationContext);

        return dataRepository.findById(id.uuidValue())
                             .map(data -> dataToEntityMapper.apply(data, context));

    }

    @Override
    public List<T> findByName(String search) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(T entity) {
        deleteById(entity.getId());
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
    public Set<T> getByIds(Set<Key<UUID>> ids) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<T> findByClient(Client client, boolean includeGroups) {
        List<S> list;
        if (includeGroups) {
            list = dataRepository.findByOwner_ClientId(client.getId()
                                                             .uuidValue());
        } else {
            list = dataRepository.findEntitiesByOwner_ClientId(client.getId()
                                                                     .uuidValue());
        }
        return list.stream()
                   .map(data -> dataToEntityMapper.apply(data,
                                                         DataTargetToEntityContext.getCompleteTransformationContext()))
                   .collect(Collectors.toList());
    }

    @Override
    public List<T> findByUnit(Unit unit, boolean includeGroups) {
        List<S> list;
        if (includeGroups) {
            list = dataRepository.findByOwnerId(unit.getId()
                                                    .uuidValue());
        } else {
            list = dataRepository.findEntitiesByOwnerId(unit.getId()
                                                            .uuidValue());
        }
        return list.stream()
                   .map(data -> dataToEntityMapper.apply(data,
                                                         DataTargetToEntityContext.getCompleteTransformationContext()))
                   .collect(Collectors.toList());
    }

    public List<T> findByLinkTarget(EntityLayerSupertype entity) {
        TransformTargetToEntityContext context = DataTargetToEntityContext.getCompleteTransformationContext();
        return dataRepository.findByLinks_TargetId(entity.getId()
                                                         .uuidValue())
                             .stream()
                             .map(data -> dataToEntityMapper.apply(data, context))
                             .collect(Collectors.toList());
    }

    @Override
    public List<BaseModelGroup<T>> findGroupsByClient(Client client) {
        return dataRepository.findGroupsByOwner_ClientId(client.getId()
                                                               .uuidValue())
                             .stream()
                             .map(data -> dataToGroupMapper.apply(data))
                             .collect(Collectors.toList());
    }

    @Override
    public List<BaseModelGroup<T>> findGroupsByUnit(Unit unit) {
        return dataRepository.findGroupsByOwnerId(unit.getId()
                                                      .uuidValue())
                             .stream()
                             .map(data -> dataToGroupMapper.apply(data))
                             .collect(Collectors.toList());
    }

    public void deleteByUnit(Unit owner) {
        dataRepository.deleteByOwnerId(owner.getId()
                                            .uuidValue());
    }

}
