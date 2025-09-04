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

import org.veo.core.UserAccessRights;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.event.RiskChangedEvent;
import org.veo.core.entity.exception.CrossUnitReferenceException;
import org.veo.core.entity.ref.TypedId;
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
  public OutputData<R> execute(InputData input, UserAccessRights userAccessRights) {
    // Retrieve required elements for operation:
    var riskAffected =
        getEntity(TypedId.from(input.riskAffectedRef(), entityClass), userAccessRights);
    var client = riskAffected.getOwningClient().get();
    // make sure that we can load the scenario with the client's context
    getEntity(TypedId.from(input.scenarioRef(), Scenario.class), userAccessRights);
    var domains = findEntities(Domain.class, input.domainRefs());
    var mitigation =
        input
            .getControlRef()
            .map(id -> getEntity(TypedId.from(id, Control.class), userAccessRights));
    var riskOwner =
        input
            .getRiskOwnerRef()
            .map(id -> getEntity(TypedId.from(id, Person.class), userAccessRights));
    var risk = riskAffected.getRisk(input.scenarioRef()).orElseThrow();

    // Validate input:
    checkETag(risk, input);
    // Validate security constraints:
    userAccessRights.checkElementWriteAccess(riskAffected);
    checkDomainOwnership(client, domains);

    // Execute requested operation:
    R result =
        riskAffected.updateRisk(
            risk, mitigation.orElse(null), riskOwner.orElse(null), input.riskValues());
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
