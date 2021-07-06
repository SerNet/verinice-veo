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

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.CompositeEntity;
import org.veo.core.entity.Control;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.repository.RiskAffectedRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.RiskAffectedDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.ModelObjectValidation;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.RiskAffectedData;
import org.veo.persistence.entity.jpa.ScenarioData;

abstract class AbstractRiskAffectedRepository<S extends CompositeEntity<S> & RiskAffected<S, R>, R extends AbstractRisk<S, R>, T extends RiskAffectedData<S, R> & CompositeEntity<S>>
        extends AbstractCompositeEntityRepositoryImpl<S, T>
        implements RiskAffectedRepository<S, R> {

    private final RiskAffectedDataRepository<T> riskAffectedRepo;

    AbstractRiskAffectedRepository(RiskAffectedDataRepository<T> riskAffectedRepo,
            ModelObjectValidation validation, CustomLinkDataRepository linkDataRepository,
            ScopeDataRepository scopeDataRepository) {
        super(riskAffectedRepo, validation, linkDataRepository, scopeDataRepository);
        this.riskAffectedRepo = riskAffectedRepo;
    }

    @Override
    public Set<S> findByRisk(Scenario cause) {
        return riskAffectedRepo.findDistinctByRisks_ScenarioIn(singleton((ScenarioData) cause))
                               .stream()
                               .map(riskAffectedData -> (S) riskAffectedData)
                               .collect(Collectors.toSet());
    }

    @Override
    public Set<S> findByRisk(Control mitigatedBy) {
        return riskAffectedRepo.findDistinctByRisks_Mitigation_In(singleton((ControlData) mitigatedBy))
                               .stream()
                               .map(riskAffectedData -> (S) riskAffectedData)
                               .collect(Collectors.toSet());
    }

    @Override
    public Set<S> findByRisk(Person riskOwner) {
        return riskAffectedRepo.findDistinctByRisks_RiskOwner_In(singleton((PersonData) riskOwner))
                               .stream()
                               .map(riskAffectedData -> (S) riskAffectedData)
                               .collect(Collectors.toSet());
    }
}
