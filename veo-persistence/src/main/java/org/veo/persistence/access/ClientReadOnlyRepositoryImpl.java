/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman.
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
package org.veo.persistence.access;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.repository.ClientReadOnlyRepository;
import org.veo.persistence.access.jpa.ClientDataRepository;

import lombok.AllArgsConstructor;

@Repository
@Transactional(readOnly = true)
@AllArgsConstructor
public class ClientReadOnlyRepositoryImpl implements ClientReadOnlyRepository {

  private final ClientDataRepository clientDataRepository;

  @Override
  public Optional<Client> findById(Key<UUID> id) {
    return clientDataRepository.findById(id.value()).map(Client.class::cast);
  }
}
