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

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Key;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.GetElementUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.rest.security.ApplicationUser;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractElementController<T extends CompositeElement<T>>
        extends AbstractEntityControllerWithDefaultSearch {

    private final GetElementUseCase<T> getElementUseCase;

    /**
     * Load the element for the given id. The result is provided asynchronously by
     * the executed use case.
     *
     * @param auth
     *            the authentication in whose context the element is loaded
     * @param uuid
     *            an ID in the UUID format as specified in RFC 4122
     * @return the element for the given ID if one was found. Null otherwise.
     */
    public @Valid CompletableFuture<ResponseEntity<AbstractElementDto>> getElement(
            Authentication auth, String uuid) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());
        CompletableFuture<AbstractElementDto> assetFuture = useCaseInteractor.execute(getElementUseCase,
                                                                                      new UseCase.IdAndClient(
                                                                                              Key.uuidFrom(uuid),
                                                                                              client),
                                                                                      output -> entityToDtoTransformer.transform2Dto(output.getElement()));
        return assetFuture.thenApply(dto -> ResponseEntity.ok()
                                                          .eTag(ETag.from(uuid, dto.getVersion()))
                                                          .body(dto));
    }

    public @Valid CompletableFuture<List<AbstractElementDto>> getElementParts(Authentication auth,
            String uuid) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(getElementUseCase,
                                         new UseCase.IdAndClient(Key.uuidFrom(uuid), client),
                                         output -> {
                                             T element = output.getElement();
                                             return element.getParts()
                                                           .stream()
                                                           .map(entityToDtoTransformer::transform2Dto)
                                                           .collect(Collectors.toList());
                                         });
    }

}
