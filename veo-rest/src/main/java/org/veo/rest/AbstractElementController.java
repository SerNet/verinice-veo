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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Key;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.GetElementUseCase;
import org.veo.core.usecase.decision.EvaluateDecisionUseCase;
import org.veo.rest.security.ApplicationUser;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractElementController<T extends CompositeElement<T>, E extends CompositeEntityDto<T>>
        extends AbstractEntityControllerWithDefaultSearch {

    private final Class<T> modelType;

    private final GetElementUseCase<T> getElementUseCase;
    private final EvaluateDecisionUseCase evaluateDecisionUseCase;

    @Autowired
    private TransactionalRunner runner;

    /**
     * Load the element for the given id. The result is provided asynchronously by
     * the executed use case.
     *
     * @param auth
     *            the authentication in whose context the element is loaded
     * @param uuid
     *            an ID in the UUID format as specified in RFC 4122
     * @param request
     *            the corresponding web request
     * @return the element for the given ID if one was found. Null otherwise.
     */
    public @Valid CompletableFuture<ResponseEntity<E>> getElement(Authentication auth, String uuid,
            WebRequest request) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());
        if (getEtag(modelType, uuid).map(request::checkNotModified)
                                    .orElse(false)) {
            return null;
        }
        CompletableFuture<E> entityFuture = useCaseInteractor.execute(getElementUseCase,
                                                                      new UseCase.IdAndClient(
                                                                              Key.uuidFrom(uuid),
                                                                              client),
                                                                      output -> entity2Dto(output.getElement()));
        return entityFuture.thenApply(dto -> ResponseEntity.ok()
                                                           .cacheControl(defaultCacheControl)
                                                           .body(dto));
    }

    public @Valid CompletableFuture<ResponseEntity<List<E>>> getElementParts(Authentication auth,
            String uuid, WebRequest request) {
        Client client = getAuthenticatedClient(auth);
        if (getEtag(modelType, uuid).map(request::checkNotModified)
                                    .orElse(false)) {
            return null;
        }
        return useCaseInteractor.execute(getElementUseCase,
                                         new UseCase.IdAndClient(Key.uuidFrom(uuid), client),
                                         output -> {
                                             T element = output.getElement();
                                             return ResponseEntity.ok()
                                                                  .cacheControl(defaultCacheControl)
                                                                  .body(element.getParts()
                                                                               .stream()
                                                                               .map(this::entity2Dto)
                                                                               .collect(Collectors.toList()));
                                         });
    }

    public @Valid CompletableFuture<ResponseEntity<DecisionResult>> evaluateDecision(
            Authentication auth, @Valid AbstractElementDto dto, String decisionKey,
            String domainId) {
        var client = getAuthenticatedClient(auth);

        var element = runner.run(() -> {
            return dtoToEntityTransformer.transformDto2Element(dto, createIdRefResolver(client));
        });
        return useCaseInteractor.execute(evaluateDecisionUseCase,
                                         new EvaluateDecisionUseCase.InputData(client,
                                                 Key.uuidFrom(domainId), decisionKey, element),
                                         output -> ResponseEntity.ok()
                                                                 .body(output.getDecisionResult()));
    }

    protected abstract E entity2Dto(T entity);

}
