/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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
package org.veo.core.entity;

import java.util.Objects;
import java.util.Optional;

import org.veo.core.entity.ref.IEntityRef;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.EntitySpecifications;

/** Something that can be owned by a specific client. */
public interface ClientOwned extends Entity {
  Optional<Client> getOwningClient();

  /**
   * @throws ClientBoundaryViolationException if the passed client is not equal to the client in the
   *     unit to which the entity belongs
   */
  default void checkSameClient(Client client) {
    Objects.requireNonNull(client, "client must not be null");
    if (!(EntitySpecifications.hasSameClient(client)
        .isSatisfiedBy(getOwningClient().orElseThrow()))) {
      throw new ClientBoundaryViolationException(IEntityRef.from(this), client);
    }
  }
}
