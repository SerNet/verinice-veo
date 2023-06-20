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

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.ScopeRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class ScopeRepositoryImpl extends AbstractScopeRiskAffectedRepository
    implements ScopeRepository {

  private final ScopeDataRepository scopeDataRepository;

  public ScopeRepositoryImpl(
      ScopeDataRepository dataRepository,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository,
      ElementQueryFactory elementQueryFactory) {
    super(dataRepository, validation, linkDataRepository, elementQueryFactory, scopeDataRepository);
    this.scopeDataRepository = scopeDataRepository;
  }

  @Override
  public ElementQuery<Scope> query(Client client) {
    return elementQueryFactory.queryScopes(client);
  }

  @Override
  @Transactional(readOnly = true)
  public Set<Scope> findWithRisksAndScenarios(Set<Key<UUID>> ids) {
    List<String> dbIDs = ids.stream().map(Key::uuidValue).toList();
    var elements = scopeDataRepository.findWithRisksAndScenariosByDbIdIn(dbIDs);
    return Collections.unmodifiableSet(elements);
  }

  @Override
  public Optional<Scope> findByIdWithRiskValues(Key<UUID> processId) {
    var processes = scopeDataRepository.findByIdsWithRiskValues(singleton(processId.uuidValue()));
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
}
