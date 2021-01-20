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

import static java.util.Collections.singleton;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Control;
import org.veo.core.entity.Person;
import org.veo.core.entity.Scenario;
import org.veo.core.usecase.repository.AssetRepository;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.AssetData;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.ModelObjectValidation;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.ScenarioData;

@Repository
public class AssetRepositoryImpl extends AbstractCompositeEntityRepositoryImpl<Asset, AssetData>
        implements AssetRepository {

    private AssetDataRepository assetDataRepository;

    public AssetRepositoryImpl(AssetDataRepository dataRepository, ModelObjectValidation validation,
            CustomLinkDataRepository linkDataRepository, ScopeDataRepository scopeDataRepository) {
        super(dataRepository, validation, linkDataRepository, scopeDataRepository);
        this.assetDataRepository = dataRepository;
    }

    @Override
    public Set<Asset> findByRisk(Scenario cause) {
        return assetDataRepository.findDistinctByRisks_ScenarioIn(singleton((ScenarioData) cause))
                                  .stream()
                                  .map(assetData -> (Asset) assetData)
                                  .collect(Collectors.toSet());
    }

    @Override
    public Set<Asset> findByRisk(Control mitigatedBy) {
        return assetDataRepository.findDistinctByRisks_Mitigation_In(singleton((ControlData) mitigatedBy))
                                  .stream()
                                  .map(assetData -> (Asset) assetData)
                                  .collect(Collectors.toSet());
    }

    @Override
    public Set<Asset> findByRisk(Person riskOwner) {
        return assetDataRepository.findDistinctByRisks_RiskOwner_In(singleton((PersonData) riskOwner))
                                  .stream()
                                  .map(assetData -> (Asset) assetData)
                                  .collect(Collectors.toSet());
    }
}
