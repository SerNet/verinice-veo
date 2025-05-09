/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

import java.util.UUID;

/**
 * An {@link Entity} which is defined as the conjunction between two other entities, which are
 * designated as "first" and "second". A compound-identifiable entity is defined by the IDs of the
 * two linked entities, but has no ID of its own.
 *
 * @param <TFirst> Type of the first entity
 * @param <TSecond> Type of the second entity
 */
public interface CompoundIdentifiable<TFirst extends Identifiable, TSecond extends Identifiable>
    extends Entity {
  @Override
  Class<? extends CompoundIdentifiable<TFirst, TSecond>> getModelInterface();

  TFirst getFirstRelation();

  default UUID getFirstId() {
    return getFirstRelation().getId();
  }

  default String getFirstIdAsString() {
    return getFirstRelation().getIdAsString();
  }

  default UUID getFirstIdAsUUID() {
    return getFirstRelation().getId();
  }

  TSecond getSecondRelation();

  default UUID getSecondId() {
    return getSecondRelation().getId();
  }

  default String getSecondIdAsString() {
    return getSecondRelation().getIdAsString();
  }
}
