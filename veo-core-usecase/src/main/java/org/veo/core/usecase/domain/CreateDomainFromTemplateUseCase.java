/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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

import jakarta.validation.Valid;

import org.veo.core.entity.AccountProvider;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.specification.MissingAdminPrivilegesException;
import org.veo.core.repository.ClientRepository;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateDomainFromTemplateUseCase
    implements TransactionalUseCase<CreateDomainFromTemplateUseCase.InputData, EmptyOutput> {

  private final AccountProvider accountProvider;
  private final ClientRepository clientRepository;
  private final DomainTemplateService domainTemplateService;

  @Override
  public EmptyOutput execute(InputData input) {
    if (!accountProvider.getCurrentUserAccount().isAdmin()) {
      throw new MissingAdminPrivilegesException();
    }
    var client = clientRepository.getActiveById(Key.uuidFrom(input.clientId));
    domainTemplateService.createDomain(client, input.domainTemplateId);
    clientRepository.save(client);
    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(String domainTemplateId, String clientId) implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid Domain domain) implements UseCase.OutputData {}
}
