/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.core.entity.specification;

import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.DomainException;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.ref.IEntityRef;
import org.veo.core.entity.ref.TypedId;

import lombok.Getter;

public class ClientBoundaryViolationException extends DomainException {

  @Getter private final IEntityRef<?> ref;

  public ClientBoundaryViolationException(Identifiable entity, Client unauthorizedClient) {
    this(TypedId.from(entity), unauthorizedClient);
  }

  public ClientBoundaryViolationException(Identifiable entity, UUID unauthorizedClientId) {
    this(TypedId.from(entity), unauthorizedClientId);
  }

  public ClientBoundaryViolationException(IEntityRef<?> ref, Client unauthorizedClient) {
    this(ref, unauthorizedClient.getId());
  }

  public ClientBoundaryViolationException(IEntityRef<?> ref, UUID unauthorizedClientId) {
    super(
        String.format(
            "The client boundary would be violated by the attempted operation on %s by client: %s",
            ref, unauthorizedClientId));
    this.ref = ref;
  }
}
