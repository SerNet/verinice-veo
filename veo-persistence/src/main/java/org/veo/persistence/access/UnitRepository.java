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
 ******************************************************************************/
package org.veo.persistence.access;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.IUnitRepository;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.persistence.access.jpa.JpaUnitDataRepository;
import org.veo.persistence.entity.jpa.SimpleKey;
import org.veo.persistence.entity.jpa.UnitData;

/**
 * An implementation of repository interface that converts between entities and
 * their JPA-annotated representations.
 *
 * @author akoderman
 *
 */
@Repository
public class UnitRepository implements IUnitRepository {

    private JpaUnitDataRepository jpaRepository;

    public UnitRepository(JpaUnitDataRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Unit save(Unit entity) {
        return jpaRepository.save(UnitData.from(entity))
                            .toUnit();
    }

    @Override
    public Optional<Unit> findById(Key<UUID> id) {
        return jpaRepository.findById(SimpleKey.from(id))
                            .map(UnitData::toUnit);
    }

    @Override
    public List<Unit> findByName(String search) {
        return jpaRepository.findByNameContainingIgnoreCase(search)
                            .stream()
                            .map(UnitData::toUnit)
                            .collect(Collectors.toList());
    }

    @Override
    public void delete(Unit entity) {
        jpaRepository.delete(UnitData.from(entity));
    }

    @Override
    public void deleteById(Key<UUID> id) {
        jpaRepository.deleteById(SimpleKey.from(id));
    }

}
