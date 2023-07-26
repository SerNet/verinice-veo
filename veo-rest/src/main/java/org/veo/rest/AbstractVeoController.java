/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade.
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

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.repository.ClientRepository;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.rest.common.ClientNotActiveException;
import org.veo.rest.security.ApplicationUser;

public abstract class AbstractVeoController {

  @Autowired protected UseCaseInteractor useCaseInteractor;
  @Autowired protected ReferenceAssembler referenceAssembler;
  @Autowired protected ClientRepository clientRepository;

  protected AbstractVeoController() {}

  protected Client getClient(String clientId) {
    Key<UUID> id = Key.uuidFrom(clientId);
    return clientRepository
        .findActiveById(id)
        .orElseThrow(() -> new ClientNotActiveException(clientId));
  }

  protected Client getAuthenticatedClient(Authentication auth) {
    ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
    return getClient(user);
  }

  protected Client getClient(ApplicationUser user) {
    return getClient(user.getClientId());
  }
}
