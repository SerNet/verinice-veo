/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
package org.veo.core.usecase.profile;

import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.ProfileRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class AbstractProfileUseCase {
  protected final ProfileRepository profileRepo;

  void checkClientOwnsDomain(Client client, UUID id) {
    client.getDomains().stream()
        .filter(d -> d.getId().equals(id))
        .findAny()
        .orElseThrow(() -> new NotFoundException(id, Domain.class));
  }
}
