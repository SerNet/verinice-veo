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
package org.veo.rest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.ElementType;
import org.veo.core.usecase.InspectElementUseCase;
import org.veo.core.usecase.base.GetElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.decision.EvaluateElementUseCase;

public abstract class AbstractCompositeElementController<
        T extends CompositeElement<T>, E extends CompositeEntityDto<T>>
    extends AbstractElementController<T, E> {

  public AbstractCompositeElementController(
      ElementType elementType,
      GetElementUseCase<T> getElementUseCase,
      EvaluateElementUseCase evaluateElementUseCase,
      InspectElementUseCase inspectElementUseCase,
      GetElementsUseCase getElementsUseCase) {
    super(
        elementType,
        getElementUseCase,
        evaluateElementUseCase,
        inspectElementUseCase,
        getElementsUseCase);
  }

  public @Valid CompletableFuture<ResponseEntity<List<E>>> getElementParts(
      UUID uuid, WebRequest request) {
    if (getEtag(elementType.getType(), uuid).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    return useCaseInteractor.execute(
        getElementUseCase,
        new GetElementUseCase.InputData(uuid),
        output -> {
          T element = output.element();
          return ResponseEntity.ok()
              .cacheControl(defaultCacheControl)
              .body(element.getParts().stream().map(this::entity2Dto).toList());
        });
  }
}
