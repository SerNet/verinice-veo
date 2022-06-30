/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
import java.util.stream.Collectors;

import org.veo.core.entity.Control;
import org.veo.core.entity.Person;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.repository.RiskAffectedRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.jpa.ScopeRiskAffectedDataRepository;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ScopeData;
import org.veo.persistence.entity.jpa.ValidationService;

abstract class AbstractScopeRiskAffectedRepository
    extends AbstractElementRepository<Scope, ScopeData>
    implements RiskAffectedRepository<Scope, ScopeRisk> {

  private final ScopeRiskAffectedDataRepository riskAffectedRepo;

  AbstractScopeRiskAffectedRepository(
      ScopeRiskAffectedDataRepository riskAffectedRepo,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository) {
    super(riskAffectedRepo, validation, linkDataRepository, scopeDataRepository);
    this.riskAffectedRepo = riskAffectedRepo;
  }

  @Override
  public Set<Scope> findByRisk(Scenario cause) {
    return riskAffectedRepo.findDistinctByRisks_ScenarioIn(singleton((ScenarioData) cause)).stream()
        .map(Scope.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Scope> findByRisk(Control mitigatedBy) {
    return riskAffectedRepo
        .findDistinctByRisks_Mitigation_In(singleton((ControlData) mitigatedBy))
        .stream()
        .map(Scope.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Scope> findByRisk(Person riskOwner) {
    return riskAffectedRepo
        .findDistinctByRisks_RiskOwner_In(singleton((PersonData) riskOwner))
        .stream()
        .map(Scope.class::cast)
        .collect(Collectors.toSet());
  }
}
