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
package org.veo.core.usecase.base;

import static java.time.Instant.now;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.state.CustomLinkState;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.service.DbIdRefResolver;
import org.veo.core.usecase.service.EntityStateMapper;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class AddLinksUseCase
    implements TransactionalUseCase<AddLinksUseCase.InputData, AddLinksUseCase.OutputData> {
  private final DomainRepository domainRepository;
  private final RepositoryProvider repositoryProvider;
  private final EntityStateMapper entityStateMapper;

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public OutputData execute(InputData input) {
    return new OutputData(
        execute(
            input.elementId, input.type, input.links, input.domainId, input.authenticatedClient));
  }

  private <T extends Element> T execute(
      Key<UUID> elementId,
      Class<T> type,
      Set<CustomLinkState> links,
      Key<UUID> domainId,
      Client client) {
    var domain = domainRepository.getById(domainId, client.getId());
    var elementRepo = repositoryProvider.getElementRepositoryFor(type);
    var element = elementRepo.getById(elementId, client.getId());
    if (!element.isAssociatedWithDomain(domain)) {
      throw new NotFoundException(
          "%s %s is not associated with domain %s"
              .formatted(element.getModelType(), element.getIdAsString(), domain.getIdAsString()));
    }
    var resolver = new DbIdRefResolver(repositoryProvider, client);
    links.stream()
        .map(linkState -> entityStateMapper.mapLink(linkState, element, domain, resolver))
        .forEach(element::addLink);
    DomainSensitiveElementValidator.validate(element);
    element.setUpdatedAt(now());
    return elementRepo.getById(elementId, client.getId());
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Key<UUID> elementId;
    Class<? extends Element> type;
    Key<UUID> domainId;
    Set<CustomLinkState> links;
    Client authenticatedClient;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid Element entity;
  }
}
