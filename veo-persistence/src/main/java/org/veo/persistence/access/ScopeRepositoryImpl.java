/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.RiskAffectedRepository;
import org.veo.core.repository.ScopeRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ScopeData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class ScopeRepositoryImpl extends AbstractElementRepository<Scope, ScopeData>
    implements ScopeRepository, RiskAffectedRepository<Scope, ScopeRisk> {

  private final ScopeDataRepository scopeDataRepository;

  public ScopeRepositoryImpl(
      ScopeDataRepository dataRepository,
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
        Scope.class);
    this.scopeDataRepository = scopeDataRepository;
  }

  @Override
  public ElementQuery<Scope> query(Client client) {
    return elementQueryFactory.queryScopes(client);
  }

  @Override
  @Transactional(readOnly = true)
  public Set<Scope> findWithRisksAndScenarios(Set<Key<UUID>> ids) {
    List<UUID> dbIDs = ids.stream().map(Key::value).toList();
    var elements = scopeDataRepository.findWithRisksAndScenariosByDbIdIn(dbIDs);
    return Collections.unmodifiableSet(elements);
  }

  @Override
  public Optional<Scope> findByIdWithRiskValues(Key<UUID> processId) {
    var processes = scopeDataRepository.findByIdsWithRiskValues(singleton(processId.value()));
    return processes.stream().findFirst().map(Scope.class::cast);
  }

  @Override
  public Set<Scope> findRisksWithValue(Scenario scenario) {
    return new HashSet<>(
        scopeDataRepository.findRisksWithValue(singleton(((ScenarioData) scenario))));
  }

  @Override
  public Optional<Scope> findById(Key<UUID> id, boolean shouldEmbedRisks) {
    if (shouldEmbedRisks) {
      return this.findByIdWithRiskValues(id);
    } else {
      return this.findById(id);
    }
  }

  @Override
  public Set<Scope> findByRisk(Scenario cause) {
    return scopeDataRepository
        .findDistinctByRisks_ScenarioIn(singleton((ScenarioData) cause))
        .stream()
        .map(Scope.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Scope> findByRisk(Control mitigatedBy) {
    return scopeDataRepository
        .findDistinctByRisks_Mitigation_In(singleton((ControlData) mitigatedBy))
        .stream()
        .map(Scope.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Scope> findByRisk(Person riskOwner) {
    return scopeDataRepository
        .findDistinctByRisks_RiskOwner_In(singleton((PersonData) riskOwner))
        .stream()
        .map(Scope.class::cast)
        .collect(Collectors.toSet());
  }
}
