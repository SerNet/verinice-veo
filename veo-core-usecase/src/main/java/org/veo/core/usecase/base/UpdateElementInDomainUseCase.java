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

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.state.ElementState;
import org.veo.core.repository.ElementRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.decision.Decider;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.RefResolverFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class UpdateElementInDomainUseCase<T extends Element>
    implements TransactionalUseCase<
        UpdateElementInDomainUseCase.InputData<T>, UpdateElementInDomainUseCase.OutputData<T>> {

  private final ElementRepository<T> repo;
  private final RefResolverFactory refResolverFactory;
  private final Decider decider;
  private final EntityStateMapper entityStateMapper;

  @Override
  public OutputData<T> execute(InputData<T> input, UserAccessRights userAccessRights) {
    var idRefResolver = refResolverFactory.db(input.authenticatedClient);

    var domain = idRefResolver.resolve(input.domainId, Domain.class);
    var inputElement = input.element;
    var storedElement = repo.getById(input.id, userAccessRights);
    userAccessRights.checkElementWriteAccess(storedElement);
    if (!storedElement.isAssociatedWithDomain(domain)) {
      throw new NotFoundException(
          "%s %s is not associated with domain %s",
          storedElement.getModelInterface().getSimpleName(),
          storedElement.getIdAsString(),
          domain.getIdAsString());
    }
    ETag.validate(input.eTag, storedElement);
    entityStateMapper.mapState(inputElement, storedElement, false, idRefResolver);
    storedElement.setUpdatedAt(Instant.now());
    storedElement.setDecisionResults(decider.decide(storedElement, domain), domain);
    DomainSensitiveElementValidator.validate(storedElement);
    repo.save(storedElement);
    // re-fetch element to make sure it is returned with updated versioning information
    return new OutputData<>(repo.getById(storedElement.getId(), userAccessRights));
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData<T extends Element>(
      UUID id,
      @Valid ElementState<T> element,
      UUID domainId,
      Client authenticatedClient,
      String eTag)
      implements UseCase.InputData {}

  @Valid
  public record OutputData<T>(@Valid T entity) implements UseCase.OutputData {}
}
