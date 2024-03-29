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

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Domain;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.event.RiskChangedEvent;
import org.veo.core.entity.exception.UnprocessableDataException;
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
  public OutputData<R> execute(InputData input) {
    boolean newRiskCreated = false;
    // Retrieve the necessary entities for the requested operation:
    var riskAffected = findEntity(entityClass, input.getRiskAffectedRef()).orElseThrow();

    var scenario = findEntity(Scenario.class, input.getScenarioRef()).orElseThrow();

    var domains = findEntities(Domain.class, input.getDomainRefs());
    if (domains.size() != input.getDomainRefs().size()) {
      throw new UnprocessableDataException("Unable to resolve all domain references");
    }

    // Validate security constraints:
    riskAffected.checkSameClient(input.getAuthenticatedClient());
    scenario.checkSameClient(input.getAuthenticatedClient());
    checkDomainOwnership(input.getAuthenticatedClient(), domains);

    // Apply requested operation:
    var risk = riskAffected.obtainRisk(scenario, domains);
    if (risk.getDesignator() == null || risk.getDesignator().isEmpty()) {
      designatorService.assignDesignator(risk, input.getAuthenticatedClient());
      newRiskCreated = true;
    }

    risk = applyOptionalInput(input, risk);

    risk.defineRiskValues(input.getRiskValues());
    publishEvents(riskAffected, risk);
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
