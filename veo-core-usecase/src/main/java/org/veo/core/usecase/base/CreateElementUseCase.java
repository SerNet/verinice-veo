/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.core.usecase.base;

import java.util.Set;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scope;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.state.ElementState;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.RetryableUseCase;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.decision.Decider;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.RefResolverFactory;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Deprecated
public class CreateElementUseCase<TEntity extends Element>
    implements TransactionalUseCase<
            CreateElementUseCase.InputData<TEntity>, CreateElementUseCase.OutputData<TEntity>>,
        RetryableUseCase {
  private final RefResolverFactory refResolverFactory;
  private final RepositoryProvider repositoryProvider;
  private final DesignatorService designatorService;
  private final EventPublisher eventPublisher;
  private final IdentifiableFactory identifiableFactory;
  private final EntityStateMapper entityStateMapper;
  private final Decider decider;

  @Override
  @Transactional(Transactional.TxType.REQUIRED)
  public CreateElementUseCase.OutputData<TEntity> execute(
      CreateElementUseCase.InputData<TEntity> input) {
    var state = input.newEntity;
    Class<TEntity> entityType = state.getModelInterface();
    var entity = identifiableFactory.create(entityType);
    entityStateMapper.mapState(
        state, entity, false, refResolverFactory.db(input.authenticatedClient));
    DomainSensitiveElementValidator.validate(entity);
    designatorService.assignDesignator(entity, input.authenticatedClient);
    addToScopes(entity, input.scopeIds, input.authenticatedClient);
    evaluateDecisions(entity);
    entity = repositoryProvider.getElementRepositoryFor(state.getModelInterface()).save(entity);
    if (Process.class.equals(entityType)
        || Asset.class.equals(entityType)
        || Scope.class.equals(entityType)) {
      eventPublisher.publish(new RiskAffectingElementChangeEvent(entity, this));
    }
    return new CreateElementUseCase.OutputData<>(entity);
  }

  private void addToScopes(TEntity element, Set<UUID> scopeIds, Client client) {
    repositoryProvider
        .getElementRepositoryFor(Scope.class)
        .findByIds(scopeIds)
        .forEach(
            scope -> {
              scope.checkSameClient(client);
              scope.addMember(element);
            });
  }

  private void evaluateDecisions(TEntity entity) {
    entity
        .getDomains()
        .forEach(domain -> entity.setDecisionResults(decider.decide(entity, domain), domain));
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public Isolation getIsolation() {
    return Isolation.SERIALIZABLE;
  }

  @Override
  public int getMaxAttempts() {
    return 5;
  }

  @Valid
  public record InputData<TEntity extends Element>(
      ElementState<TEntity> newEntity, Client authenticatedClient, Set<UUID> scopeIds)
      implements UseCase.InputData {}

  @Valid
  public record OutputData<TEntity>(@Valid TEntity entity) implements UseCase.OutputData {}
}
