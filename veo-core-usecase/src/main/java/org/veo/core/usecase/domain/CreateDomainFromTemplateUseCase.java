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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.veo.core.entity.AccountProvider;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.specification.MissingAdminPrivilegesException;
import org.veo.core.repository.ClientRepository;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;

import lombok.RequiredArgsConstructor;
import lombok.Value;

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
    Collection<Client> clients;
    if (input.getClientIDs().isPresent()) {
      Set<Key<UUID>> clientIDs =
          input.getClientIDs().get().stream().map(Key::uuidFrom).collect(Collectors.toSet());
      clients = clientRepository.findByIds(clientIDs);
    } else {
      clients = clientRepository.findAll();
    }

    for (Client client : clients) {
      domainTemplateService.createDomain(client, input.domainTemplateId);
      clientRepository.save(client);
    }
    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    String domainTemplateId;
    Optional<List<String>> clientIDs;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid Domain domain;
  }
}
