/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.usecase.profile;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Client;
import org.veo.core.entity.IncarnationConfiguration;
import org.veo.core.entity.Key;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class GetIncarnationConfigurationUseCase
    implements TransactionalUseCase<
        GetIncarnationConfigurationUseCase.InputData,
        GetIncarnationConfigurationUseCase.OutputData> {

  private final DomainRepository domainRepository;

  @Override
  public OutputData execute(InputData input) {
    var domain = domainRepository.getActiveById(input.domainId, input.authenticatedClient.getId());
    return new OutputData(domain.getIncarnationConfiguration());
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    @NotNull Client authenticatedClient;
    @NotNull Key<UUID> domainId;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid IncarnationConfiguration incarnationConfiguration;
  }
}
