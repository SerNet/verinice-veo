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

import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class SaveRiskDefinitionUseCase
    implements TransactionalUseCase<
        SaveRiskDefinitionUseCase.InputData, SaveRiskDefinitionUseCase.OutputData> {

  private final DomainRepository repository;

  @Override
  public OutputData execute(InputData input) {
    var domain = repository.getById(input.getDomainId(), input.getAuthenticatedClientId());
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    return new OutputData(
        domain.applyRiskDefinition(input.riskDefinitionRef, input.riskDefinition));
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Key<UUID> authenticatedClientId;
    Key<UUID> domainId;
    String riskDefinitionRef;
    RiskDefinition riskDefinition;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    boolean newRiskDefinition;
  }
}
