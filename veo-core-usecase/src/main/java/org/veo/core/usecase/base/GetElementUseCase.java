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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ElementRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetElementUseCase<T extends Element>
    implements TransactionalUseCase<GetElementUseCase.InputData, GetElementUseCase.OutputData<T>> {

  private final DomainRepository domainRepository;
  private final ElementRepository<T> repository;
  private final Class<T> type;

  public OutputData<T> execute(InputData input) {
    T element =
        repository
            .findById(input.elementId)
            .orElseThrow(() -> new NotFoundException(input.elementId, type));
    element.checkSameClient(input.authenticatedClient);
    return new OutputData<>(element, getDomain(element, input).orElse(null));
  }

  protected Optional<Domain> getDomain(T element, InputData input) {
    return Optional.ofNullable(input.domainId)
        .map(id -> domainRepository.getById(id, input.authenticatedClient.getId()))
        .map(
            domain -> {
              if (!element.isAssociatedWithDomain(domain)) {
                throw NotFoundException.elementNotAssociatedWithDomain(
                    element, domain.getIdAsString());
              }
              return domain;
            });
  }

  @Valid
  public record InputData(
      @NotNull Key<UUID> elementId,
      @NotNull Client authenticatedClient,
      Key<UUID> domainId,
      boolean embedRisks)
      implements UseCase.InputData {
    public InputData(Key<UUID> id, Client authenticatedClient, boolean embedRisks) {
      this(id, authenticatedClient, null, embedRisks);
    }

    public InputData(Key<UUID> id, Client client) {
      this(id, client, false);
    }

    public InputData(Key<UUID> id, Client client, Key<UUID> domainId) {
      this(id, client, domainId, false);
    }
  }

  @Valid
  public record OutputData<T>(@Valid T element, @Valid Domain domain)
      implements UseCase.OutputData {}
}
