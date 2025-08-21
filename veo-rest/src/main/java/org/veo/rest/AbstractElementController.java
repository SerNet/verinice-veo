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
package org.veo.rest;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.repository.QueryCondition;
import org.veo.core.usecase.InspectElementUseCase;
import org.veo.core.usecase.base.GetElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.rest.security.ApplicationUser;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractElementController<T extends Element, E extends AbstractElementDto<T>>
    extends AbstractEntityController {

  protected final ElementType elementType;

  protected final GetElementUseCase<T> getElementUseCase;
  private final EvaluateElementUseCase evaluateElementUseCase;
  private final InspectElementUseCase inspectElementUseCase;
  private final GetElementsUseCase getElementsUseCase;

  @Autowired private TransactionalRunner runner;

  /**
   * Load the element for the given id. The result is provided asynchronously by the executed use
   * case.
   *
   * @param auth the authentication in whose context the element is loaded
   * @param uuid an ID in the UUID format as specified in RFC 4122
   * @param request the corresponding web request
   * @return the element for the given ID if one was found. Null otherwise.
   */
  public @Valid Future<ResponseEntity<E>> getElement(
      Authentication auth, UUID uuid, WebRequest request) {
    if (getEtag(elementType.getType(), uuid).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    CompletableFuture<E> entityFuture =
        useCaseInteractor.execute(
            getElementUseCase,
            new GetElementUseCase.InputData(uuid),
            output -> entity2Dto(output.element()));
    return entityFuture.thenApply(
        dto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(dto));
  }

  protected Future<PageDto<E>> getElements(GetElementsUseCase.InputData inputData) {
    return getElements(inputData, this::entity2Dto);
  }

  protected Future<PageDto<E>> getElements(
      GetElementsUseCase.InputData inputData, Function<T, E> dtoMapper) {
    return useCaseInteractor.execute(
        getElementsUseCase,
        inputData.withElementTypes(new QueryCondition<>(Set.of(elementType))),
        o -> PagingMapper.toPage(o.elements(), e -> dtoMapper.apply((T) e)));
  }

  public @Valid CompletableFuture<ResponseEntity<EvaluateElementUseCase.OutputData>> evaluate(
      ApplicationUser user, @Valid E dto, String domainId) {
    return useCaseInteractor.execute(
        evaluateElementUseCase,
        new EvaluateElementUseCase.InputData(getClient(user), UUID.fromString(domainId), dto),
        output -> ResponseEntity.ok().body(output));
  }

  public CompletableFuture<ResponseEntity<Set<Finding>>> inspect(
      UUID elementId, UUID domainId, Class<T> elementType) {
    return useCaseInteractor.execute(
        inspectElementUseCase,
        new InspectElementUseCase.InputData(elementType, elementId, domainId),
        output -> ResponseEntity.ok().body(output.findings()));
  }

  @SuppressWarnings("unchecked")
  protected ResponseEntity<E> toResponseEntity(T entity) {
    return ResponseEntity.ok()
        .eTag(ETag.from(entity.getIdAsString(), entity.getVersion()))
        .body((E) entityToDtoTransformer.transform2Dto(entity, false));
  }

  protected abstract E entity2Dto(T entity);
}
