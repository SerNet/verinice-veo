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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.state.ElementTypeDefinitionState;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;
import org.veo.core.usecase.service.DomainStateMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateElementTypeDefinitionUseCase
    implements TransactionalUseCase<UpdateElementTypeDefinitionUseCase.InputData, EmptyOutput> {

  private final DomainStateMapper domainStateMapper;
  private final DomainRepository repository;

  @Override
  public EmptyOutput execute(InputData input) {
    Domain domain =
        repository
            .findById(input.domainId)
            .orElseThrow(() -> new NotFoundException(input.domainId, Domain.class));
    Client client = input.authenticatedClient;
    if (!client.equals(domain.getOwner())) {
      throw new ClientBoundaryViolationException(domain, client);
    }
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    var elementTypeDefinition =
        domainStateMapper.toElementTypeDefinition(
            input.elementType.getSingularTerm(), input.elementTypeDefinition, domain);

    // TODO #3042: remove this when we remove support for JSON schema
    if (input.preserveSortKeys) {
      elementTypeDefinition
          .getSubTypes()
          .forEach(
              (subTypeId, subTypeDefinition) ->
                  domain
                      .findElementTypeDefinition(input.elementType.getSingularTerm())
                      .ifPresent(
                          existingDefinition ->
                              Optional.ofNullable(existingDefinition.getSubTypes().get(subTypeId))
                                  .ifPresent(
                                      existingSubtypeDefinition ->
                                          subTypeDefinition.setSortKey(
                                              existingSubtypeDefinition.getSortKey()))));
    }

    domain.applyElementTypeDefinition(elementTypeDefinition);
    domain.setUpdatedAt(Instant.now());
    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      @Valid Client authenticatedClient,
      UUID domainId,
      ElementType elementType,
      ElementTypeDefinitionState elementTypeDefinition,
      // TODO #3042: remove this when we remove support for JSON schema
      boolean preserveSortKeys)
      implements UseCase.InputData {}
}
