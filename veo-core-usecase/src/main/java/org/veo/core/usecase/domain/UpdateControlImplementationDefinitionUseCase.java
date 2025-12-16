/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Aziz Khalledi
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

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.definitions.ControlImplementationDefinition;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ElementTypeDefinitionValidator;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateControlImplementationDefinitionUseCase
    implements TransactionalUseCase<
        UpdateControlImplementationDefinitionUseCase.InputData, EmptyOutput> {

  private final DomainRepository repository;

  @Override
  public EmptyOutput execute(InputData input, UserAccessRights userAccessRights) {
    Domain domain =
        repository
            .findById(input.domainId, userAccessRights.getClientId())
            .orElseThrow(() -> new NotFoundException(input.domainId, Domain.class));

    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }

    var elementTypeDefinition = domain.getElementTypeDefinition(input.elementType);
    elementTypeDefinition.setControlImplementationDefinition(input.controlImplementationDefinition);
    ElementTypeDefinitionValidator.validate(elementTypeDefinition);
    domain.setUpdatedAt(Instant.now());

    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      UUID domainId,
      ElementType elementType,
      ControlImplementationDefinition controlImplementationDefinition)
      implements UseCase.InputData {}
}
