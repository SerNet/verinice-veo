/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.Value;

public class GetRiskUseCase<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
    implements TransactionalUseCase<GetRiskUseCase.InputData, GetRiskUseCase.OutputData<R>> {

  private final Class<T> entityClass;
  private final RepositoryProvider repositoryProvider;

  public GetRiskUseCase(RepositoryProvider repositoryProvider, Class<T> entityClass) {
    this.entityClass = entityClass;
    this.repositoryProvider = repositoryProvider;
  }

  @Transactional
  public OutputData<R> execute(InputData input) {
    var entity =
        repositoryProvider
            .getRepositoryFor(entityClass)
            .findById(input.riskAffectedId)
            .orElseThrow(() -> new NotFoundException(input.riskAffectedId, entityClass));
    var scenario =
        repositoryProvider
            .getRepositoryFor(Scenario.class)
            .findById(input.scenarioId)
            .orElseThrow(() -> new NotFoundException(input.scenarioId, Scenario.class));

    entity.checkSameClient(input.authenticatedClient);
    scenario.checkSameClient(input.authenticatedClient);

    return new OutputData<>(
        entity
            .getRisk(scenario)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "No risk found for entity %s and scenario %s",
                        entity.getDisplayName(), scenario.getDisplayName())));
  }

  @Valid
  public record InputData(Client authenticatedClient, UUID riskAffectedId, UUID scenarioId)
      implements UseCase.InputData {}

  @Valid
  @Value
  public static class OutputData<R> implements UseCase.OutputData {
    @Valid R risk;
  }
}
