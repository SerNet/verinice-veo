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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Key;
import org.veo.core.entity.asset.Asset;
import org.veo.core.entity.process.Process;
import org.veo.core.entity.process.ProcessRepository;
import org.veo.persistence.access.jpa.JpaProcessDataRepository;
import org.veo.persistence.entity.jpa.ProcessData;
import org.veo.persistence.entity.jpa.SimpleKey;

/**
 * An implementation of repository interface that converts between entities and
 * their JPA-annotated representations.
 *
 * @author akoderman
 *
 */
@Repository
public class ProcessRepositoryImpl implements ProcessRepository {

    private JpaProcessDataRepository jpaRepository;

    public ProcessRepositoryImpl(JpaProcessDataRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Process save(Process process) {
        return jpaRepository.save(ProcessData.from(process))
                            .toProcess();
    }

    @Override
    @EntityGraph(attributePaths = "assets")
    @Transactional(readOnly = true)
    public Optional<Process> findById(Key<UUID> id) {
        return jpaRepository.findById(SimpleKey.from(id))
                            .map(ProcessData::toProcess);
    }

    @Override
    @EntityGraph(attributePaths = "assets")
    public List<Process> findByName(String search) {
        return jpaRepository.findByNameContainingIgnoreCase(search)
                            .stream()
                            .map(ProcessData::toProcess)
                            .collect(Collectors.toList());
    }

    @Override
    public void delete(Process entity) {
        jpaRepository.delete(ProcessData.from(entity));
    }

    @Override
    public Set<Process> getProcessByResponsiblePerson(Key<UUID> personId) {
        // TODO Use query over aspect relation to find processes for the given person
        // entity
        return null;
    }

    @Override
    public void deleteById(Key<UUID> id) {
        jpaRepository.deleteById(SimpleKey.from(id));
    }

    @Override
    public Set<Process> findProcessesContainingAsset(Asset asset) {
        return jpaRepository.findDistinctByAssetsIn(new HashSet<Asset>(Arrays.asList(asset)));
    }

}
