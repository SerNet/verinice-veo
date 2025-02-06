/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.usecase.domain;

import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.detectChanges;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.event.RiskDefinitionChangedEvent;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinitionChange;
import org.veo.core.repository.DomainRepository;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SaveRiskDefinitionUseCase
    implements TransactionalUseCase<
        SaveRiskDefinitionUseCase.InputData, SaveRiskDefinitionUseCase.OutputData> {

  private final DomainRepository repository;
  private final EventPublisher eventPublisher;

  @Override
  public OutputData execute(InputData input) {
    var domain = repository.getById(input.domainId, input.authenticatedClientId);
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    Set<RiskDefinitionChange> detectedChanges = new HashSet<>();
    domain
        .getRiskDefinition(input.riskDefinitionRef)
        .ifPresentOrElse(
            rd -> detectedChanges.addAll(detectChanges(rd, input.riskDefinition)),
            () -> detectedChanges.add(new RiskDefinitionChange.NewRiskDefinition()));

    if (detectedChanges.stream().anyMatch(c -> !input.allowedChanges.contains(c.getClass()))) {
      throw new UnprocessableDataException(
          "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: "
              + input.allowedChanges.stream().map(Class::getSimpleName).sorted().toList());
    }
    domain.applyRiskDefinition(input.riskDefinitionRef, input.riskDefinition);

    eventPublisher.publish(
        RiskDefinitionChangedEvent.from(domain, input.riskDefinition, detectedChanges, this));
    return new OutputData(detectedChanges.contains(new RiskDefinitionChange.NewRiskDefinition()));
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      UUID authenticatedClientId,
      UUID domainId,
      String riskDefinitionRef,
      RiskDefinition riskDefinition,
      Set<Class<? extends RiskDefinitionChange>> allowedChanges)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(boolean newRiskDefinition) implements UseCase.OutputData {}
}
