/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.ProcessRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.ProcessData;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class ProcessRepositoryImpl
    extends AbstractCompositeRiskAffectedRepository<Process, ProcessRisk, ProcessData>
    implements ProcessRepository {

  private final ProcessDataRepository processDataRepository;

  public ProcessRepositoryImpl(
      ProcessDataRepository dataRepository,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository,
      ElementQueryFactory elementQueryFactory) {
    super(
        dataRepository,
        validation,
        linkDataRepository,
        scopeDataRepository,
        elementQueryFactory,
        Process.class);
    processDataRepository = dataRepository;
  }

  @Override
  public Set<Process> findRisksWithValue(Scenario scenario) {
    return new HashSet<>(
        ((ProcessDataRepository) dataRepository)
            .findRisksWithValue(singleton(((ScenarioData) scenario))));
  }

  @Override
  public Optional<Process> findByIdWithRiskValues(UUID processId) {
    var processes =
        ((ProcessDataRepository) dataRepository).findByIdsWithRiskValues(singleton(processId));
    return processes.stream().findFirst().map(Process.class::cast);
  }

  @Override
  @Transactional(readOnly = true)
  public Set<Process> findWithRisksAndScenarios(Set<UUID> ids) {
    var elements = processDataRepository.findWithRisksAndScenariosByIdIn(ids);
    return Collections.unmodifiableSet(elements);
  }

  @Override
  public ElementQuery<Process> query(Client client) {
    return elementQueryFactory.queryProcesses(client);
  }

  @Override
  public Optional<Process> findById(UUID id, boolean shouldEmbedRisks) {
    if (shouldEmbedRisks) {
      return this.findByIdWithRiskValues(id);
    } else {
      return this.findById(id);
    }
  }
}
