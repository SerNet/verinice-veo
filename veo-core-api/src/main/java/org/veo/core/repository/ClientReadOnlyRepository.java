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
package org.veo.core.repository;

import java.util.Optional;
import java.util.UUID;

import org.veo.core.entity.Client;

/**
 * A repository for {@link Client} entities.
 *
 * <p>Only implements operations that do not modify or create clients. Therefore it does not need a
 * validator service. It should be used during authentication operations that do not require write
 * access to clients.
 */
public interface ClientReadOnlyRepository {
  Optional<Client> findById(UUID id);

  Optional<Client> findByIdFetchTranslations(UUID id);
}
