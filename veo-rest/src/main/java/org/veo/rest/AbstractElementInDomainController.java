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

import java.util.concurrent.Future;

import javax.validation.Valid;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.dto.AbstractElementInDomainDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.usecase.base.GetElementUseCase;
import org.veo.rest.security.ApplicationUser;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractElementInDomainController<
        T extends Element, E extends AbstractElementInDomainDto<T>>
    extends AbstractEntityController {

  private final Class<T> modelType;

  private final GetElementUseCase<T> getElementUseCase;

  public @Valid Future<ResponseEntity<E>> getElement(
      Authentication auth, String domainId, String uuid, WebRequest request) {
    ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
    Client client = getClient(user.getClientId());
    if (getEtag(modelType, uuid).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    return useCaseInteractor
        .execute(
            getElementUseCase,
            new GetElementUseCase.InputData(Key.uuidFrom(uuid), client, Key.uuidFrom(domainId)),
            output -> entity2Dto(output.getElement(), output.getDomain()))
        .thenApply(dto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(dto));
  }

  protected abstract E entity2Dto(T entity, Domain domain);

  @Override
  protected String buildSearchUri(String searchId) {
    throw new NotImplementedException();
  }
}
