/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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

import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ElementRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class GetElementUseCase<T extends Element>
    implements TransactionalUseCase<GetElementUseCase.InputData, GetElementUseCase.OutputData<T>> {

  private final DomainRepository domainRepository;
  private final ElementRepository<T> repository;
  private final Class<T> type;

  public OutputData<T> execute(InputData input) {
    T element =
        repository
            .findById(input.getId())
            .orElseThrow(() -> new NotFoundException(input.getId(), type));
    element.checkSameClient(input.getAuthenticatedClient());
    return new OutputData<>(element, getDomain(element, input).orElse(null));
  }

  protected Optional<Domain> getDomain(T element, InputData input) {
    return Optional.ofNullable(input.domainId)
        .map(id -> domainRepository.getById(id, input.getAuthenticatedClient().getId()))
        .map(
            domain -> {
              if (!element.isAssociatedWithDomain(domain)) {
                throw new NotFoundException(
                    "%s %s is not associated with domain %s",
                    element.getModelInterface().getSimpleName(),
                    element.getIdAsString(),
                    domain.getIdAsString());
              }
              return domain;
            });
  }

  @EqualsAndHashCode(callSuper = true)
  @Valid
  @Getter
  public static class InputData extends UseCase.IdAndClient {
    public InputData(Key<UUID> id, Client authenticatedClient) {
      super(id, authenticatedClient);
      this.domainId = null;
    }

    public InputData(Key<UUID> id, Client authenticatedClient, Key<UUID> domainId) {
      super(id, authenticatedClient);
      this.domainId = domainId;
    }

    Key<UUID> domainId;
  }

  @Valid
  @Value
  public static class OutputData<T> implements UseCase.OutputData {
    @Valid T element;
    @Valid Domain domain;
  }
}
