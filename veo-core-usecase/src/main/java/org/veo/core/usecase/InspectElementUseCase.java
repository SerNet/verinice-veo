/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.usecase;

import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.inspection.Inspector;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InspectElementUseCase
    implements TransactionalUseCase<
        InspectElementUseCase.InputData, InspectElementUseCase.OutputData> {
  private final DomainRepository domainRepository;
  private final RepositoryProvider repositoryProvider;
  private final Inspector inspector;

  @Override
  public OutputData execute(InputData input) {
    var client = input.client;
    var domain =
        domainRepository
            .findById(input.domainId)
            .orElseThrow(
                () ->
                    new NotFoundException("Domain with ID %s not found".formatted(input.domainId)));
    if (!client.equals(domain.getOwner())) {
      throw new ClientBoundaryViolationException(domain, client);
    }
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }

    var element =
        repositoryProvider
            .getRepositoryFor(input.elementType)
            .findById(input.elementId)
            .orElseThrow(() -> new NotFoundException(input.elementId, input.elementType));
    element.checkSameClient(client);
    return new OutputData(inspector.inspect(element, domain));
  }

  public record InputData(
      Client client, Class<? extends Element> elementType, UUID elementId, UUID domainId)
      implements UseCase.InputData {}

  public record OutputData(Set<Finding> findings) implements UseCase.OutputData {}
}
