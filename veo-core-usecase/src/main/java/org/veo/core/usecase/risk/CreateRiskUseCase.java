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

import static org.veo.core.entity.event.RiskEvent.ChangedValues.RISK_CREATED;

import jakarta.transaction.Transactional;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Domain;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.event.RiskChangedEvent;
import org.veo.core.entity.exception.CrossUnitReferenceException;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.specification.RiskOnlyReferencesItsOwnersUnitSpecification;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.DesignatorService;

public abstract class CreateRiskUseCase<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
    extends AbstractRiskUseCase<T, R> {

  private final DesignatorService designatorService;
  private final Class<T> entityClass;
  private final EventPublisher eventPublisher;

  public CreateRiskUseCase(
      Class<T> entityClass,
      RepositoryProvider repositoryProvider,
      DesignatorService designatorService,
      EventPublisher eventPublisher) {
    super(repositoryProvider);
    this.entityClass = entityClass;
    this.designatorService = designatorService;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  @Override
  public OutputData<R> execute(InputData input, UserAccessRights userAccessRights) {
    // Retrieve the necessary entities for the requested operation:
    var riskAffected =
        findElement(entityClass, input.riskAffectedRef(), userAccessRights)
            .orElseThrow(() -> new NotFoundException(input.riskAffectedRef(), entityClass));

    var scenario =
        findElement(Scenario.class, input.scenarioRef(), userAccessRights)
            .orElseThrow(() -> new NotFoundException(input.scenarioRef(), entityClass));

    var domains = findEntities(Domain.class, input.domainRefs());
    if (domains.size() != input.domainRefs().size()) {
      throw new UnprocessableDataException("Unable to resolve all domain references");
    }

    // Validate security constraints:
    userAccessRights.checkElementWriteAccess(riskAffected);

    // Apply requested operation:
    var risk = riskAffected.obtainRisk(scenario);
    boolean newRiskCreated = false;

    if (risk.getDesignator() == null || risk.getDesignator().isEmpty()) {
      designatorService.assignDesignator(risk, input.authenticatedClient());
      newRiskCreated = true;
    }

    risk = applyOptionalInput(input, risk, userAccessRights);

    risk.defineRiskValues(input.riskValues());
    publishEvents(riskAffected, risk);
    if (!new RiskOnlyReferencesItsOwnersUnitSpecification().test(risk)) {
      throw new CrossUnitReferenceException();
    }
    return new OutputData<>(risk, newRiskCreated);
  }

  private void publishEvents(T riskAffected, R risk) {
    var riskEvent = new RiskChangedEvent(risk, this);
    riskEvent.addChange(RISK_CREATED);
    eventPublisher.publish(new RiskAffectingElementChangeEvent(riskAffected, this, riskEvent));
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }
}
