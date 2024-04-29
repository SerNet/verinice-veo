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

import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.event.RiskChangedEvent;
import org.veo.core.entity.event.RiskEvent;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

public class DeleteRiskUseCase
    implements TransactionalUseCase<DeleteRiskUseCase.InputData, UseCase.EmptyOutput> {

  private final RepositoryProvider repositoryProvider;
  private final EventPublisher eventPublisher;

  public DeleteRiskUseCase(RepositoryProvider repositoryProvider, EventPublisher eventPublisher) {
    this.repositoryProvider = repositoryProvider;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  @Override
  public EmptyOutput execute(InputData input) {
    var riskAffected =
        repositoryProvider
            .getRepositoryFor(input.entityClass)
            .findById(input.riskAffectedRef)
            .orElseThrow();

    riskAffected.checkSameClient(input.authenticatedClient);
    var risk = riskAffected.getRisk(input.scenarioRef).orElseThrow();
    risk.remove();
    publishEvents(riskAffected, risk);
    return EmptyOutput.INSTANCE;
  }

  private void publishEvents(Element element, AbstractRisk<?, ?> risk) {
    var riskEvent = new RiskChangedEvent(risk, this);
    riskEvent.addChange(RiskEvent.ChangedValues.RISK_DELETED);
    eventPublisher.publish(new RiskAffectingElementChangeEvent(element, this, riskEvent));
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      Class<? extends RiskAffected<?, ?>> entityClass,
      Client authenticatedClient,
      Key<UUID> riskAffectedRef,
      Key<UUID> scenarioRef)
      implements UseCase.InputData {}
}
