/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
import org.veo.core.entity.Domain;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;

/** Utility class to create entity specifications */
public final class EntitySpecifications {

  public static <T extends Identifiable> EntitySpecification<T> matchAll() {
    return i -> true;
  }

  public static EntitySpecification<Domain> isActive() {
    return Domain::isActive;
  }

  public static EntitySpecification<Identifiable> hasId(Key<UUID> id) {
    return o -> o.getId().equals(id);
  }

  public static SameClientSpecification hasSameClient(Client client) {
    return new SameClientSpecification(client);
  }

  public static ValidUnitSpecification hasValidUnit() {
    return ValidUnitSpecification.INSTANCE;
  }

  private EntitySpecifications() {}
}
