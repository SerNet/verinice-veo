/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.core.usecase;

import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.ModelConsistencyException;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.ClientRepository;

/** A collection of methods used by use cases. */
public final class UseCaseTools {

  private UseCaseTools() {}

  /**
   * Check if the client exists.
   *
   * @throws NotFoundException
   */
  public static Client checkClientExists(Key<UUID> clientId, ClientRepository clientRepository) {
    return clientRepository
        .findById(clientId)
        .orElseThrow(() -> new NotFoundException("Invalid client ID"));
  }

  /**
   * Checks if the given domain is owned by the client.
   *
   * @throws IllegalArgumentException when used with a Domaintemplate instance, as Domaintemplate
   *     can not be owned by a client.
   * @throws ModelConsistencyException when the domain is not owned by the client.
   */
  public static void checkDomainBelongsToClient(Client client, DomainTemplate domaintemplate) {
    if (!Domain.class.isAssignableFrom(domaintemplate.getModelInterface())) {
      throw new IllegalArgumentException("A DomainTemplate never belongs to a client");
    }
    if (!client.getDomains().contains(domaintemplate)) {
      throw new ClientBoundaryViolationException(domaintemplate, client);
    }
  }
}
