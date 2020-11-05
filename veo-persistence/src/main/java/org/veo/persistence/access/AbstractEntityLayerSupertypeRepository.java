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

import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.Unit;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;
import org.veo.persistence.access.jpa.EntityLayerSupertypeDataRepository;
import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.ModelObjectValidation;

abstract class AbstractEntityLayerSupertypeRepository<T extends EntityLayerSupertype, S extends EntityLayerSupertypeData>
        extends AbstractModelObjectRepository<T, S> implements EntityLayerSupertypeRepository<T> {

    protected final EntityLayerSupertypeDataRepository<S> dataRepository;

    public AbstractEntityLayerSupertypeRepository(
            EntityLayerSupertypeDataRepository<S> dataRepository,
            ModelObjectValidation validation) {
        super(dataRepository, validation);
        this.dataRepository = dataRepository;
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
    public List<T> findByUnits(Set<Unit> units) {
        var unitIdSet = units.stream()
                             .map(unit -> unit.getId()
                                              .uuidValue())
                             .collect(Collectors.toSet());
        return (List<T>) dataRepository.findEntitiesByUnits(unitIdSet);
    }

    @Override
    public List<ModelGroup<T>> findGroupsByUnits(Set<Unit> units) {
        var unitIdSet = units.stream()
                             .map(unit -> unit.getId()
                                              .uuidValue())
                             .collect(Collectors.toSet());
        return dataRepository.findGroupsByUnits(unitIdSet)
                             .stream()
                             .map(data -> (ModelGroup<T>) data)
                             .collect(Collectors.toList());
    }

    @Override
    public List<ModelGroup<T>> findGroupsByClient(Client client) {
        return dataRepository.findGroupsByOwner_Client_DbId(client.getDbId())
                             .stream()
                             .map(data -> (ModelGroup<T>) data)
                             .collect(Collectors.toList());
    }

    @Transactional
    public void deleteByUnit(Unit owner) {
        var entities = dataRepository.findByUnits(singleton(owner.getDbId()));
        // using deleteAll() to utilize batching and optimistic locking:
        dataRepository.deleteAll(entities);
    }

    public List<T> findByLinkTarget(EntityLayerSupertype entity) {
        return (List<T>) dataRepository.findByLinks_Target_DbId(entity.getId()
                                                                      .uuidValue())
                                       .stream()
                                       .collect(Collectors.toList());
    }

}
