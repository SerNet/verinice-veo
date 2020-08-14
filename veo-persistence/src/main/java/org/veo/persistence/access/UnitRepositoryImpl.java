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
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.usecase.repository.UnitRepository;
import org.veo.persistence.access.jpa.UnitDataRepository;
import org.veo.persistence.entity.jpa.ModelObjectValidation;
import org.veo.persistence.entity.jpa.UnitData;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class UnitRepositoryImpl implements UnitRepository {

    private UnitDataRepository dataRepository;

    private ModelObjectValidation validation;

    @Override
    public Unit save(Unit unit) {
        validation.validateModelObject(unit);
        return dataRepository.save((UnitData) unit);
    }

    @Override
    public Optional<Unit> findById(Key<UUID> id) {
        return (Optional) dataRepository.findById(id.uuidValue());
    }

    @Override
    public List<Unit> findByName(String search) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(Unit entity) {
        dataRepository.delete((UnitData) entity);
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

    @Override
    public List<Unit> findByClient(Client client) {
        return dataRepository.findByClient_DbId(client.getDbId())
                             .stream()
                             .map(e -> (Unit) e)
                             .collect(Collectors.toList());

    }

    @Override
    public List<Unit> findByParent(Unit parent) {
        return dataRepository.findByParent_DbId(parent.getDbId())
                             .stream()
                             .map(e -> (Unit) e)
                             .collect(Collectors.toList());
    }

}
