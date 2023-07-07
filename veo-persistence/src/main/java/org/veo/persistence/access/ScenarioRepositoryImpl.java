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
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.Scenario;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.ScenarioRepository;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.access.jpa.ScenarioDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class ScenarioRepositoryImpl
    extends AbstractCompositeEntityRepositoryImpl<Scenario, ScenarioData>
    implements ScenarioRepository {

  private final AssetDataRepository assetDataRepository;
  private final ProcessDataRepository processDataRepository;
  private final ScenarioDataRepository scenarioDataRepository;

  public ScenarioRepositoryImpl(
      ScenarioDataRepository dataRepository,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository,
      AssetDataRepository assetDataRepository,
      ProcessDataRepository processDataRepository,
      ElementQueryFactory elementQueryFactory) {
    super(
        dataRepository,
        validation,
        linkDataRepository,
        scopeDataRepository,
        elementQueryFactory,
        Scenario.class);
    this.scenarioDataRepository = dataRepository;
    this.assetDataRepository = assetDataRepository;
    this.processDataRepository = processDataRepository;
  }

  @Override
  public ElementQuery<Scenario> query(Client client) {
    return elementQueryFactory.queryScenarios(client);
  }

  @Override
  public void delete(Scenario scenario) {
    removeRisks(singleton((ScenarioData) scenario));
    super.deleteById(scenario.getId());
  }

  private void removeRisks(Set<ScenarioData> scenarios) {
    // remove risks associated with these scenarios:
    var assets = assetDataRepository.findDistinctByRisks_ScenarioIn(scenarios);
    assets.forEach(
        assetData ->
            scenarios.forEach(scenario -> assetData.getRisk(scenario).orElseThrow().remove()));

    var processes = processDataRepository.findRisksWithValue(scenarios);
    processes.forEach(
        processData ->
            scenarios.forEach(scenario -> processData.getRisk(scenario).orElseThrow().remove()));
  }

  @Override
  public void deleteById(Key<UUID> id) {
    delete(dataRepository.findById(id.uuidValue()).orElseThrow());
  }

  @Override
  @Transactional
  public void deleteAll(Set<Scenario> elements) {
    removeRisks(elements.stream().map(ScenarioData.class::cast).collect(Collectors.toSet()));
    super.deleteAll(elements);
  }

  @Override
  public Set<Scenario> findByDomainWhereRiskValuesExist(Domain domain) {
    return scenarioDataRepository.findByDomainWhereRiskValuesExist(domain).stream()
        .map(Scenario.class::cast)
        .collect(Collectors.toSet());
  }
}
