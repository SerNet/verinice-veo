/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Ben Nasrallah.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.access;

import static java.util.Collections.singleton;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Key;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Unit;
import org.veo.core.usecase.repository.ScenarioRepository;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.access.jpa.ScenarioDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.ModelObjectValidation;
import org.veo.persistence.entity.jpa.ScenarioData;

@Repository
public class ScenarioRepositoryImpl
        extends AbstractCompositeEntityRepositoryImpl<Scenario, ScenarioData>
        implements ScenarioRepository {

    private final AssetDataRepository assetDataRepository;
    private final ProcessDataRepository processDataRepository;

    public ScenarioRepositoryImpl(ScenarioDataRepository dataRepository,
            ModelObjectValidation validation, CustomLinkDataRepository linkDataRepository,
            ScopeDataRepository scopeDataRepository, AssetDataRepository assetDataRepository,
            ProcessDataRepository processDataRepository) {
        super(dataRepository, validation, linkDataRepository, scopeDataRepository);
        this.assetDataRepository = assetDataRepository;
        this.processDataRepository = processDataRepository;
    }

    @Override
    public void delete(Scenario scenario) {
        removeRisks(singleton((ScenarioData) scenario));
        super.deleteById(scenario.getId());
    }

    private void removeRisks(Set<ScenarioData> scenarios) {
        // remove risks associated with these scenarios:
        var assets = assetDataRepository.findDistinctByRisks_ScenarioIn(scenarios);
        assets.forEach(assetData -> scenarios.forEach(scenario -> assetData.getRisk(scenario)
                                                                           .orElseThrow()
                                                                           .remove()));

        var processes = processDataRepository.findDistinctByRisks_ScenarioIn(scenarios);
        processes.forEach(processData -> scenarios.forEach(scenario -> processData.getRisk(scenario)
                                                                                  .orElseThrow()
                                                                                  .remove()));
    }

    @Override
    public void deleteById(Key<UUID> id) {
        delete(dataRepository.findById(id.uuidValue())
                             .orElseThrow());
    }

    @Override
    @Transactional
    public void deleteByUnit(Unit owner) {
        removeRisks(dataRepository.findByUnits(singleton(owner.getId()
                                                              .uuidValue())));
        super.deleteByUnit(owner);
    }
}