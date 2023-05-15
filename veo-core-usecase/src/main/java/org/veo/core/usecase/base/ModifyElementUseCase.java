/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.state.ElementState;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.common.ETagMismatchException;
import org.veo.core.usecase.decision.Decider;
import org.veo.core.usecase.service.DbIdRefResolver;
import org.veo.core.usecase.service.EntityStateMapper;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public abstract class ModifyElementUseCase<T extends Element>
    implements TransactionalUseCase<
        ModifyElementUseCase.InputData<T>, ModifyElementUseCase.OutputData<T>> {

  private final Class<T> elementClass;
  private final RepositoryProvider repositoryProvider;
  private final Decider decider;
  private final EntityStateMapper entityStateMapper;

  @Override
  public OutputData<T> execute(InputData<T> input) {
    ElementState<T> entity = input.getElement();
    ElementRepository<T> repo = repositoryProvider.getElementRepositoryFor(elementClass);
    var storedEntity =
        repo.findById(Key.uuidFrom(input.getId()))
            .orElseThrow(() -> new NotFoundException(Key.uuidFrom(input.getId()), elementClass));
    checkETag(storedEntity, input);
    checkClientBoundaries(input, storedEntity);
    entityStateMapper.mapState(
        entity,
        storedEntity,
        new DbIdRefResolver(repositoryProvider, input.getAuthenticatedClient()));
    evaluateDecisions(storedEntity);
    DomainSensitiveElementValidator.validate(storedEntity);

    storedEntity.setUpdatedAt(Instant.now());
    repo.save(storedEntity);
    return new OutputData<>(
        repo.getById(Key.uuidFrom(input.getId()), input.authenticatedClient.getId()));
  }

  private void evaluateDecisions(T entity) {
    entity
        .getDomains()
        .forEach(domain -> entity.setDecisionResults(decider.decide(entity, domain), domain));
  }

  private void checkETag(Element storedElement, InputData<? extends Element> input) {
    if (!ETag.matches(
        storedElement.getId().uuidValue(), storedElement.getVersion(), input.getETag())) {
      throw new ETagMismatchException(
          String.format(
              "The eTag does not match for the element with the ID %s",
              storedElement.getId().uuidValue()));
    }
  }

  protected void checkClientBoundaries(InputData<? extends Element> input, Element storedEntity) {
    storedEntity.checkSameClient(input.getAuthenticatedClient());
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData<T extends Element> implements UseCase.InputData {

    String id;

    @Valid ElementState<T> element;

    @Valid Client authenticatedClient;

    String eTag;

    String username;
  }

  @Valid
  @Value
  public static class OutputData<T> implements UseCase.OutputData {

    @Valid T entity;
  }
}
