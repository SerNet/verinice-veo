/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade
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

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.state.ElementTypeDefinitionState;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;
import org.veo.core.usecase.service.DomainStateMapper;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class UpdateElementTypeDefinitionUseCase
    implements TransactionalUseCase<UpdateElementTypeDefinitionUseCase.InputData, EmptyOutput> {

  private final DomainStateMapper domainStateMapper;
  private final DomainRepository repository;

  @Override
  public EmptyOutput execute(InputData input) {
    Domain domain =
        repository
            .findById(input.getDomainId())
            .orElseThrow(() -> new NotFoundException(input.getDomainId().uuidValue()));
    Client client = input.getAuthenticatedClient();
    if (!client.equals(domain.getOwner())) {
      throw new ClientBoundaryViolationException(domain, client);
    }
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    var elementTypeDefinition =
        domainStateMapper.toElementTypeDefinition(
            input.entityType.getSingularTerm(), input.elementTypeDefinition, domain);
    domain.applyElementTypeDefinition(elementTypeDefinition);
    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {

    @Valid Client authenticatedClient;

    Key<UUID> domainId;

    EntityType entityType;

    ElementTypeDefinitionState elementTypeDefinition;
  }
}
