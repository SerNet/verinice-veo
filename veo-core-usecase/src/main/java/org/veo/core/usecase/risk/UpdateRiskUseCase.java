/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.core.usecase.risk;

import jakarta.transaction.Transactional;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.event.RiskChangedEvent;
import org.veo.core.entity.exception.CrossUnitReferenceException;
import org.veo.core.entity.specification.RiskOnlyReferencesItsOwnersUnitSpecification;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.common.ETagMismatchException;

public abstract class UpdateRiskUseCase<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
    extends AbstractRiskUseCase<T, R> {

  private final Class<T> entityClass;
  private final EventPublisher eventPublisher;

  public UpdateRiskUseCase(
      RepositoryProvider repositoryProvider, Class<T> entityClass, EventPublisher eventPublisher) {
    super(repositoryProvider);
    this.entityClass = entityClass;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  @Override
  public OutputData<R> execute(InputData input) {
    // Retrieve required elements for operation:
    var riskAffected = findEntity(entityClass, input.riskAffectedRef()).orElseThrow();

    var scenario = findEntity(Scenario.class, input.scenarioRef()).orElseThrow();

    var domains = findEntities(Domain.class, input.domainRefs());

    var mitigation = input.getControlRef().flatMap(id -> findEntity(Control.class, id));

    var riskOwner = input.getRiskOwnerRef().flatMap(id -> findEntity(Person.class, id));

    var risk = riskAffected.getRisk(input.scenarioRef()).orElseThrow();

    // Validate input:
    checkETag(risk, input);
    riskAffected.checkSameClient(input.authenticatedClient());
    scenario.checkSameClient(input.authenticatedClient());
    checkDomainOwnership(input.authenticatedClient(), domains);
    mitigation.ifPresent(control -> control.checkSameClient(input.authenticatedClient()));
    riskOwner.ifPresent(person -> person.checkSameClient(input.authenticatedClient()));

    // Execute requested operation:
    R result =
        riskAffected.updateRisk(
            risk, domains, mitigation.orElse(null), riskOwner.orElse(null), input.riskValues());
    if (!new RiskOnlyReferencesItsOwnersUnitSpecification().test(risk)) {
      throw new CrossUnitReferenceException();
    }
    publishEvents(riskAffected, result);
    return new OutputData<>(result);
  }

  private void publishEvents(T riskAffected, R risk) {
    RiskChangedEvent riskEvent = new RiskChangedEvent(risk, this);
    eventPublisher.publish(riskEvent);
    eventPublisher.publish(new RiskAffectingElementChangeEvent(riskAffected, this, riskEvent));
  }

  private void checkETag(AbstractRisk<T, R> risk, InputData input) {
    var riskAffectedId = risk.getEntity().getIdAsString();
    var scenarioId = risk.getScenario().getIdAsString();
    if (!ETag.matches(riskAffectedId, scenarioId, risk.getVersion(), input.eTag())) {
      throw new ETagMismatchException(
          String.format(
              "The eTag does not match for the element with the ID %s_%s",
              riskAffectedId, scenarioId));
    }
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }
}
