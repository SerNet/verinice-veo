/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import java.util.List;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetDomainsUseCase
    implements TransactionalUseCase<GetDomainsUseCase.InputData, GetDomainsUseCase.OutputData> {
  private final DomainRepository domainRepository;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    return new OutputData(
        domainRepository
            .findActiveDomainsWithProfilesAndRiskDefinitions(input.authenticatedClient.getId())
            .stream()
            .toList());
  }

  @Valid
  public record InputData(Client authenticatedClient) implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid List<Domain> objects) implements UseCase.OutputData {}
}
