/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade
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
package org.veo.core.entity.ref;

import org.veo.core.entity.EntityType;
import org.veo.core.entity.Identifiable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Value
public class TypedId<T extends Identifiable> implements ITypedId<T> {

  @NonNull private final String id;

  @NonNull private final Class<T> type;

  public static <T extends Identifiable> TypedId<T> from(String id, Class<T> type) {
    if (id == null) {
      throw new IllegalArgumentException(
          "Missing ID for %s".formatted(EntityType.getSingularTermByType(type)));
    }
    return new TypedId<>(id, type);
  }

  public static <T extends Identifiable> TypedId<T> from(T entity) {
    return TypedId.from(entity.getIdAsString(), (Class<T>) entity.getModelInterface());
  }

  @Override
  public boolean equals(Object other) {
    return ITypedId.equals(this, other);
  }

  @Override
  public int hashCode() {
    return ITypedId.hashCode(this);
  }

  @Override
  public String toString() {
    return ITypedId.toString(this);
  }
}
