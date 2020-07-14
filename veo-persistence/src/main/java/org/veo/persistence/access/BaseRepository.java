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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.Unit;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;
import org.veo.persistence.access.jpa.EntityLayerSupertypeDataRepository;
import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.ModelObjectValidation;

@RequiredArgsConstructor
public abstract class BaseRepository<T extends EntityLayerSupertype, S extends EntityLayerSupertypeData>
        implements EntityLayerSupertypeRepository<T> {

    protected final EntityLayerSupertypeDataRepository<S> dataRepository;

    protected final ModelObjectValidation validation;

    @Override
    public T save(T entity) {
        validation.validateModelObject(entity);
        return (T) dataRepository.save((S) entity);
    }

    @Override
    public Optional<T> findById(Key<UUID> id) {
        return (Optional<T>) dataRepository.findById(id.uuidValue());
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
        List<S> list = Collections.emptyList();
        if (includeGroups) {
            list = dataRepository.findByOwner_Client_DbId(client.getId()
                                                                .uuidValue());
        } else {
            list = dataRepository.findEntitiesByOwner_Client_DbId(client.getId()
                                                                        .uuidValue());
        }
        return (List<T>) list;
    }

    @Override
    public List<T> findByUnit(Unit unit, boolean includeGroups) {
        List<S> list = Collections.emptyList();
        if (includeGroups) {
            list = dataRepository.findByOwner_DbId(unit.getId()
                                                       .uuidValue());
        } else {
            list = dataRepository.findEntitiesByOwner_DbId(unit.getId()
                                                               .uuidValue());
        }
        return (List<T>) list;
    }

    @Override
    public List<ModelGroup<T>> findGroupsByClient(Client client) {
        return dataRepository.findGroupsByOwner_Client_DbId(client.getDbId())
                             .stream()
                             .map(data -> (ModelGroup<T>) data)
                             .collect(Collectors.toList());
    }

    @Override
    public List<ModelGroup<T>> findGroupsByUnit(Unit unit) {
        return dataRepository.findGroupsByOwner_DbId(unit.getDbId())
                             .stream()
                             .map(data -> (ModelGroup<T>) data)
                             .collect(Collectors.toList());
    }

    public void deleteByUnit(Unit owner) {
        dataRepository.deleteByOwner_DbId(owner.getDbId());
    }

    public List<T> findByLinkTarget(EntityLayerSupertype entity) {
        return (List<T>) dataRepository.findByLinks_Target_DbId(entity.getId()
                                                                      .uuidValue())
                                       .stream()
                                       .collect(Collectors.toList());
    }

}
