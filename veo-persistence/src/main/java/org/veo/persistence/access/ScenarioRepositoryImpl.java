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

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.Scenario;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.ScenarioRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.RiskAffectedDataRepository;
import org.veo.persistence.access.jpa.ScenarioDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.RiskAffectedData;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class ScenarioRepositoryImpl
    extends AbstractCompositeEntityRepositoryImpl<Scenario, ScenarioData>
    implements ScenarioRepository {

  private final RiskAffectedDataRepository<RiskAffectedData<?, ?>> riskAffectedDataRepository;
  private final ScenarioDataRepository scenarioDataRepository;

  public ScenarioRepositoryImpl(
      ScenarioDataRepository dataRepository,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository,
      RiskAffectedDataRepository<RiskAffectedData<?, ?>> riskAffectedDataRepository,
      ElementQueryFactory elementQueryFactory) {
    super(
        dataRepository,
        validation,
        linkDataRepository,
        scopeDataRepository,
        elementQueryFactory,
        Scenario.class);
    this.scenarioDataRepository = dataRepository;
    this.riskAffectedDataRepository = riskAffectedDataRepository;
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
    riskAffectedDataRepository
        .findDistinctByRisks_ScenarioIn(scenarios)
        .forEach(
            riskAffected -> {
              scenarios.forEach(
                  scenario -> riskAffected.getRisk(scenario).ifPresent(AbstractRisk::remove));
            });
  }

  @Override
  public void deleteById(UUID id) {
    delete(dataRepository.findById(id).orElseThrow());
  }

  @Override
  @Transactional
  public void deleteAll(Set<Scenario> elements) {
    removeRisks(elements.stream().map(ScenarioData.class::cast).collect(Collectors.toSet()));
    super.deleteAll(elements);
  }
}
